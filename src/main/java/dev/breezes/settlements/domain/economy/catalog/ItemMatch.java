package dev.breezes.settlements.domain.economy.catalog;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import javax.annotation.Nonnull;

/**
 * Represents a match rule without forcing callers to immediately expand tags.
 * Matching stays abstract here so inventory-aware code can resolve it against concrete stacks.
 */
public sealed interface ItemMatch permits ItemMatch.ItemRef, ItemMatch.TagRef {

    String asDebugString();

    record ItemRef(@Nonnull ResourceLocation id) implements ItemMatch {

        @Override
        public String asDebugString() {
            return this.id.toString();
        }

    }

    record TagRef(@Nonnull TagKey<Item> tag) implements ItemMatch {

        public TagRef {
            if (!tag.registry().equals(Registries.ITEM)) {
                throw new IllegalArgumentException("Item match tags must belong to the item registry");
            }
        }

        @Override
        public String asDebugString() {
            return "#" + this.tag.location();
        }

    }

}
