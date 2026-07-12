package dev.breezes.settlements.domain.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.world.WorldCalendar;
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
 * schedule for a single authored day.
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

    /**
     * The midnight-aligned calendar day this plan was authored for — the plan's identity.
     * <p>
     * All slot and schedule ticks are civil-space offsets INTO this one day, so no wrap arithmetic
     * is needed anywhere in this class; see {@link #getWakeAtAbsoluteTick()} for the absolute-tick view
     * PlanRunner's long-space spine still needs.
     */
    private final long calendarDay;

    private final DayPlanSchedule schedule;

    private PlanStatus status;

    private int currentSlotIndex;

    @Builder
    public DayPlan(@Nonnull @Singular List<PlanSlot> slots,
                   PlanDayType dayType,
                   long calendarDay,
                   @Nonnull DayPlanSchedule schedule,
                   @Nullable PlanStatus status,
                   int currentSlotIndex) {
        this.schedule = schedule;
        validateSchedule(schedule);
        validateSlotWindows(slots);
        this.slots = slots.stream()
                .sorted(Comparator.comparingInt(PlanSlot::getStartTick))
                .toList();
        this.dayType = dayType;
        this.calendarDay = calendarDay;
        this.status = status == null ? PlanStatus.PENDING : status;
        this.currentSlotIndex = currentSlotIndex;
    }

    /**
     * Derives the absolute Minecraft {@code dayTime} this plan's wake tick occurs at, from the
     * stored {@link #calendarDay} and the schedule's civil wake tick.
     */
    public long getWakeAtAbsoluteTick() {
        return WorldCalendar.absoluteTickForCivil(this.calendarDay, this.schedule.wakeTick());
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

    private static void validateSlotWindows(List<PlanSlot> slots) {
        for (PlanSlot slot : slots) {
            if (slot.getStartTick() + slot.getEstimatedDurationTicks() > TICKS_PER_DAY) {
                throw new IllegalArgumentException("slot must end within its civil day");
            }
        }
    }

    private static void validateSchedule(DayPlanSchedule schedule) {
        List<DayPlanActivityBlock> sortedBlocks = schedule.activityBlocks().stream()
                .sorted(Comparator.comparingInt(DayPlanActivityBlock::startTick))
                .toList();

        int previousEnd = Integer.MIN_VALUE;
        for (DayPlanActivityBlock block : sortedBlocks) {
            if (block.endTick() <= block.startTick()) {
                throw new IllegalArgumentException("activity block endTick must be after startTick");
            }
            if (previousEnd != Integer.MIN_VALUE && block.startTick() < previousEnd) {
                throw new IllegalArgumentException("activity blocks must not overlap");
            }
            previousEnd = block.endTick();
        }
    }

}
