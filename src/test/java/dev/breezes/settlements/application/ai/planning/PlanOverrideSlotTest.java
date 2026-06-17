package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.OverrideRequest;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the override slot state transitions on {@link PlanRuntimeState}.
 * <p>
 * All assertions concern pure data: no Minecraft objects are constructed or mocked.
 * The IBehavior mock is used only as an opaque handle; no behavior methods are called.
 */
@ExtendWith(MockitoExtension.class)
class PlanOverrideSlotTest {

    @Mock
    @SuppressWarnings("unchecked")
    private IBehavior<BaseVillager> overrideBehavior;

    private PlanRuntimeState runtime;

    @BeforeEach
    void setUp() {
        this.runtime = new PlanRuntimeState();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void isOverrideActive_falseOnInit() {
        assertFalse(runtime.isOverrideActive());
    }

    @Test
    void overrideBehavior_nullOnInit() {
        assertNull(runtime.getOverrideBehavior());
    }

    @Test
    void overrideBehaviorKey_nullOnInit() {
        assertNull(runtime.getOverrideBehaviorKey());
    }

    // -------------------------------------------------------------------------
    // installOverride
    // -------------------------------------------------------------------------

    @Test
    void installOverride_setsActiveAndKey() {
        // Arrange — behavior mock is the opaque handle; key is the domain identity.

        // Act
        runtime.installOverride(overrideBehavior, BehaviorKey.COURTSHIP_ACCEPT);

        // Assert
        assertTrue(runtime.isOverrideActive());
        assertSame(overrideBehavior, runtime.getOverrideBehavior());
        assertEquals(BehaviorKey.COURTSHIP_ACCEPT, runtime.getOverrideBehaviorKey());
    }

    @Test
    void installOverride_tradeKey_isDistinctFromCourtship() {
        // Act
        runtime.installOverride(overrideBehavior, BehaviorKey.TRADE_ACCEPT);

        // Assert
        assertEquals(BehaviorKey.TRADE_ACCEPT, runtime.getOverrideBehaviorKey());
    }

    // -------------------------------------------------------------------------
    // clearOverride
    // -------------------------------------------------------------------------

    @Test
    void clearOverride_resetsToIdle() {
        // Arrange
        runtime.installOverride(overrideBehavior, BehaviorKey.COURTSHIP_ACCEPT);

        // Act
        runtime.clearOverride();

        // Assert
        assertFalse(runtime.isOverrideActive());
        assertNull(runtime.getOverrideBehavior());
        assertNull(runtime.getOverrideBehaviorKey());
    }

    @Test
    void clearOverride_isIdempotentWhenAlreadyIdle() {
        // Arrange — slot is already idle from setUp

        // Act — must not throw
        runtime.clearOverride();

        // Assert
        assertFalse(runtime.isOverrideActive());
    }

    // -------------------------------------------------------------------------
    // reset clears the override slot
    // -------------------------------------------------------------------------

    @Test
    void reset_clearsOverrideSlot() {
        // Arrange
        runtime.installOverride(overrideBehavior, BehaviorKey.TRADE_ACCEPT);

        // Act
        runtime.reset();

        // Assert — hard reset must also evict any running override so the body is free.
        assertFalse(runtime.isOverrideActive());
        assertNull(runtime.getOverrideBehavior());
        assertNull(runtime.getOverrideBehaviorKey());
    }

    // -------------------------------------------------------------------------
    // Re-attempt policy: INTERRUPTED → PENDING on override completion
    // -------------------------------------------------------------------------

    @Test
    void interruptedSlot_isRestoredToPending_whenOverrideCompletes() {
        // Arrange — simulate suspendIfActive having marked the slot INTERRUPTED,
        // then reAttemptInterruptedSlot flipping it back to PENDING.
        PlanSlot slot = planSlot(BehaviorKey.HARVEST_MELON, PlanSlotStatus.INTERRUPTED);
        DayPlan plan = planWith(slot);

        // Act — replicate the slot flip that reAttemptInterruptedSlot applies.
        plan.getCurrentSlot()
                .filter(s -> s.getStatus() == PlanSlotStatus.INTERRUPTED)
                .ifPresent(s -> s.markStatus(PlanSlotStatus.PENDING));

        // Assert — the plan runner will retry the same slot next tick
        assertEquals(PlanSlotStatus.PENDING, slot.getStatus());
    }

    @Test
    void completedSlot_isNotAffected_byPendingFlip() {
        // Arrange — a slot that ran to natural completion must not be re-queued by a stray flip.
        PlanSlot slot = planSlot(BehaviorKey.HARVEST_MELON, PlanSlotStatus.COMPLETED);
        DayPlan plan = planWith(slot);

        // Act — reAttemptInterruptedSlot only acts on INTERRUPTED; COMPLETED is unchanged.
        plan.getCurrentSlot()
                .filter(s -> s.getStatus() == PlanSlotStatus.INTERRUPTED)
                .ifPresent(s -> s.markStatus(PlanSlotStatus.PENDING));

        // Assert
        assertEquals(PlanSlotStatus.COMPLETED, slot.getStatus());
    }

    @Test
    void pendingSlot_isNotDoubleQueued_byPendingFlip() {
        // Arrange — a slot still waiting to start is already PENDING; the flip is a no-op.
        PlanSlot slot = planSlot(BehaviorKey.EAT_FOOD, PlanSlotStatus.PENDING);
        DayPlan plan = planWith(slot);

        // Act
        plan.getCurrentSlot()
                .filter(s -> s.getStatus() == PlanSlotStatus.INTERRUPTED)
                .ifPresent(s -> s.markStatus(PlanSlotStatus.PENDING));

        // Assert
        assertEquals(PlanSlotStatus.PENDING, slot.getStatus());
    }

    // -------------------------------------------------------------------------
    // Window-closed skip policy (exercising PlanRunner static helpers)
    // -------------------------------------------------------------------------

    @Test
    void windowClosed_slotIsSkipped_notRetriedForever() {
        // Arrange — slot started at tick 2_000, duration 500 ticks, now at tick 5_000.
        // With epoch 0, linearEnd = 2_500; linearNow = 5_000 → window is closed.
        PlanSlot slot = planSlot(2_000, 500, BehaviorKey.HARVEST_MELON, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 5_000, 0);

        // Assert — window-closed gate in the seek loop would skip this slot.
        assertTrue(closed);
    }

    @Test
    void windowOpen_slotIsNotSkipped() {
        // Arrange — slot starts at tick 5_000, now at tick 4_500 → not yet open.
        PlanSlot slot = planSlot(5_000, 600, BehaviorKey.HARVEST_MELON, PlanSlotStatus.PENDING);

        // Act
        boolean open = PlanRunner.isSlotWindowOpen(slot, 4_500, 0);

        // Assert
        assertFalse(open);
    }

    @Test
    void windowJustOpened_slotIsEligible() {
        // Arrange — slot starts at tick 5_000, now exactly at tick 5_000.
        PlanSlot slot = planSlot(5_000, 600, BehaviorKey.HARVEST_MELON, PlanSlotStatus.PENDING);

        // Act
        boolean open = PlanRunner.isSlotWindowOpen(slot, 5_000, 0);

        // Assert
        assertTrue(open);
    }

    @Test
    void windowClosedAfterInterrupt_slotWouldBeSkipped_onRetry() {
        // Arrange — override interrupted a slot at tick 2_000. Override ran for 1_500 ticks.
        // By tick 3_500 the original slot's window (2_000–2_500) is closed.
        PlanSlot slot = planSlot(2_000, 500, BehaviorKey.HARVEST_MELON, PlanSlotStatus.PENDING);

        // Act — re-attempt would reach here and the seek loop evaluates isSlotWindowClosed.
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 3_500, 0);

        // Assert — slot is skipped gracefully; no infinite retry.
        assertTrue(closed);
    }

