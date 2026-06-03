package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Application port for the per-villager persisted crash-recovery ledger.
 * <p>
 * The live in-process {@link TeardownScope} writes durable obligations through to
 * this ledger at track-time. This keeps the ledger as the single, always-current
 * record of outstanding obligations, so the save hook ({@code addAdditionalSaveData})
 * only has to snapshot it rather than enumerate the live behavior scopes.
 * <p>
 * Persistence itself still happens only at save-time: an obligation survives a crash
 * only if a save occurred between its creation and the crash. A hard kill before the
 * next save — or an obligation created and discharged entirely between two saves —
 * leaves nothing on disk. Crash recovery is therefore best-effort, and most useful for
 * longer-lived artifacts (e.g. a furnace left lit across a smelt); a short-lived artifact
 * that is simply gone after a crash is handled by {@link TeardownObligation#stillValid}
 * returning {@code false}, which the reconciler treats as a safe no-op.
 * <p>
 * Implementation lives in the infrastructure layer
 * ({@code VillagerTeardownLedger}) and is bound into the scope via
 * {@link TeardownScope#bindLedger} in {@code StateMachineBehavior.doStart}.
 * <p>
 * The ledger maintains two logical sets internally:
 * <ul>
 *   <li><b>Orphans</b> — obligations loaded from NBT at reload; not managed by any live
 *       {@link TeardownScope}; processed by the per-villager reconciler.</li>
 *   <li><b>Live</b> — obligations tracked by the current behavior run(s);
 *       managed by their {@link TeardownScope}; not touched by the reconciler.</li>
 * </ul>
 * On save, both sets are merged and written to NBT.  On the next reload, all entries
 * come back as orphans, so obligations that survived a crash are automatically
 * picked up by the reconciler.
 */
public interface ITeardownLedger {

    /**
     * Called by {@link TeardownScope#track} when a durable obligation is created.
     * The obligation is placed in the "live" set and will be written to NBT on save.
     */
    void add(@Nonnull TeardownObligation obligation);

    /**
     * Called by {@link TeardownScope#teardownAll} and {@link TeardownScope#removeObligation}
     * when a live obligation is discharged or disposed mid-run.
     * Removes the obligation from the "live" set; does not touch orphan entries.
     */
    void remove(@Nonnull TeardownObligation obligation);

    /**
     * Non-destructive view of crash-orphaned {@link LedgerEntry}s awaiting reconciliation.
     * <p>
     * The returned list must not be mutated by callers.  Entries are removed from this set
     * via {@link #resolveOrphan} once the reconciler successfully discharges or confirms
     * the target is gone.  Entries remain until explicitly resolved so that {@code failedAttempts}
     * accumulates correctly across short reload sessions.
     */
    List<LedgerEntry> pendingOrphans();

    /**
     * Remove an orphaned entry from the ledger after it has been successfully discharged
     * or confirmed gone (i.e. {@link TeardownObligation#stillValid} returned {@code false}).
     * Also called when the circuit breaker triggers to abandon a stuck entry.
     */
    void resolveOrphan(@Nonnull TeardownObligation obligation);

    /**
     * Whether a currently-live obligation targets the given position. The reconciler uses this
     * to skip orphans whose resource a new behavior run has already re-acquired, so a stale
     * orphan discharge cannot clobber the live run's state.
     */
    boolean hasLiveObligationAt(@Nonnull BlockPos pos);

    /**
     * Snapshot of all outstanding obligations — orphans plus live — for NBT persistence.
     * Live obligations are wrapped in a new {@link LedgerEntry} with {@code failedAttempts=0}.
     */
    List<LedgerEntry> snapshot();

}
