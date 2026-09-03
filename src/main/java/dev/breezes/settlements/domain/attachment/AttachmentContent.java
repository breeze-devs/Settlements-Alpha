package dev.breezes.settlements.domain.attachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public sealed interface AttachmentContent permits AttachmentContent.ItemContent,
        AttachmentContent.ModelContent,
        AttachmentContent.BillboardContent {

    record ItemContent(@Nonnull ItemStack stack) implements AttachmentContent {
    }

    record ModelContent(@Nonnull ResourceLocation modelId,
                        @Nonnull ResourceLocation texture) implements AttachmentContent {
    }

    record BillboardContent(@Nonnull ResourceLocation texture) implements AttachmentContent {
    }

}
