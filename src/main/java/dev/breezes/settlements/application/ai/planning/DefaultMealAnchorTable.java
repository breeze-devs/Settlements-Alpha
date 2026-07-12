package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;

import java.util.List;

/**
 * The mod's default meal floor: breakfast at wake, lunch at 11:00, dinner at 16:30
 */
public final class DefaultMealAnchorTable {

    public static List<MealAnchorRule> rows() {
        return List.of(
                MealAnchorRule.builder()
                        .anchorMode(MealAnchorRule.AnchorMode.WAKE_RELATIVE)
                        .behaviorKey(BehaviorKey.EAT_FOOD)
                        .priority(100)
                        .durationTicks(GameTicks.minutes(8).getTicksAsInt())
                        .authoredKeepProbability(1.00)
                        .appliesChronotypeMealOffset(false)
                        .build(),
                MealAnchorRule.builder()
                        .anchorMode(MealAnchorRule.AnchorMode.FIXED)
                        .fixedTime(TimeOfDay.AT_11_00)
                        .behaviorKey(BehaviorKey.EAT_FOOD)
                        .priority(95)
                        .durationTicks(GameTicks.minutes(10).getTicksAsInt())
                        .authoredKeepProbability(0.95)
                        .appliesChronotypeMealOffset(true)
                        .build(),
                MealAnchorRule.builder()
                        .anchorMode(MealAnchorRule.AnchorMode.FIXED)
                        .fixedTime(TimeOfDay.AT_16_30)
                        .behaviorKey(BehaviorKey.EAT_FOOD)
                        .priority(90)
                        .durationTicks(GameTicks.minutes(10).getTicksAsInt())
                        .authoredKeepProbability(0.90)
                        .appliesChronotypeMealOffset(true)
                        .build()
        );
    }

}
