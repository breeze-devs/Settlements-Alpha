package dev.breezes.settlements.domain.presentation;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import javax.annotation.Nonnull;

// TODO: Deprecate for a more scalable way?
public enum ItemCategory {

    AXE,
    SWORD,
    MACE,
    PICKAXE,
    SHOVEL,
    HOE,
    FISHING_ROD,
    TORCH,
    LANTERN,
    SPYGLASS,
    GENERIC;

    public static ItemCategory of(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return GENERIC;
        }

        Item item = stack.getItem();
        if (item instanceof AxeItem) {
            return AXE;
        }
        if (item instanceof SwordItem) {
            return SWORD;
        }
        if (item instanceof PickaxeItem) {
            return PICKAXE;
        }
        if (item instanceof ShovelItem) {
            return SHOVEL;
        }
        if (item instanceof HoeItem) {
            return HOE;
        }
        if (item instanceof FishingRodItem) {
            return FISHING_ROD;
        }

        // Keep item-specific taxonomy centralized so render and animation code never branch on concrete items.
        if (stack.is(Items.MACE)) {
            return MACE;
        }
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH)) {
            return TORCH;
        }
        if (stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN)) {
            return LANTERN;
        }
        if (stack.is(Items.SPYGLASS)) {
            return SPYGLASS;
        }

        return GENERIC;
    }

}
