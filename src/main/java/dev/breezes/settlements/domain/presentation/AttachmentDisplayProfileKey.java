package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;

import javax.annotation.Nonnull;

public record AttachmentDisplayProfileKey(AttachmentSlot slot,
                                          ItemCategory category) {

    public static AttachmentDisplayProfileKey of(@Nonnull AttachmentSlot slot, @Nonnull ItemCategory category) {
        return new AttachmentDisplayProfileKey(slot, category);
    }

}
