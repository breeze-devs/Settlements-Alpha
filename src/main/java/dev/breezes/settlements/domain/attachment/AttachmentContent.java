package dev.breezes.settlements.domain.attachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public sealed interface AttachmentContent permits AttachmentContent.ItemContent,
        AttachmentContent.ModelContent,
        AttachmentContent.BillboardContent {

    record ItemContent(@Nonnull ItemStack stack) implements AttachmentContent {
    }

    /**
     * Carries a stable model id instead of a baked client model so the attachment plane stays safe for common code.
     * The client renderer can resolve this id once custom attachment models are introduced.
     */
    record ModelContent(@Nonnull ResourceLocation modelId) implements AttachmentContent {
    }

    record BillboardContent(@Nonnull ResourceLocation texture) implements AttachmentContent {
    }

}
