package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import net.minecraft.world.entity.LivingEntity;

public final class VillagerHungerAttachment {

    private VillagerHungerAttachment() {
    }

    public static float getHunger(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_HUNGER);
    }

    public static void setHunger(LivingEntity entity, float hunger) {
        entity.setData(AttachmentRegistry.VILLAGER_HUNGER, hunger);
    }

}
