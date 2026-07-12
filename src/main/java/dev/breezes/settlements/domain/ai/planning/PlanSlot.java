package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;
import lombok.Getter;

/**
 * A single scheduled activity within a {@link DayPlan}.
 * <p>
 * {@code startTick} is a civil-space tick (0–24 000 ticks). The plan runner uses this to
 * skip stale slots when significant time has elapsed past a slot's intended start.
 * <p>
 * A {@code flexible} slot is bypassed if its behavior's preconditions fail.
 * A rigid slot (e.g. eating, sleeping) is retried until its preconditions
 * pass and is never automatically skipped.
 * <p>
 * A {@code pinned} slot came from a fixed-time placement (an LLM {@code at}/commitment pin, not a
 * default meal or ordinary gap-fill) and is eligible to be carried into the successor plan when a
 * hard reset regenerates the day — see {@code PlanRunner#collectCarriedPins}.
 */
@Getter
public class PlanSlot {

    private final int startTick;
    private final BehaviorKey behaviorKey;
    private final int priority;
    private final boolean flexible;
    private final int estimatedDurationTicks;
    private final boolean pinned;

    private PlanSlotStatus status;

    @Builder
    public PlanSlot(int startTick,
                    BehaviorKey behaviorKey,
                    int priority,
                    boolean flexible,
                    int estimatedDurationTicks,
                    boolean pinned,
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
        this.pinned = pinned;
        this.status = status == null ? PlanSlotStatus.PENDING : status;
    }

    public void markStatus(PlanSlotStatus status) {
        this.status = status;
    }

}
