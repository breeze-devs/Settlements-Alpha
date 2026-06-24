package dev.breezes.settlements.domain.farming;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface CultivationCropRegistry {

    Optional<CultivationCropDefinition> resolveBySeedItem(@Nonnull ItemStack stack);

    Optional<CultivationCropDefinition> resolveByCropBlock(@Nonnull ResourceLocation cropBlockId);

    /**
     * Returns all known crop definitions loaded from the datapack.
     * Used by the behavior precondition to build the "any registered seed" demand signal.
     */
    Collection<CultivationCropDefinition> all();

}
