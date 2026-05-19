package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayPlanAttachmentCodecTest {

    @Test
    void stateCodec_roundTripsPersistedPlanState() {
        // Arrange
        PlanSlot completedSlot = slot(BehaviorKey.EAT_FOOD, 23_000, PlanSlotStatus.COMPLETED);
        PlanSlot pendingSlot = slot(BehaviorKey.TRADE_INITIATE, 500, PlanSlotStatus.PENDING);
        DayPlan plan = DayPlan.builder()
                .slot(pendingSlot)
                .slot(completedSlot)
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(47_000L)
                .status(PlanStatus.ACTIVE)
                .currentSlotIndex(1)
                .schedule(schedule(23_000))
                .dayStartTick(23_000)
                .build();

        // Act
        DayPlan decoded = DayPlanAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE,
                        DayPlanAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, DayPlanAttachmentState.of(plan)).resultOrPartial(Assertions::fail).orElseThrow())
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst()
                .plan()
                .orElseThrow();

        // Assert
        assertEquals(PlanDayType.WORK_DAY, decoded.getDayType());
        assertEquals(47_000L, decoded.getWakeAtAbsoluteTick());
        assertEquals(2L, decoded.getCalendarDay());
        assertEquals(1, decoded.getCurrentSlotIndex());
        assertEquals(23_000, decoded.getDayStartTick());
        assertEquals(23_000, decoded.getSchedule().wakeTick());
        assertEquals(DayPlanActivityContext.IDLE, decoded.getSchedule().activityBlocks().getFirst().context());
        assertEquals(PlanStatus.PENDING, decoded.getStatus());
        assertEquals(BehaviorKey.EAT_FOOD, decoded.getSlots().get(0).getBehaviorKey());
        assertEquals(PlanSlotStatus.COMPLETED, decoded.getSlots().get(0).getStatus());
        assertEquals(BehaviorKey.TRADE_INITIATE, decoded.getSlots().get(1).getBehaviorKey());
        assertEquals(PlanSlotStatus.PENDING, decoded.getSlots().get(1).getStatus());
    }

    @Test
    void stateCodec_normalizesActiveSlotsToPending() {
        // Arrange
        DayPlan plan = DayPlan.builder()
                .slot(slot(BehaviorKey.HARVEST_SUGARCANE, 2_000, PlanSlotStatus.ACTIVE))
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(7L)
                .schedule(schedule(0))
                .status(PlanStatus.SUSPENDED)
                .build();

        // Act
        DayPlan decoded = DayPlanAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE,
                        DayPlanAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, DayPlanAttachmentState.of(plan)).resultOrPartial(Assertions::fail).orElseThrow())
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst()
                .plan()
                .orElseThrow();

        // Assert
        assertEquals(PlanStatus.PENDING, decoded.getStatus());
        assertEquals(PlanSlotStatus.PENDING, decoded.getSlots().getFirst().getStatus());
    }

    @Test
    void stateCodec_roundTripsEmptyState() {
        // Arrange, Act
        DayPlanAttachmentState decoded = DayPlanAttachmentCodec.STATE_CODEC.decode(JsonOps.INSTANCE,
                        DayPlanAttachmentCodec.STATE_CODEC.encodeStart(JsonOps.INSTANCE, DayPlanAttachmentState.empty()).resultOrPartial(Assertions::fail).orElseThrow())
                .resultOrPartial(Assertions::fail)
                .orElseThrow()
                .getFirst();

        // Assert
        assertEquals(Optional.empty(), decoded.plan());
    }

    private static PlanSlot slot(BehaviorKey key, int startTick, PlanSlotStatus status) {
        return PlanSlot.builder()
                .startTick(startTick)
                .behaviorKey(key)
                .priority(10)
                .flexible(true)
                .estimatedDurationTicks(600)
                .reason("test")
                .status(status)
                .build();
    }

    private static DayPlanSchedule schedule(int wakeTick) {
        return DayPlanSchedule.builder()
                .wakeTick(wakeTick)
                .bedtimeTick(12_000)
                .activityBlock(DayPlanActivityBlock.builder()
                        .context(DayPlanActivityContext.IDLE)
                        .startTick(wakeTick)
                        .endTick(12_000)
                        .reason("test")
                        .build())
                .build();
    }

    private static final class Assertions {

        private Assertions() {
        }

        private static void fail(String message) {
            org.junit.jupiter.api.Assertions.fail(message);
        }

    }

}
