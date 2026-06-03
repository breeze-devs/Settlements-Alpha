package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;

public final class VillagerTeardownLedgerAttachment {

    /**
     * Creates and returns a fresh {@link VillagerTeardownLedger} populated from the
     * villager's persisted NBT attachment.  Entries loaded from NBT become crash-orphans
     * immediately available to the reconciler.
     */
    @Nonnull
    public static VillagerTeardownLedger loadInto(@Nonnull BaseVillager villager) {
        TeardownLedgerAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_TEARDOWN_LEDGER);
        return new VillagerTeardownLedger(state.entries());
    }

    /**
     * Persists the ledger's current snapshot (orphans + live obligations) to the
     * villager's NBT attachment so they survive the next save/crash.
     */
    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull VillagerTeardownLedger ledger) {
        villager.setData(AttachmentRegistry.VILLAGER_TEARDOWN_LEDGER,
                TeardownLedgerAttachmentState.of(ledger.snapshot()));
    }

}
