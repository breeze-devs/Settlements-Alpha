package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.application.ai.behavior.teardown.LedgerEntry;

import javax.annotation.Nonnull;
import java.util.List;

public final class TeardownLedgerAttachmentSerializer
        extends StateCodecAttachmentSerializer<List<LedgerEntry>, VillagerTeardownLedger> {

    public TeardownLedgerAttachmentSerializer() {
        super(TeardownLedgerAttachmentCodec.STATE_CODEC);
    }

    @Override
    List<LedgerEntry> toState(@Nonnull VillagerTeardownLedger ledger) {
        return ledger.snapshot();
    }

    @Override
    VillagerTeardownLedger fromState(@Nonnull List<LedgerEntry> entries) {
        return new VillagerTeardownLedger(entries);
    }

}
