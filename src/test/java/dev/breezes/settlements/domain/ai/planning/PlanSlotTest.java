package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanSlotTest {

    @Test
    void builder_populatesFields() {
        PlanSlot slot = PlanSlot.builder()
                .startTick(6_000)
                .behaviorKey(BehaviorKey.EAT_FOOD)
                .priority(10)
                .flexible(false)
                .estimatedDurationTicks(1_200)
                .reason("scheduled lunch")
                .build();

        assertEquals(6_000, slot.getStartTick());
        assertEquals(BehaviorKey.EAT_FOOD, slot.getBehaviorKey());
        assertEquals(10, slot.getPriority());
        assertFalse(slot.isFlexible());
        assertEquals(1_200, slot.getEstimatedDurationTicks());
        assertEquals("scheduled lunch", slot.getReason());
        assertEquals(PlanSlotStatus.PENDING, slot.getStatus());
    }

    @Test
    void markStatus_updatesStatus() {
        PlanSlot slot = PlanSlot.builder()
                .startTick(1_000)
                .behaviorKey(BehaviorKey.TRADE_INITIATE)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(600)
                .reason("morning walk")
                .build();

        slot.markStatus(PlanSlotStatus.ACTIVE);

        assertEquals(PlanSlotStatus.ACTIVE, slot.getStatus());
    }

    @Test
    void builder_rejectsNegativeStartTick() {
        assertThrows(IllegalArgumentException.class, () -> PlanSlot.builder()
                .startTick(-1)
                .behaviorKey(BehaviorKey.TRADE_INITIATE)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(600)
                .reason("invalid")
                .build());
    }

    @Test
    void builder_rejectsWrappedDayTick() {
        assertThrows(IllegalArgumentException.class, () -> PlanSlot.builder()
                .startTick(24_000)
                .behaviorKey(BehaviorKey.TRADE_INITIATE)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(600)
                .reason("invalid")
                .build());
    }

    @Test
    void builder_rejectsNegativePriority() {
        assertThrows(IllegalArgumentException.class, () -> PlanSlot.builder()
                .startTick(1_000)
                .behaviorKey(BehaviorKey.TRADE_INITIATE)
                .priority(-1)
                .flexible(true)
                .estimatedDurationTicks(600)
                .reason("invalid")
                .build());
    }

}
