package dev.breezes.settlements.infrastructure.minecraft.attachments;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public record VillagerInventorySlotState(int slot,
                                         @Nonnull ItemStack stack) {
}
