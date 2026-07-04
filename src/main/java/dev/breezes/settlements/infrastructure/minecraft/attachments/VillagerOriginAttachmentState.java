package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.personality.OriginType;

/**
 * The {@code initialized} sentinel distinguishes "never stamped" (old save, or an entity that
 * skipped every genesis seam) from "stamped {@link OriginType#UNKNOWN}" — both read as UNKNOWN via
 * {@code VillagerOriginAttachment#read}, but only the latter reflects a genesis seam that actually ran.
 */
public record VillagerOriginAttachmentState(boolean initialized, OriginType origin) {

    public static VillagerOriginAttachmentState empty() {
        return new VillagerOriginAttachmentState(false, OriginType.UNKNOWN);
    }

    public static VillagerOriginAttachmentState of(OriginType origin) {
        return new VillagerOriginAttachmentState(true, origin);
    }

}
