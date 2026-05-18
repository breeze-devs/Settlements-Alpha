package dev.breezes.settlements.domain.economy.catalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public final class ItemMatches {

    public static boolean test(@Nonnull ItemMatch match, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (match) {
            case ItemMatch.ItemRef ref -> stack.is(BuiltInRegistries.ITEM.get(ref.id()));
            case ItemMatch.TagRef tagRef -> stack.is(tagRef.tag());
        };
    }

}
