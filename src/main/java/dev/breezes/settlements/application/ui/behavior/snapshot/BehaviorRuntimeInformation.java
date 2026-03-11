package dev.breezes.settlements.application.ui.behavior.snapshot;

import dev.breezes.settlements.application.ui.behavior.model.PreconditionSummary;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Builder
public record BehaviorRuntimeInformation(
        @Nullable String currentStageLabel,
        int cooldownRemainingTicks,
        /**
         * Aggregate precondition status with LAST-KNOWN semantics.
         *
         * The producer may return the latest cached evaluation result instead of
         * recomputing preconditions exactly at snapshot time.
         */
        @Nonnull PreconditionSummary preconditionSummary
) {

    public BehaviorRuntimeInformation {
        Objects.requireNonNull(preconditionSummary, "preconditionSummary");
    }

}
