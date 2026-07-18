package dev.breezes.settlements.application.ai.inference;

import com.google.gson.Gson;
import jakarta.inject.Inject;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Java HTTP transport for inference capability requests
 */
@CustomLog
public final class HttpInferenceTransport implements InferenceTransport {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final Gson GSON = new Gson();

    private final InferenceConfig config;
    private final HttpClient httpClient;

    @Inject
    public HttpInferenceTransport(@Nonnull InferenceConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public CompletableFuture<InferenceTransportResponse> post(@Nonnull InferenceCapability capability,
                                                              @Nonnull Object payload,
                                                              @Nonnull Duration deadline) {
        if (!this.config.hasEndpoint()) {
            return CompletableFuture.completedFuture(InferenceTransportResponse.miss());
        }

        String body = this.renderEnvelope(capability, payload, deadline);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(this.config.normalizedBaseUrl() + capability.getPath()))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header("X-Settlements-Inference-Protocol", Integer.toString(InferenceProtocol.VERSION))
                .timeout(deadline)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (this.config.hasApiKey()) {
            requestBuilder.header("Authorization", "Bearer " + this.config.apiKey());
        }

        return this.httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.error("Inference backend returned HTTP {} for {}: {}", response.statusCode(), capability, response.body());
                        return InferenceTransportResponse.miss();
                    }
                    return InferenceTransportResponse.success(response.body(), response.statusCode());
                })
                .exceptionally(throwable -> {
                    log.error("Inference request for {} missed", capability, throwable);
                    return InferenceTransportResponse.miss();
                });
    }

    @Override
    public InferenceStreamHandle postStreaming(@Nonnull InferenceCapability capability,
                                               @Nonnull Object payload,
                                               @Nonnull Duration deadline,
                                               @Nonnull Consumer<String> onLine) {
        if (!this.config.hasEndpoint()) {
            return InferenceStreamHandle.noOp();
        }

        String body = this.renderEnvelope(capability, payload, deadline);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(this.config.normalizedBaseUrl() + capability.getPath()))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header("X-Settlements-Inference-Protocol", Integer.toString(InferenceProtocol.VERSION))
                .timeout(deadline)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (this.config.hasApiKey()) {
            requestBuilder.header("Authorization", "Bearer " + this.config.apiKey());
        }

        // Branch inside the BodyHandler so non-2xx error bodies are discarded (no lines delivered),
        // while 2xx responses are forwarded line-by-line as they arrive.
        HttpResponse.BodyHandler<Void> bodyHandler = responseInfo -> {
            if (responseInfo.statusCode() >= 200 && responseInfo.statusCode() < 300) {
                return buildLineBodySubscriber(onLine);
            } else {
                log.error("Inference streaming backend returned HTTP {} for {}", responseInfo.statusCode(), capability);
                return HttpResponse.BodySubscribers.replacing(null);
            }
        };

        // Holding the source future directly is the only way cancel() can reach the actual
        // HTTP exchange — cancelling a derived thenApply/thenCompose stage does NOT propagate
        // upstream and leaves the connection open, wasting backend GPU.
        CompletableFuture<HttpResponse<Void>> sourceFuture = this.httpClient.sendAsync(requestBuilder.build(), bodyHandler);

        // Absorb all errors so the completion future never fails exceptionally — callers can
        // safely attach thenRun cleanup without wrapping in exceptionally.
        CompletableFuture<Void> completion = sourceFuture.handle((response, throwable) -> {
            if (throwable != null) {
                log.error("Inference streaming request for {} ended with error", capability, throwable);
            }
            return null;
        });

        return new InferenceStreamHandle() {
            @Override
            public void cancel() {
                // Cancel the source future so the underlying HTTP connection is aborted.
                // The backend sees the disconnect and can free its KV-cache slots.
                sourceFuture.cancel(true);
            }

            @Override
            public CompletableFuture<Void> completion() {
                return completion;
            }
        };
    }

    /**
     * Builds a body subscriber that forwards each complete line to {@code onLine}.
     * Blank lines are silently skipped; per-line errors are absorbed (the stream continues).
     */
    private static HttpResponse.BodySubscriber<Void> buildLineBodySubscriber(Consumer<String> onLine) {
        Flow.Subscriber<String> lineSubscriber = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                // Unbounded demand — we process lines as fast as they arrive from the backend.
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String line) {
                if (!line.isBlank()) {
                    onLine.accept(line);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                // Errors surface at the sourceFuture level; nothing to do per-line.
            }

            @Override
            public void onComplete() {
                // Completion is signaled via the sourceFuture, not here.
            }
        };

        // The explicit type witness forces T=Void so the return type is BodySubscriber<Void>.
        // The finisher returns null (the only valid Void value) after the subscriber finishes.
        Function<Flow.Subscriber<String>, Void> finisher = ignored -> null;
        return HttpResponse.BodySubscribers.fromLineSubscriber(lineSubscriber, finisher, StandardCharsets.UTF_8, "\n");
    }

    /**
     * Builds the compact wire JSON for the given capability, payload, and deadline without sending it.
     * Centralizes envelope construction so {@link #post} and dev-tool dump commands share one code path.
     */
    @Override
    public String renderEnvelope(@Nonnull InferenceCapability capability,
                                 @Nonnull Object payload,
                                 @Nonnull Duration deadline) {
        InferenceRequestEnvelope envelope = InferenceRequestEnvelope.builder()
                .protocolVersion(InferenceProtocol.VERSION)
                .requestId(UUID.randomUUID())
                .capability(capability)
                .deadlineMillis(deadline.toMillis())
                .payload(payload)
                .build();
        return GSON.toJson(envelope);
    }

    @Override
    public void close() {
        this.httpClient.close();
    }

}
