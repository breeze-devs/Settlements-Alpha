package dev.breezes.settlements.application.ai.behavior.teardown;

import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;

/**
 * Per-behavior-run working set of {@link TeardownObligation}s.
 * <p>
 * Obligations are tracked at the moment artifacts are created and discharged
 * exactly once on every termination path — normal stop, precondition failure,
 * panic interrupt, death, or chunk-unload.
 * <p>
 * Teardown is LIFO so that artifacts created last are released first, and each
 * discharge is exception-isolated so one failure does not block the rest.
 * <p>
 * The optional {@link ITeardownLedger} binding mirrors every durable
 * obligation to a per-villager persisted ledger so crash-orphaned artifacts can
 * be reclaimed on the next reload without a world-level sweep.
 */
@CustomLog
public final class TeardownScope {

    // Head of deque = most recently tracked (LIFO pop order)
    private final Deque<TeardownObligation> live = new ArrayDeque<>();

    @Nullable
    private ITeardownLedger ledger;

    /**
     * Register an artifact obligation.  Returns an {@link TemporaryArtifactHandle} for
     * callers that need mid-run disposal (early release + ledger removal).
     * Ignoring the handle is safe — {@link #teardownAll} covers all remaining
     * obligations.
     */
    public TemporaryArtifactHandle track(@Nonnull TeardownObligation obligation) {
        this.live.push(obligation);
        if (this.ledger != null && obligation.durable()) {
            this.ledger.add(obligation);
        }

        return new TemporaryArtifactHandle(obligation, this);
    }

    /**
     * Discharge all remaining obligations in LIFO order, each inside its own
     * try/catch so one failure does not block the rest.  Idempotent: safe to
     * call on an already-empty scope.
     */
    public void teardownAll(@Nonnull ServerLevel level) {
        while (!this.live.isEmpty()) {
            TeardownObligation obligation = this.live.pop();
            try {
                if (obligation.stillValid(level)) {
                    obligation.discharge(level);
                }
            } catch (Exception e) {
                log.error("Teardown obligation failed '{}': {}", obligation.describe(), e.getMessage());
            } finally {
                // Remove only this scope's own obligation from the ledger, mirroring track().
                // Never drainPending() here: that would also clear un-reconciled crash-orphans
                // left by a previous run, silently leaking the artifacts Dim 3 exists to reclaim.
                if (this.ledger != null && obligation.durable()) {
                    this.ledger.remove(obligation);
                }
            }
        }
    }

    /**
     * Bind a Dim 3 crash-recovery ledger.  Called in {@code StateMachineBehavior.doStart}
     * when the entity implements {@code ProvidesTeardownLedger}.
     */
    public void bindLedger(@Nonnull ITeardownLedger ledger) {
        this.ledger = ledger;
    }

    /**
     * Remove a single obligation from this scope and the ledger (used by {@link TemporaryArtifactHandle#dispose}).
     */
    void removeObligation(@Nonnull TeardownObligation obligation) {
        this.live.remove(obligation);
        if (this.ledger != null && obligation.durable()) {
            this.ledger.remove(obligation);
        }
    }

    /**
     * Read-only view of open obligations — for introspection and tests.
     */
    public Collection<TeardownObligation> openObligations() {
        return Collections.unmodifiableCollection(this.live);
    }

}
