package dev.breezes.settlements.application.ai.inference;

import com.google.gson.Gson;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
                        log.error("Inference backend returned HTTP {} for {}", response.statusCode(), capability);
                        return InferenceTransportResponse.miss();
                    }
                    return InferenceTransportResponse.success(response.body(), response.statusCode());
                })
                .exceptionally(throwable -> {
                    log.error("Inference request for {} missed: {}", capability, throwable.getMessage());
                    return InferenceTransportResponse.miss();
                });
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
                .deadlineSlackMillis(this.config.deadlineSlackMillis())
                .payload(payload)
                .build();
        return GSON.toJson(envelope);
    }

    @Override
    public void close() {
        this.httpClient.close();
    }

}