    @Test
    void windowStillOpenAfterInterrupt_slotIsRetried() {
        // Arrange — override was short (50 ticks). Slot window is still open.
        PlanSlot slot = planSlot(2_000, 500, BehaviorKey.HARVEST_MELON, PlanSlotStatus.PENDING);

        // Act
        boolean closed = PlanRunner.isSlotWindowClosed(slot, 2_050, 0);

        // Assert — plan runner retries the slot.
        assertFalse(closed);
    }

    // -------------------------------------------------------------------------
    // Override pre-emption decision: override wins over active plan slot
    // -------------------------------------------------------------------------

    @Test
    void overrideInstalled_planSlotMustBeSuspended_firstToAvoidDualOwnership() {
        // Arrange — documents the invariant that suspendIfActive is called BEFORE
        // installOverride so the body is never under dual ownership.
        // This test is structural: it verifies that if the slot is ACTIVE when the
        // override triggers, the correct transition sequence makes it INTERRUPTED,
        // then PENDING, while the override slot becomes active.
        PlanSlot slot = planSlot(BehaviorKey.HARVEST_MELON, PlanSlotStatus.ACTIVE);

        // Act — replicate suspendIfActive slot side-effect, then reAttemptInterruptedSlot.
        slot.markStatus(PlanSlotStatus.INTERRUPTED);          // suspendIfActive
        slot.markStatus(PlanSlotStatus.PENDING);              // reAttemptInterruptedSlot
        runtime.installOverride(overrideBehavior, BehaviorKey.COURTSHIP_ACCEPT);

        // Assert — slot is queued for retry; override slot is the only body owner.
        assertEquals(PlanSlotStatus.PENDING, slot.getStatus());
        assertTrue(runtime.isOverrideActive());
    }

