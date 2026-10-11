package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.inventory.BackpackEntry;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;

import javax.annotation.Nonnull;
import java.util.List;

public final class VillagerInventoryAttachmentSerializer
        extends StateCodecAttachmentSerializer<List<BackpackEntry>, VillagerInventory> {

    public VillagerInventoryAttachmentSerializer() {
        super(VillagerInventoryAttachmentCodec.STATE_CODEC);
    }

    @Override
    List<BackpackEntry> toState(@Nonnull VillagerInventory inventory) {
        return inventory.entries();
    }

    @Override
    VillagerInventory fromState(@Nonnull List<BackpackEntry> entries) {
        VillagerInventory inventory = new VillagerInventory(() -> GeneralConfig.bypassInventoryRequirements);
        for (BackpackEntry entry : entries) {
            if (entry.count() <= 0 || entry.representative().isEmpty()) {
                continue;
            }
            inventory.add(entry.representative().copyWithCount(entry.count()));
        }
        return inventory;
    }

}
