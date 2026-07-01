package dev.breezes.settlements.domain.crafting.catalog;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * The product of a {@link CraftRecipe}.
 * <p>
 * The item is stored as a {@link ResourceLocation} and resolved to a concrete {@code Item} lazily at
 * use-time (via {@code BuiltInRegistries.ITEM.get(itemId)}), mirroring how {@code ItemMatch} stays
 * abstract. Resolving eagerly at parse time would couple catalog loading to registry population order.
 */
public record CraftOutput(
        @Nonnull ResourceLocation itemId,
        int count
) {

    public CraftOutput {
        if (count < 1) {
            throw new IllegalArgumentException("Craft output count must be at least 1");
        }
    }

}
