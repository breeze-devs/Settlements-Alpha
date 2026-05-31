package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.inventory.BackpackEntry;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;

public final class VillagerInventoryAttachment {

    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull VillagerInventory inventory) {
        VillagerInventoryAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_INVENTORY);
        if (!state.initialized()) {
            return false;
        }

        for (BackpackEntry entry : state.entries()) {
            if (entry.count() <= 0 || entry.representative().isEmpty()) {
                continue;
            }
            inventory.add(entry.representative().copyWithCount(entry.count()));
        }
        return true;
    }

    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull VillagerInventory inventory) {
        villager.setData(AttachmentRegistry.VILLAGER_INVENTORY, VillagerInventoryAttachmentState.of(inventory.entries()));
    }

}
