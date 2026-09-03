package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Resolves a stable attachment-model id to its baked client model.
 */
public interface AttachmentModelRegistry {

    /**
     * Returns the baked model for the given id, or null if nothing was baked under it.
     */
    @Nullable
    AttachmentModel get(@Nonnull ResourceLocation modelId);

}
