package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.application.ai.behavior.teardown.LedgerEntry;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Persisted snapshot of the per-villager teardown ledger.
 * Written to NBT via {@link TeardownLedgerAttachmentCodec#STATE_CODEC} on each save
 * and read back on reload to populate the crash-orphan reconciliation set.
 */
public record TeardownLedgerAttachmentState(@Nonnull List<LedgerEntry> entries) {

    public static TeardownLedgerAttachmentState empty() {
        return new TeardownLedgerAttachmentState(List.of());
    }

    public static TeardownLedgerAttachmentState of(@Nonnull List<LedgerEntry> entries) {
        return new TeardownLedgerAttachmentState(List.copyOf(entries));
    }

}