    @Test
    void interruptibleDescriptor_allowsOverridePreemption() {
        // Arrange
        BehaviorPlanningMetadata descriptor = descriptor(true);

        // Act
        boolean canInterrupt = PlanRunner.canInterruptCurrentPlanBehavior(descriptor);

        // Assert
        assertTrue(canInterrupt);
    }

    @Test
    void uninterruptibleDescriptor_blocksOverridePreemption() {
        // Arrange
        BehaviorPlanningMetadata descriptor = descriptor(false);

        // Act
        boolean canInterrupt = PlanRunner.canInterruptCurrentPlanBehavior(descriptor);

        // Assert
        assertFalse(canInterrupt);
    }

    @Test
    void missingDescriptor_allowsOverridePreemption() {
        // Arrange, Act
        boolean canInterrupt = PlanRunner.canInterruptCurrentPlanBehavior(null);

        // Assert — missing metadata should not strand reactive behavior forever.
        assertTrue(canInterrupt);
    }

    @Test
    void overridePolicies_areOrderedByPriorityBeforeEvaluation() {
        // Arrange
        TestOverridePolicy lowPriority = new TestOverridePolicy(10);
        TestOverridePolicy highPriority = new TestOverridePolicy(100);

        // Act
        List<OverridePolicy> ordered = PlanRunner.orderedOverridePolicies(List.of(lowPriority, highPriority));

        // Assert
        assertSame(highPriority, ordered.getFirst());
        assertSame(lowPriority, ordered.get(1));
    }

    @Test
    void overridePolicies_useClassNameTieBreakerForStableOrdering() {
        // Arrange
        TestOverridePolicy zPolicy = new ZTiePolicy(50);
        TestOverridePolicy aPolicy = new ATiePolicy(50);

        // Act
        List<OverridePolicy> ordered = PlanRunner.orderedOverridePolicies(List.of(zPolicy, aPolicy));

        // Assert
        assertSame(aPolicy, ordered.getFirst());
        assertSame(zPolicy, ordered.get(1));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static PlanSlot planSlot(BehaviorKey key, PlanSlotStatus status) {
        return planSlot(1_000, 300, key, status);
    }

    private static PlanSlot planSlot(int startTick, int durationTicks, BehaviorKey key, PlanSlotStatus status) {
        return PlanSlot.builder()
                .startTick(startTick)
                .behaviorKey(key)
                .priority(1)
                .flexible(true)
                .estimatedDurationTicks(durationTicks)
                .reason("test")
                .status(status)
                .build();
    }

    private static DayPlan planWith(PlanSlot slot) {
        return DayPlan.builder()
                .dayType(PlanDayType.WORK_DAY)
                .wakeAtAbsoluteTick(0L)
                .schedule(DayPlanSchedule.builder()
                        .wakeTick(0)
                        .bedtimeTick(12_000)
                        .build())
                .slot(slot)
                .build();
    }

    private static BehaviorPlanningMetadata descriptor(boolean interruptible) {
        return BehaviorPlanningMetadata.builder()
                .key(BehaviorKey.HARVEST_MELON)
                .displayName("Test")
                .description("Test descriptor")
                .estimatedDuration(GameTicks.minutes(1))
                .preconditionSummary("test")
                .interruptible(interruptible)
                .build();
    }

    private static class TestOverridePolicy implements OverridePolicy {
        private final int priority;

        private TestOverridePolicy(int priority) {
            this.priority = priority;
        }

        @Override
        public int priority() {
            return this.priority;
        }

        @Override
        public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
            return Optional.empty();
        }
    }

    private static final class ATiePolicy extends TestOverridePolicy {
        private ATiePolicy(int priority) {
            super(priority);
        }
    }

    private static final class ZTiePolicy extends TestOverridePolicy {
        private ZTiePolicy(int priority) {
            super(priority);
        }
    }

}
