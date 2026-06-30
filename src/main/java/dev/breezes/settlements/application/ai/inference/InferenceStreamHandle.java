package dev.breezes.settlements.application.ai.inference;

import java.util.concurrent.CompletableFuture;

/**
 * Handle to an in-flight streaming inference request.
 * <p>
 * Exposes two operations: cancellation (which must reach the source HTTP exchange so the backend
 * sees the disconnect and can free GPU resources) and a completion signal (which fires when the
 * stream ends by any means — success, deadline, cancellation, or error — never exceptionally).
 * <p>
 * Implementations must cancel the source {@link java.net.http.HttpClient#sendAsync} future
 * directly, not a derived stage, because {@link CompletableFuture#cancel} does not propagate
 * upstream through chained {@code thenApply}/{@code thenCompose} stages.
 */
public interface InferenceStreamHandle {

    /**
     * Aborts the in-flight HTTP exchange. The cancellation may race with lines already delivered
     * to the body subscriber — callers must guard at the install site (epoch check) to drop
     * any stragglers.
     * <p>
     * Safe to call from any thread. Idempotent after stream completion.
     */
    void cancel();

    /**
     * Future that completes (normally, with {@code null}) when the stream ends — by success,
     * deadline expiry, cancellation, or error. Never completes exceptionally.
     */
    CompletableFuture<Void> completion();

    /**
     * Shared singleton handle that is already complete and whose {@link #cancel()} is a no-op.
     * Stateless and immutable, so one instance is safely shared across all call sites — used as the
     * empty-input short-circuit and as the initial sentinel in epoch-managing callers.
     */
    InferenceStreamHandle NO_OP = new InferenceStreamHandle() {
        @Override
        public void cancel() {
            // No exchange in flight — nothing to abort.
        }

        @Override
        public CompletableFuture<Void> completion() {
            // Fresh completed future per call so a caller can never mutate shared state.
            return CompletableFuture.completedFuture(null);
        }
    };

    /**
     * Returns the shared {@link #NO_OP} handle.
     */
    static InferenceStreamHandle noOp() {
        return NO_OP;
    }

}
