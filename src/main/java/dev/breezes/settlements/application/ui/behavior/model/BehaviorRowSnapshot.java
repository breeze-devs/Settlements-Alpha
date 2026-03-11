package dev.breezes.settlements.application.ui.behavior.model;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

@Builder
public record BehaviorRowSnapshot(
        @Nonnull String behaviorId,
        @Nonnull String displayNameKey,
        @Nullable String displaySuffix,
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

    public BehaviorRowSnapshot {
        Objects.requireNonNull(behaviorId, "behaviorId");
        Objects.requireNonNull(displayNameKey, "displayNameKey");
        Objects.requireNonNull(iconItemId, "iconItemId");
        Objects.requireNonNull(registeredSchedules, "registeredSchedules");
        Objects.requireNonNull(preconditionSummary, "preconditionSummary");
        registeredSchedules = List.copyOf(registeredSchedules);
    }

}
