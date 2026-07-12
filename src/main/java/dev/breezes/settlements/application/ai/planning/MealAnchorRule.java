package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.Chronotype;
import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;

import javax.annotation.Nullable;

/**
 * One row of the data-driven meal anchor table, replacing the three inline meal-slot builder
 * blocks the day plan used to hardcode.
 * <p>
 * A row's placement tick is resolved by {@link DayPlanComposer} against the plan's frame: either
 * relative to wake ({@link AnchorMode#WAKE_RELATIVE}, breakfast's mode — it moves with wake
 * automatically) or a fixed {@link TimeOfDay} ({@link AnchorMode#FIXED}, lunch/dinner's mode).
 *
 * @param fixedTime                   the anchor time when {@code anchorMode == FIXED}; {@code null}
 *                                    when {@code anchorMode == WAKE_RELATIVE}, since that mode
 *                                    derives its tick from the frame's wake instead
 * @param appliesChronotypeMealOffset whether {@link Chronotype#mealOffsetTicks()} shifts this
 *                                    anchor's placement; breakfast already moves with wake and so
 *                                    does not carry this additional offset
 * @param authoredKeepProbability     the authored floor-table probability this meal survives its
 *                                    independent per-meal skip roll on a given day (1.0 = never
 *                                    skips); rolled via {@code random.nextDouble() < this value}.
 */
@Builder
public record MealAnchorRule(AnchorMode anchorMode,
                             @Nullable TimeOfDay fixedTime,
                             BehaviorKey behaviorKey,
                             int priority,
                             int durationTicks,
                             double authoredKeepProbability,
                             boolean appliesChronotypeMealOffset) {

    public enum AnchorMode {
        WAKE_RELATIVE,
        FIXED
    }

    /**
     * Resolves this row's civil placement tick against a plan frame's {@code wakeCivil} and
     * {@code mealOffsetTicks}. This is the single source of truth for "where does this meal sit,"
     * shared by {@link DayPlanComposer}'s anchor stage and {@link MinimalPlanFactory}'s floor plan,
     * so the two never drift.
     */
    public int placementTick(int wakeCivil, int mealOffsetTicks) {
        int baseTick = this.anchorMode == AnchorMode.WAKE_RELATIVE
                ? wakeCivil
                : this.fixedTime.getCivilTick();
        return this.appliesChronotypeMealOffset ? baseTick + mealOffsetTicks : baseTick;
    }

}
