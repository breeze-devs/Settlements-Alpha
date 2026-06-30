package dev.breezes.settlements.application.ai.inference;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Capability-agnostic transport seam for the external inference backend.
 */
public interface InferenceTransport extends AutoCloseable {

    /**
     * Sends a unary (collect-all-then-return) request and resolves to a single response.
     * General-purpose seam reused by any capability that does not need streaming.
     */
    CompletableFuture<InferenceTransportResponse> post(InferenceCapability capability, Object payload, Duration deadline);

    /**
     * Sends a streaming request that invokes {@code onLine} for each NDJSON line as it arrives,
     * rather than waiting for the full body.
     * <p>
     * The returned handle lets the caller cancel the in-flight exchange (aborting the HTTP
     * connection so the backend sees the disconnect and can free resources) and observe
     * stream completion via a future that never completes exceptionally.
     * <p>
     * Lines are delivered before the deadline fires and before cancel takes effect; the caller
     * must guard at the install site to handle stragglers from a canceled stream.
     *
     * @param capability capability discriminator that selects the backend endpoint
     * @param payload    request body serialized as the capability's expected shape
     * @param deadline   wall-clock budget for the entire stream; lines arriving before expiry keep
     * @param onLine     invoked on the HTTP client's executor thread for each non-blank line
     * @return a handle for cancellation and completion observation
     */
    InferenceStreamHandle postStreaming(InferenceCapability capability, Object payload, Duration deadline, Consumer<String> onLine);

    /**
     * Renders the exact wire request body that {@link #post} would send, without transmitting it.
     * <p>
     * Exposes the full envelope shape — protocol version, a sampled request-id, capability, and
     * deadline fields — so dev tools can capture byte-faithful payloads for offline prompt iteration
     * and gateway-test fixtures, without duplicating the envelope construction logic.
     */
    String renderEnvelope(InferenceCapability capability, Object payload, Duration deadline);

    @Override
    void close();

}
