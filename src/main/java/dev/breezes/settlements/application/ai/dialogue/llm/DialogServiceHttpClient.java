package dev.breezes.settlements.application.ai.dialogue.llm;

import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import lombok.CustomLog;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * TODO:CONFIRM -- do we want our own api service? we might be able to wrap prompts, etc on the backend instead of sending over network
 * TODO:CONFIRM -- this is a bigger review/refactoring, might be good for another commit.
 * Thin async HTTP wrapper around the dialog service.
 * <p>
 * This class is the <em>only</em> place in the codebase that touches {@code HttpClient}
 * for dialog inference. All callers go through it so the transport is swappable without
 * touching request-assembly logic.
 * <p>
 * Threading: {@code sendAsync} always returns a {@code CompletableFuture}. I/O never
 * blocks the server thread — this is enforced by the contract (§2) and by using
 * {@link HttpClient#sendAsync} throughout.
 * <p>
 * Auth: the Bearer token is included only when {@link DialogueConfig#hasApiKey()} is true.
 * The key is never logged above DEBUG level and is never sent to the client (contract §6).
 */
@CustomLog
public final class DialogServiceHttpClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final String baseUrl;
    @Nullable
    private final String apiKey;
    private final HttpClient httpClient;

    public DialogServiceHttpClient(DialogueConfig config) {
        this.baseUrl = config.endpointBaseUrl().stripTrailing();
        this.apiKey = config.hasApiKey() ? config.apiKey() : null;
        // One shared HttpClient per DialogServiceHttpClient instance — reuses the underlying
        // connection pool (HTTP/1.1 keep-alive or HTTP/2 multiplexing) across all requests.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends a Chat Completions request asynchronously and returns a future that resolves
     * to the parsed response.
     * <p>
     * On HTTP error, network failure, or JSON parse failure the future resolves to an
     * empty {@link LlmResponse} (never throws into callers). Degradation is always silent.
     *
     * @param request  the request to send
     * @param deadline maximum time to wait for a response from the server
     * @return a future resolving to a (possibly empty) parsed response
     */
    public CompletableFuture<LlmResponse> send(DialogRequest request, Duration deadline) {
        String requestBody = request.toJson();
        log.debug("LLM request: model={} tokens={} seeds={}", request.getModel(), request.getMaxTokens(), request.getN());

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(this.baseUrl + CHAT_COMPLETIONS_PATH))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .timeout(deadline)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        // Only attach the Authorization header when a key is configured
        if (this.apiKey != null) {
            httpRequestBuilder.header("Authorization", "Bearer " + this.apiKey);
        }

        HttpRequest httpRequest = httpRequestBuilder.build();

        return this.httpClient
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .orTimeout(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.warn("LLM backend returned HTTP {}: {}", response.statusCode(), response.body());
                        return LlmResponse.parse(null);
                    }
                    log.debug("LLM response received: status={}", response.statusCode());
                    return LlmResponse.parse(response.body());
                })
                .exceptionally(throwable -> {
                    // Network failures, timeouts, and cancellations all degrade silently.
                    // The fallback ladder (PACKS → canned) handles the miss.
                    log.debug("LLM request failed (will fall back): {}", throwable.getMessage());
                    return LlmResponse.parse(null);
                });
    }

}
