package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.personality.OriginType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Static facade over the serialized {@link AttachmentRegistry#VILLAGER_ORIGIN} attachment.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerOriginAttachment {

    public static void stamp(LivingEntity entity, OriginType origin) {
        entity.setData(AttachmentRegistry.VILLAGER_ORIGIN, VillagerOriginAttachmentState.of(origin));
    }

    public static OriginType read(LivingEntity entity) {
        VillagerOriginAttachmentState state = entity.getData(AttachmentRegistry.VILLAGER_ORIGIN);
        return state.initialized() ? state.origin() : OriginType.UNKNOWN;
    }

    public static boolean isStamped(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_ORIGIN).initialized();
    }

}
