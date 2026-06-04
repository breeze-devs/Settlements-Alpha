package dev.breezes.settlements.application.ai.behavior.teardown;

import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;

/**
 * A cancellable reference to a tracked obligation, returned by {@link TeardownScope#track}.
 * <p>
 * Use {@link #dispose} when a behavior needs to release an artifact mid-run
 * (e.g. swapping the initial block display for the final one) rather than
 * waiting for end-of-behavior teardown. Disposing eagerly also removes the
 * entry from the ledger, keeping crash-recovery accurate.
 * <p>
 * Both {@link #dispose} and {@link TeardownScope#teardownAll} are safe to call
 * in any order — duplicate discharge is a no-op.
 */
@CustomLog
public final class TemporaryArtifactHandle {

    private final TeardownObligation obligation;
    private final TeardownScope scope;
    @Getter
    private boolean disposed;

    public TemporaryArtifactHandle(@Nonnull TeardownObligation obligation, @Nonnull TeardownScope scope) {
        this.obligation = obligation;
        this.scope = scope;
        this.disposed = false;
    }

    /**
     * Stop tracking this obligation WITHOUT discharging it. Used when ownership of the artifact
     * transfers elsewhere (e.g. a claimed bed handed to a newborn) so end-of-behavior teardown
     * must not revert it. Contrast with {@link #dispose}, which discharges.
     */
    public void cancel() {
        if (this.disposed) {
            return;
        }
        this.disposed = true;
        this.scope.removeObligation(this.obligation);
    }

    public void dispose(@Nonnull ServerLevel level) {
        if (this.disposed) {
            return;
        }

        this.disposed = true;
        this.scope.removeObligation(this.obligation);

        try {
            if (this.obligation.stillValid(level)) {
                this.obligation.discharge(level);
            }
        } catch (Exception e) {
            log.error("Failed to discharge obligation via handle '{}': {}", this.obligation.describe(), e.getMessage());
        }
    }

}
