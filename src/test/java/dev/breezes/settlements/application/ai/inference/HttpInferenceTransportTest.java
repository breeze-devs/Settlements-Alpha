package dev.breezes.settlements.application.ai.inference;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
        assertTrue(capturedRequest.body().contains("\"deadlineSlackMillis\":250"));
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

    private HttpInferenceTransport newTransport(InferenceConfig config) {
        HttpInferenceTransport transport = new HttpInferenceTransport(config);
        this.transports.add(transport);
        return transport;
    }

    private static InferenceConfig config(String endpointBaseUrl, String apiKey) {
        return new InferenceConfig(endpointBaseUrl, apiKey, "en_us", 2, 250);
    }

    private CapturedRequest startServer(int statusCode, String responseBody) throws IOException {
        CapturedRequest capturedRequest = new CapturedRequest();
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.server.createContext(InferenceCapability.MONOLOGUE.getPath(), exchange -> handle(exchange, statusCode, responseBody, capturedRequest));
        this.serverExecutor = Executors.newSingleThreadExecutor();
        this.server.setExecutor(this.serverExecutor);
        this.server.start();
        return capturedRequest;
    }

    private String baseUrl() {
        return "http://localhost:" + this.server.getAddress().getPort();
    }

    private static void handle(HttpExchange exchange,
                               int statusCode,
                               String responseBody,
                               CapturedRequest capturedRequest) throws IOException {
        capturedRequest.capture(exchange);
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
