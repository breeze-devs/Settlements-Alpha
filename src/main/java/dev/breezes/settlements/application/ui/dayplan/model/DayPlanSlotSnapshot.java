package dev.breezes.settlements.application.ui.dayplan.model;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Builder
public record DayPlanSlotSnapshot(
        @Nonnull String behaviorKey,
        @Nonnull String displayNameKey,
        @Nonnull String formattedTime,
        @Nonnull ResourceLocation iconItemId,
        @Nonnull DayPlanSlotVisualStatus status,
        @Nullable String description,
        boolean flexible
) {

}
