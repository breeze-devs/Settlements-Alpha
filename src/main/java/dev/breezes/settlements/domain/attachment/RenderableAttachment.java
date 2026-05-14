package dev.breezes.settlements.domain.attachment;

import dev.breezes.settlements.domain.presentation.ItemCategory;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
public record RenderableAttachment(@Nonnull AttachmentSlot slot,
                                   @Nonnull AttachmentContent content,
                                   @Nullable ItemCategory category) {
}
