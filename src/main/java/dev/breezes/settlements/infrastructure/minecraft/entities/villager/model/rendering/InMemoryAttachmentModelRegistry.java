package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model.rendering;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public final class InMemoryAttachmentModelRegistry implements AttachmentModelRegistry {

    private final Map<ResourceLocation, AttachmentModel> modelsById;

    public InMemoryAttachmentModelRegistry(@Nonnull Map<ResourceLocation, AttachmentModel> modelsById) {
        this.modelsById = Map.copyOf(modelsById);
    }

    @Nullable
    @Override
    public AttachmentModel get(@Nonnull ResourceLocation modelId) {
        return this.modelsById.get(modelId);
    }

}
