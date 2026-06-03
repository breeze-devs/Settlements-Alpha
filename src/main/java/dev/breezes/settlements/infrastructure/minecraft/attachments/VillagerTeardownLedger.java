package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.application.ai.behavior.teardown.ITeardownLedger;
import dev.breezes.settlements.application.ai.behavior.teardown.LedgerEntry;
import dev.breezes.settlements.application.ai.behavior.teardown.TeardownObligation;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-villager {@link ITeardownLedger} implementation backed by two logical sets:
 * <ol>
 *   <li><b>Orphans</b> — obligations loaded from NBT at reload.  These are crash-survivors
 *       that have no live {@link dev.breezes.settlements.application.ai.behavior.teardown.TeardownScope}
 *       owning them; the per-villager reconciler processes them in {@code customServerAiStep}.</li>
 *   <li><b>Live</b> — obligations tracked by the currently running behavior.  Managed
 *       exclusively through the behavior's {@code TeardownScope}; the reconciler never touches
 *       this set.</li>
 * </ol>
 * On save ({@link #snapshot}), both sets are merged so that a crash at any moment
 * leaves all outstanding obligations recoverable on the next reload.
 */
public final class VillagerTeardownLedger implements ITeardownLedger {

    /**
     * Crash-orphaned entries loaded from NBT.  Mutable: the reconciler removes entries as
     * they are resolved and increments {@link LedgerEntry#incrementFailedAttempts()} in-place.
     */
    private final List<LedgerEntry> orphans;

    /**
     * Obligations tracked by the current behavior run.  Managed via {@link #add}/{@link #remove}.
     */
    private final List<TeardownObligation> live;

    public VillagerTeardownLedger(@Nonnull List<LedgerEntry> loadedOrphans) {
        this.orphans = new ArrayList<>(loadedOrphans);
        this.live = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // ITeardownLedger — write-through from TeardownScope
    // -------------------------------------------------------------------------

    @Override
    public void add(@Nonnull TeardownObligation obligation) {
        this.live.add(obligation);
    }

    @Override
    public void remove(@Nonnull TeardownObligation obligation) {
        this.live.remove(obligation);
    }

    // -------------------------------------------------------------------------
    // ITeardownLedger — reconciler interface
    // -------------------------------------------------------------------------

    @Override
    public List<LedgerEntry> pendingOrphans() {
        return Collections.unmodifiableList(this.orphans);
    }

    @Override
    public void resolveOrphan(@Nonnull TeardownObligation obligation) {
        // Identity equality (==) is intentional: the reconciler holds direct references
        // to entries from pendingOrphans(), so we remove exactly the entry it resolved.
        this.orphans.removeIf(entry -> entry.getObligation() == obligation);
    }

    @Override
    public boolean hasLiveObligationAt(@Nonnull BlockPos pos) {
        for (TeardownObligation obligation : this.live) {
            if (obligation.targetPos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // ITeardownLedger — persistence
    // -------------------------------------------------------------------------

    @Override
    public List<LedgerEntry> snapshot() {
        List<LedgerEntry> result = new ArrayList<>(this.orphans);
        for (TeardownObligation obligation : this.live) {
            // Live obligations start with failedAttempts=0.
            result.add(new LedgerEntry(obligation, 0));
        }
        return result;
    }

}
