package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.ai.planning.DayPlan;

import java.util.Optional;

public record DayPlanAttachmentState(Optional<DayPlan> plan) {

    public static DayPlanAttachmentState empty() {
        return new DayPlanAttachmentState(Optional.empty());
    }

    public static DayPlanAttachmentState of(DayPlan plan) {
        return new DayPlanAttachmentState(Optional.of(plan));
    }

}
