package dev.breezes.settlements.domain.attachment;

import dev.breezes.settlements.domain.presentation.ItemCategory;
import lombok.Builder;

import javax.annotation.Nullable;

@Builder
public record RenderableAttachment(AttachmentSlot slot,
                                   AttachmentContent content,
                                   @Nullable ItemCategory category) {
}
