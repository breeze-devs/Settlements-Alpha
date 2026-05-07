package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;
import lombok.Getter;

/**
 * A single scheduled activity within a {@link DayPlan}.
 * <p>
 * {@code startTick} is in game time (0–24 000 ticks), where tick 0 = 06:00 AM in
 * Minecraft's clock. The plan runner uses this to skip stale slots when significant
 * time has elapsed past a slot's intended start.
 * <p>
 * A {@code flexible} slot is bypassed if its behavior's preconditions fail.
 * A rigid slot (e.g. eating, sleeping) is retried until its preconditions
 * pass and is never automatically skipped.
 */
@Getter
public class PlanSlot {

    private final int startTick;
    private final BehaviorKey behaviorKey;
    private final int priority;
    private final boolean flexible;
    private final int estimatedDurationTicks;
    private final String reason;

    private PlanSlotStatus status;

    @Builder
    public PlanSlot(int startTick,
                    BehaviorKey behaviorKey,
                    int priority,
                    boolean flexible,
                    int estimatedDurationTicks,
                    String reason,
                    PlanSlotStatus status) {
        if (!TimeOfDay.isValidTick(startTick)) {
            throw new IllegalArgumentException("startTick must be between 0 and 23999");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must be non-negative");
        }
        this.startTick = startTick;
        this.behaviorKey = behaviorKey;
        this.priority = priority;
        this.flexible = flexible;
        this.estimatedDurationTicks = estimatedDurationTicks;
        this.reason = reason;
        this.status = status == null ? PlanSlotStatus.PENDING : status;
    }

    public void markStatus(PlanSlotStatus status) {
        this.status = status;
    }

}
