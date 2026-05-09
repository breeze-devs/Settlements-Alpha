package dev.breezes.settlements.infrastructure.minecraft.attachments;

import net.minecraft.world.item.ItemStack;

public record VillagerInventorySlotState(int slot, ItemStack stack) {
}
