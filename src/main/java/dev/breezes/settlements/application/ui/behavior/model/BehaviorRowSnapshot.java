package dev.breezes.settlements.application.ui.behavior.model;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Builder
public record BehaviorRowSnapshot(
        @Nonnull String behaviorId,
        @Nonnull String displayNameKey,
        @Nonnull ResourceLocation iconItemId,
        int priority,
        int uiBehaviorIndex,
        @Nonnull List<SchedulePhase> registeredSchedules,
        boolean running,
        @Nullable String currentStageLabel,
        int cooldownRemainingTicks,
        /**
         * Aggregate precondition status using LAST-KNOWN semantics.
         * <p>
         * This value reflects the most recent behavior-side precondition evaluation,
         * and may be older than the current snapshot tick.
         */
        @Nonnull PreconditionSummary preconditionSummary
) {

}
