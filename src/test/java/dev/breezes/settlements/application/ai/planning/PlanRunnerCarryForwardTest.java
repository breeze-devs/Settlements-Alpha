package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PinnedSelection;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PlanRunner#collectCarriedPins} is the pure projection a hard reset uses to preserve
 * fixed-time placements across plan regeneration — exercised here without any Minecraft object.
 */
class PlanRunnerCarryForwardTest {

    private static final int NOW_CIVIL = 5_000;

    @Test
    void collectCarriedPins_keepsOnlyFuturePinnedSlots() {
        // Arrange
        PlanSlot pinnedFuture = slot(BehaviorKey.TRADE_INITIATE, 9_000, true);
        PlanSlot pinnedPast = slot(BehaviorKey.HARVEST_SUGARCANE, 1_000, true);
        PlanSlot unpinnedFuture = slot(BehaviorKey.MILK_COW, 10_000, false);
        DayPlan oldPlan = DayPlan.builder()
                .slot(pinnedFuture)
                .slot(pinnedPast)
                .slot(unpinnedFuture)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        // Act
        List<PinnedSelection> carried = PlanRunner.collectCarriedPins(oldPlan, NOW_CIVIL);

        // Assert — exactly the future pinned slot survives, mapped with meal=false (carried pins
        // are never default meals, which regenerate fresh instead of being carried).
        assertEquals(List.of(new PinnedSelection(BehaviorKey.TRADE_INITIATE, 9_000, false)), carried);
    }

    @Test
    void collectCarriedPins_dropsElapsedPinExactlyAtNow() {
        // Arrange — a pin AT nowCivil is not strictly future, so it must not be carried.
        PlanSlot pinnedAtNow = slot(BehaviorKey.TRADE_INITIATE, NOW_CIVIL, true);
        DayPlan oldPlan = DayPlan.builder()
                .slot(pinnedAtNow)
                .dayType(PlanDayType.WORK_DAY)
                .calendarDay(1L)
                .schedule(schedule())
                .build();

        // Act
        List<PinnedSelection> carried = PlanRunner.collectCarriedPins(oldPlan, NOW_CIVIL);

        // Assert
        assertTrue(carried.isEmpty());
    }

    @Test
    void collectCarriedPins_nullOldPlanReturnsEmptyList() {
        // Arrange, Act
        List<PinnedSelection> carried = PlanRunner.collectCarriedPins(null, NOW_CIVIL);

        // Assert
        assertTrue(carried.isEmpty());
    }

    private static PlanSlot slot(BehaviorKey key, int startTick, boolean pinned) {
        return PlanSlot.builder()
                .startTick(startTick)
                .behaviorKey(key)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(300)
                .pinned(pinned)
                .status(PlanSlotStatus.PENDING)
                .build();
    }

    private static DayPlanSchedule schedule() {
        return DayPlanSchedule.builder()
                .wakeTick(0)
                .bedtimeTick(12_000)
                .activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(0)
                        .endTick(12_000)
                        .build())
                .build();
    }

}
