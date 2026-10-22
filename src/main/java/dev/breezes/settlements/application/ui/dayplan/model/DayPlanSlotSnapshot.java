package dev.breezes.settlements.application.ui.dayplan.model;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@Builder
public record DayPlanSlotSnapshot(
        String behaviorKey,
        String displayNameKey,
        String formattedTime,
        ResourceLocation iconItemId,
        DayPlanSlotVisualStatus status,
        @Nullable String description,
        boolean flexible
) {

}
