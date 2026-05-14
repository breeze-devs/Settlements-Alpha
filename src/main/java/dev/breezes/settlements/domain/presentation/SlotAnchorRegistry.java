package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;

import javax.annotation.Nonnull;

public interface SlotAnchorRegistry {

    SlotAnchor get(@Nonnull AttachmentSlot slot);

}
