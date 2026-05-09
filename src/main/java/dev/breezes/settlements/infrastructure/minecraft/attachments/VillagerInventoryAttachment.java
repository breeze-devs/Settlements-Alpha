package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class VillagerInventoryAttachment {

    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull VillagerInventory inventory) {
        VillagerInventoryAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_INVENTORY);
        if (!state.initialized()) {
            return false;
        }

        SimpleContainer backpack = inventory.getBackpack();
        for (VillagerInventorySlotState slotState : state.slots()) {
            if (slotState.slot() < 0 || slotState.slot() >= backpack.getContainerSize()) {
                continue;
            }

            backpack.setItem(slotState.slot(), slotState.stack().copy());
        }
        return true;
    }

    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull VillagerInventory inventory) {
        villager.setData(AttachmentRegistry.VILLAGER_INVENTORY, VillagerInventoryAttachmentState.of(toSlotStates(inventory)));
    }

    private static List<VillagerInventorySlotState> toSlotStates(@Nonnull VillagerInventory inventory) {
        SimpleContainer backpack = inventory.getBackpack();
        List<VillagerInventorySlotState> slots = new ArrayList<>();
        for (int slot = 0; slot < backpack.getContainerSize(); slot++) {
            ItemStack stack = backpack.getItem(slot);
            if (!stack.isEmpty()) {
                slots.add(new VillagerInventorySlotState(slot, stack.copy()));
            }
        }
        return slots;
    }

}
