package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nullable;

public final class VillagerDayPlanAttachment {

    private VillagerDayPlanAttachment() {
    }

    @Nullable
    public static DayPlan getDayPlan(BaseVillager villager) {
        return villager.getData(AttachmentRegistry.VILLAGER_DAY_PLAN).plan().orElse(null);
    }

    public static void setDayPlan(BaseVillager villager, DayPlan dayPlan) {
        villager.setData(AttachmentRegistry.VILLAGER_DAY_PLAN, DayPlanAttachmentState.of(dayPlan));
    }

    public static void clearDayPlan(BaseVillager villager) {
        villager.setData(AttachmentRegistry.VILLAGER_DAY_PLAN, DayPlanAttachmentState.empty());
    }

}
