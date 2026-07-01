package dev.breezes.settlements.application.ai.inference;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpInferenceTransportTest {

    private final List<HttpInferenceTransport> transports = new ArrayList<>();
    private HttpServer server;
    private ExecutorService serverExecutor;

    @AfterEach
    void tearDown() {
        for (HttpInferenceTransport transport : this.transports) {
            transport.close();
        }

        if (this.server != null) {
            this.server.stop(0);
        }

        if (this.serverExecutor != null) {
            this.serverExecutor.shutdownNow();
        }
    }

    @Test
    void post_returnsMissWhenEndpointIsMissing() {
        // Arrange
        HttpInferenceTransport transport = newTransport(config("", ""));

        // Act
        InferenceTransportResponse response = transport.post(
                InferenceCapability.MONOLOGUE,
                TestPayload.of("hello"),
                Duration.ofSeconds(1)).join();

        // Assert
        assertFalse(response.body().isPresent());
        assertEquals(0, response.getStatusCode());
    }

    @Test
    void post_returnsMissWhenBackendReturnsNonSuccessStatus() throws IOException {
        // Arrange
        CapturedRequest capturedRequest = startServer(503, "unavailable");
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));

        // Act
        InferenceTransportResponse response = transport.post(
                InferenceCapability.MONOLOGUE,
                TestPayload.of("hello"),
                Duration.ofSeconds(1)).join();

        // Assert
        assertTrue(capturedRequest.awaitRequest());
        assertFalse(response.body().isPresent());
        assertEquals(0, response.getStatusCode());
    }

    @Test
    void post_sendsProtocolEnvelopeWithoutAuthorizationWhenApiKeyIsBlank() throws IOException {
        // Arrange
        CapturedRequest capturedRequest = startServer(200, "{\"ok\":true}");
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));

        // Act
        InferenceTransportResponse response = transport.post(
                InferenceCapability.MONOLOGUE,
                TestPayload.of("hello"),
                Duration.ofMillis(750)).join();

        // Assert
        assertTrue(capturedRequest.awaitRequest());
        assertEquals(200, response.getStatusCode());
        assertEquals("{\"ok\":true}", response.body().orElseThrow());
        assertEquals("/v1/monologue", capturedRequest.path());
        assertEquals("1", capturedRequest.header("X-Settlements-Inference-Protocol"));
        assertEquals("application/json", capturedRequest.header("Content-Type"));
        assertTrue(capturedRequest.headers("Authorization").isEmpty());
        assertTrue(capturedRequest.body().contains("\"capability\":\"MONOLOGUE\""));
        assertTrue(capturedRequest.body().contains("\"deadlineMillis\":750"));
        assertFalse(capturedRequest.body().contains("deadlineSlackMillis"), "deadlineSlackMillis must not appear — SIS extra=forbid rejects it");
        assertTrue(capturedRequest.body().contains("\"message\":\"hello\""));
    }

    @Test
    void post_sendsBearerAuthorizationWhenApiKeyIsConfigured() throws IOException {
        // Arrange
        CapturedRequest capturedRequest = startServer(200, "{\"ok\":true}");
        HttpInferenceTransport transport = newTransport(config(baseUrl(), "secret-token"));

        // Act
        transport.post(
                InferenceCapability.MONOLOGUE,
                TestPayload.of("hello"),
                Duration.ofSeconds(1)).join();

        // Assert
        assertTrue(capturedRequest.awaitRequest());
        assertEquals("Bearer secret-token", capturedRequest.header("Authorization"));
    }

    @Test
    void postStreaming_deliversEachNonBlankLineInOrder() throws Exception {
        // Arrange — three NDJSON lines with a blank line that must be skipped
        String body = "{\"a\":1}\n{\"b\":2}\n\n{\"c\":3}\n";
        startServer(exchange -> respondFixed(exchange, 200, body));
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));
        List<String> received = Collections.synchronizedList(new ArrayList<>());

        // Act
        InferenceStreamHandle handle = transport.postStreaming(
                InferenceCapability.MONOLOGUE, TestPayload.of("hello"), Duration.ofSeconds(5), received::add);
        handle.completion().get(5, TimeUnit.SECONDS);

        // Assert — blank line dropped, order preserved
        assertEquals(List.of("{\"a\":1}", "{\"b\":2}", "{\"c\":3}"), received);
    }

    @Test
    void postStreaming_nonSuccessStatus_deliversNoLinesAndCompletes() throws Exception {
        // Arrange — backend errors out; its error body must never surface as lines
        startServer(exchange -> respondFixed(exchange, 503, "{\"error\":\"unavailable\"}\n"));
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));
        List<String> received = Collections.synchronizedList(new ArrayList<>());

        // Act
        InferenceStreamHandle handle = transport.postStreaming(
                InferenceCapability.MONOLOGUE, TestPayload.of("hello"), Duration.ofSeconds(5), received::add);
        handle.completion().get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(received.isEmpty(), "non-2xx body must be discarded, not delivered as lines");
    }

    @Test
    void postStreaming_missingEndpoint_returnsCompletedNoOp() throws Exception {
        // Arrange — no endpoint configured
        HttpInferenceTransport transport = newTransport(config("", ""));
        List<String> received = Collections.synchronizedList(new ArrayList<>());

        // Act
        InferenceStreamHandle handle = transport.postStreaming(
                InferenceCapability.MONOLOGUE, TestPayload.of("hello"), Duration.ofSeconds(1), received::add);

        // Assert — already-complete handle, no lines, cancel is safe
        assertTrue(handle.completion().isDone());
        assertTrue(received.isEmpty());
        handle.cancel();
    }

    @Test
    void postStreaming_cancelWhileStreaming_stopsDeliveryAndCompletes() throws Exception {
        // Arrange — server sends one line then holds the stream open until released
        CountDownLatch clientGotFirstLine = new CountDownLatch(1);
        CountDownLatch serverMayFinish = new CountDownLatch(1);
        startServer(exchange -> {
            try {
                exchange.sendResponseHeaders(200, 0); // 0 => chunked, arbitrary length
                OutputStream body = exchange.getResponseBody();
                body.write("line-1\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
                // Hold the response open so the stream can only end via the client's cancel.
                serverMayFinish.await(5, TimeUnit.SECONDS);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));
        List<String> received = Collections.synchronizedList(new ArrayList<>());

        // Act — consume the first line, then cancel mid-stream
        InferenceStreamHandle handle = transport.postStreaming(
                InferenceCapability.MONOLOGUE, TestPayload.of("hello"), Duration.ofSeconds(30),
                line -> {
                    received.add(line);
                    clientGotFirstLine.countDown();
                });
        assertTrue(clientGotFirstLine.await(5, TimeUnit.SECONDS), "first line should arrive before cancel");
        assertFalse(handle.completion().isDone(), "stream must still be open before cancel");
        handle.cancel();

        // Assert — completion resolves (never exceptionally) and no further lines were delivered
        handle.completion().get(5, TimeUnit.SECONDS);
        assertEquals(List.of("line-1"), received);
        serverMayFinish.countDown(); // release the server handler thread
    }

    @Test
    void postStreaming_cancelAbortsConnection_serverObservesDisconnect() throws Exception {
        // Arrange — server streams continuously; once the client cancels, its next write must fail,
        // proving the HTTP connection was aborted so the backend can free GPU resources.
        CountDownLatch clientGotFirstLine = new CountDownLatch(1);
        CountDownLatch serverWriteFailed = new CountDownLatch(1);
        startServer(exchange -> {
            try {
                exchange.sendResponseHeaders(200, 0); // chunked
                OutputStream body = exchange.getResponseBody();
                body.write("line-1\n".getBytes(StandardCharsets.UTF_8));
                body.flush();
                byte[] filler = ("x".repeat(8192) + "\n").getBytes(StandardCharsets.UTF_8);
                for (int i = 0; i < 600; i++) {
                    body.write(filler);
                    body.flush();
                    Thread.sleep(10);
                }
            } catch (IOException e) {
                serverWriteFailed.countDown(); // client aborted the exchange
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        HttpInferenceTransport transport = newTransport(config(baseUrl(), ""));

        InferenceStreamHandle handle = transport.postStreaming(
                InferenceCapability.MONOLOGUE, TestPayload.of("hello"), Duration.ofSeconds(30),
                line -> clientGotFirstLine.countDown());
        assertTrue(clientGotFirstLine.await(5, TimeUnit.SECONDS), "first line should arrive before cancel");

        // Act
        handle.cancel();

        // Assert — the server's ongoing writes fail once the connection is aborted
        assertTrue(serverWriteFailed.await(6, TimeUnit.SECONDS),
                "cancel must abort the HTTP connection so the backend sees the disconnect");
    }

    private HttpInferenceTransport newTransport(InferenceConfig config) {
        HttpInferenceTransport transport = new HttpInferenceTransport(config);
        this.transports.add(transport);
        return transport;
    }

    private static InferenceConfig config(String endpointBaseUrl, String apiKey) {
        return new InferenceConfig(endpointBaseUrl, apiKey, "en_us", 50);
    }

    private CapturedRequest startServer(int statusCode, String responseBody) throws IOException {
        CapturedRequest capturedRequest = new CapturedRequest();
        startServer(exchange -> handle(exchange, statusCode, responseBody, capturedRequest));
        return capturedRequest;
    }

    private void startServer(HttpHandler handler) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.server.createContext(InferenceCapability.MONOLOGUE.getPath(), handler);
        this.serverExecutor = Executors.newSingleThreadExecutor();
        this.server.setExecutor(this.serverExecutor);
        this.server.start();
    }

    private String baseUrl() {
        return "http://localhost:" + this.server.getAddress().getPort();
    }

    private static void handle(HttpExchange exchange,
                               int statusCode,
                               String responseBody,
                               CapturedRequest capturedRequest) throws IOException {
        capturedRequest.capture(exchange);
        respondFixed(exchange, statusCode, responseBody);
    }

    private static void respondFixed(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private record TestPayload(String message) {

        static TestPayload of(String message) {
            return new TestPayload(message);
        }

    }

    private static final class CapturedRequest {

        private String path;
        private Headers headers;
        private String body;

        void capture(HttpExchange exchange) throws IOException {
            this.path = exchange.getRequestURI().getPath();
            this.headers = exchange.getRequestHeaders();
            this.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean awaitRequest() {
            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (System.nanoTime() < deadlineNanos) {
                if (this.body != null) {
                    return true;
                }
                Thread.onSpinWait();
            }

            return false;
        }

        String path() {
            return this.path;
        }

        String header(String name) {
            return this.headers.getFirst(name);
        }

        List<String> headers(String name) {
            return this.headers.getOrDefault(name, List.of());
        }

        String body() {
            return this.body;
        }

    }

}
