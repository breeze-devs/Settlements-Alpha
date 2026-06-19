package dev.breezes.settlements.application.ai.inference;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Capability-agnostic transport seam for the external inference backend.
 */
public interface InferenceTransport extends AutoCloseable {

    CompletableFuture<InferenceTransportResponse> post(InferenceCapability capability, Object payload, Duration deadline);

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
