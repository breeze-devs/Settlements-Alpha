package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Static facade over the serialized {@link AttachmentRegistry#VILLAGER_PERSONALITY} attachment.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerPersonalityAttachment {

    public static VillagerPersonality read(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_PERSONALITY);
    }

    public static void write(LivingEntity entity, VillagerPersonality personality) {
        entity.setData(AttachmentRegistry.VILLAGER_PERSONALITY, personality);
    }

}
