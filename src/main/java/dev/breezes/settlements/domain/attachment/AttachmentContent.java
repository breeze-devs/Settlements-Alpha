package dev.breezes.settlements.domain.attachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public sealed interface AttachmentContent permits AttachmentContent.ItemContent,
        AttachmentContent.ModelContent,
        AttachmentContent.BillboardContent {

    record ItemContent(ItemStack stack) implements AttachmentContent {
    }

    record ModelContent(ResourceLocation modelId,
                        ResourceLocation texture) implements AttachmentContent {
    }

    record BillboardContent(ResourceLocation texture) implements AttachmentContent {
    }

}
