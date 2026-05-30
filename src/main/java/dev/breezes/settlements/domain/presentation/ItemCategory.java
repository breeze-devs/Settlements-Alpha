package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
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
    MAP,
    GENERIC;

    public static ItemCategory of(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return GENERIC;
        }

        Item item = stack.getItem();
        switch (item) {
            case AxeItem axeItem -> {
                return AXE;
            }
            case SwordItem swordItem -> {
                return SWORD;
            }
            case PickaxeItem pickaxeItem -> {
                return PICKAXE;
            }
            case ShovelItem shovelItem -> {
                return SHOVEL;
            }
            case HoeItem hoeItem -> {
                return HOE;
            }
            case FishingRodItem fishingRodItem -> {
                return FISHING_ROD;
            }
            default -> {
            }
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
        if (stack.is(Items.MAP) || stack.is(Items.FILLED_MAP)) {
            return MAP;
        }
        if (stack.is(ItemRegistry.VILLAGER_FISHING_ROD.get())) {
            return FISHING_ROD;
        }

        return GENERIC;
    }

}
