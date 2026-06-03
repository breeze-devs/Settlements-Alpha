package dev.breezes.settlements.application.ai.behavior.teardown;

import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Persisted wrapper around a {@link TeardownObligation} that carries reconciliation metadata.
 * <p>
 * Entries in the "crash-orphan" set are processed by the per-villager reconciler
 * in {@code customServerAiStep}.  {@code failedAttempts} accumulates across short
 * reload sessions (it is saved to NBT) so the circuit-breaker fires on truly stuck
 * targets, not on targets whose chunk happened to be unloaded for a while.
 * <p>
 * Mutable by design: the reconciler increments {@code failedAttempts} in-place
 * rather than replacing the entry, which avoids list-rebuild allocations every tick.
 */
@Getter
public final class LedgerEntry {

    private final TeardownObligation obligation;

    /**
     * Number of times the reconciler attempted to discharge this obligation while the
     * target's chunk was loaded and {@link TeardownObligation#stillValid} returned
     * {@code true}, but the discharge did not take.
     */
    private int failedAttempts;

    public LedgerEntry(@Nonnull TeardownObligation obligation, int failedAttempts) {
        this.obligation = obligation;
        this.failedAttempts = failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

}
