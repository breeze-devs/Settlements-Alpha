package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;

/**
 * An ordered sequence of {@link PlanSlot}s defining a villager's intended activity
 * schedule for a single game day.
 * <p>
 * Slots are executed in index order by the plan runner. The plan tracks its own
 * {@link PlanStatus} and current slot index as mutable state.
 * All slots are fixed at construction time.
 * <p>
 * When all slots have been advanced past, the plan is exhausted.
 */
@Getter
public class DayPlan {

    private final List<PlanSlot> slots;

    private final PlanDayType dayType;
    private final long generatedForDay;
    private final DayPlanSchedule schedule;

    /**
     * The real game tick at which this plan's chronological day begins (typically the wake tick).
     * Used to sort slots correctly when the plan spans the Minecraft day boundary — e.g. a farmer
     * waking at 5am (tick 23 000) has slots before tick 0 (6am) that must sort first.
     * Defaults to 0 (6am) for standard-hours villagers where no wrapping occurs.
     */
    private final int dayStartTick;

    private PlanStatus status;

    private int currentSlotIndex;

    @Builder
    public DayPlan(@Nonnull @Singular List<PlanSlot> slots,
                   PlanDayType dayType,
                   long generatedForDay,
                   @Nonnull DayPlanSchedule schedule,
                   @Nullable PlanStatus status,
                   int currentSlotIndex,
                   int dayStartTick) {
        this.dayStartTick = dayStartTick;
        this.schedule = schedule;
        validateSchedule(schedule);
        validateSlotWindows(slots, dayStartTick);
        this.slots = slots.stream()
                .sorted(Comparator.comparingInt(s -> Math.floorMod(s.getStartTick() - dayStartTick, TICKS_PER_DAY)))
                .toList();
        this.dayType = dayType;
        this.generatedForDay = generatedForDay;
        this.status = status == null ? PlanStatus.PENDING : status;
        this.currentSlotIndex = currentSlotIndex;
    }

    public Optional<PlanSlot> getCurrentSlot() {
        if (this.currentSlotIndex < 0 || this.currentSlotIndex >= this.slots.size()) {
            return Optional.empty();
        }
        return Optional.of(this.slots.get(this.currentSlotIndex));
    }

    /**
     * Advances the current slot cursor by one. Does not perform a bounds check — call
     * {@link #isExhausted()} after advancing to detect plan exhaustion.
     */
    public void advanceSlot() {
        this.currentSlotIndex++;
    }

    public boolean isExhausted() {
        return this.currentSlotIndex >= this.slots.size();
    }

    /**
     * Returns a snapshot of slots from the current index to the end of the plan.
     */
    public List<PlanSlot> getRemainingSlots() {
        if (this.currentSlotIndex >= this.slots.size()) {
            return List.of();
        }
        return List.copyOf(this.slots.subList(Math.max(0, this.currentSlotIndex), this.slots.size()));
    }

    public void markStatus(PlanStatus status) {
        this.status = status;
    }

    private static void validateSlotWindows(List<PlanSlot> slots, int dayStartTick) {
        for (PlanSlot slot : slots) {
            int linearStart = Math.floorMod(slot.getStartTick() - dayStartTick, TICKS_PER_DAY);
            if (linearStart + slot.getEstimatedDurationTicks() >= TICKS_PER_DAY) {
                throw new IllegalArgumentException("slot window must not cross the plan day boundary");
            }
        }
    }

    private static void validateSchedule(DayPlanSchedule schedule) {
        int bedtimeLinear = Math.floorMod(schedule.bedtimeTick() - schedule.wakeTick(), TICKS_PER_DAY);
        if (bedtimeLinear == 0) {
            throw new IllegalArgumentException("bedtimeTick must be after wakeTick in authored-day space");
        }

        List<DayPlanActivityBlock> sortedBlocks = schedule.activityBlocks().stream()
                .sorted(Comparator.comparingInt(block -> Math.floorMod(block.startTick() - schedule.wakeTick(), TICKS_PER_DAY)))
                .toList();

        int previousEndLinear = 0;
        for (DayPlanActivityBlock block : sortedBlocks) {
            int startLinear = Math.floorMod(block.startTick() - schedule.wakeTick(), TICKS_PER_DAY);
            int endLinear = Math.floorMod(block.endTick() - schedule.wakeTick(), TICKS_PER_DAY);

            if (endLinear <= startLinear) {
                throw new IllegalArgumentException("activity block endTick must be after startTick in authored-day space");
            }
            if (startLinear < previousEndLinear) {
                throw new IllegalArgumentException("activity blocks must not overlap");
            }
            previousEndLinear = endLinear;
        }
    }

}
