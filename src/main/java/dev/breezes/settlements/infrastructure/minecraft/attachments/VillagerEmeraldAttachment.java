package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

public final class VillagerEmeraldAttachment {

    public static final int INITIAL_EMERALD_COUNT = 200;

    public static int getEmeralds(BaseVillager villager) {
        return villager.getData(AttachmentRegistry.VILLAGER_EMERALDS);
    }

    public static void setEmeralds(BaseVillager villager, int emeralds) {
        villager.setData(AttachmentRegistry.VILLAGER_EMERALDS, emeralds);
    }

}
