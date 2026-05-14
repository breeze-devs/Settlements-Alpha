package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AttachmentDisplayProfileRegistry {

    AttachmentDisplayProfile get(@Nonnull AttachmentSlot slot, @Nullable ItemCategory category);

}
