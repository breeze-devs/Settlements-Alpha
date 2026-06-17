package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZombieSettlementsOriginAttachment {

    public static boolean isSettlementsOrigin(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.ZOMBIE_SETTLEMENTS_ORIGIN);
    }

    public static void markAsSettlementsOrigin(LivingEntity entity) {
        entity.setData(AttachmentRegistry.ZOMBIE_SETTLEMENTS_ORIGIN, true);
    }

}
