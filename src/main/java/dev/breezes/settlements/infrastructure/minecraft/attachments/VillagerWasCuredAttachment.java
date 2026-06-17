package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerWasCuredAttachment {

    public static boolean wasCured(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_WAS_CURED);
    }

    public static void markAsCured(LivingEntity entity) {
        entity.setData(AttachmentRegistry.VILLAGER_WAS_CURED, true);
    }

}
