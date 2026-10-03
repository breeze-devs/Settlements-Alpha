package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PlanArrival;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class PlanRuntimeState {

    @Nullable
    private IBehavior<BaseVillager> currentBehavior;

    @Nullable
    private BehaviorPlanningMetadata currentDescriptor;

    /**
     * Real elapsed ticks the current plan-slot behavior has been running. PlanRunner uses this as
     * a safety-net ceiling to break a wedged plan behavior that would otherwise freeze the
     * villager's entire remaining day. Reset to zero whenever a behavior is assigned, cleared,
     * or the runtime is reset.
     */
    private int currentBehaviorElapsedTicks;

    /**
     * Ticks remaining before the current rigid plan slot may next attempt to start.
     */
    private int slotStartRetryDelayTicks;

    /**
     * The staged next {@link PlanArrival} awaiting its wake gate, retained whole (plan + author +
     * target day) so author-priority arbitration in {@code PlanRunner#drainPendingArrivals} can
     * compare a newly polled arrival against the one already staged.
     */
    @Nullable
    private PlanArrival pendingArrival;

    private boolean planExhausted;

    @Setter
    @Nullable
    private CompletableFuture<DayPlan> pendingFuture;

    private final ConcurrentLinkedQueue<PlanArrival> pendingArrivals;

    @Setter
    private long pendingFutureSubmittedAtDayTime;

    @Setter
    private long pendingFutureWakeAtAbsoluteTick;

    private long previousPlanTickDayTime;

    public PlanRuntimeState() {
        this.pendingArrivals = new ConcurrentLinkedQueue<>();
        this.reset();
    }

    public void reset() {
        this.currentBehavior = null;
        this.currentDescriptor = null;
        this.currentBehaviorElapsedTicks = 0;
        this.slotStartRetryDelayTicks = 0;
        this.clearPendingGeneration();
        this.pendingArrival = null;
        this.planExhausted = false;
        this.previousPlanTickDayTime = -1L;
    }

    public void reset(long dayTime) {
        this.reset();
        this.previousPlanTickDayTime = dayTime;
    }

    public void setPendingArrival(@Nullable PlanArrival pendingArrival) {
        this.pendingArrival = pendingArrival;
    }

    /**
     * The plan of the staged {@link #pendingArrival}, or null when nothing is staged. Derived so
     * readers that only need the plan (wake gate, adoption, fallback guards) are unaffected by the
     * arbitration layer now staging the full arrival rather than a bare {@link DayPlan}.
     */
    @Nullable
    public DayPlan getPendingNextPlan() {
        return this.pendingArrival == null ? null : this.pendingArrival.plan();
    }

    public void clearCurrentBehavior() {
        this.currentBehavior = null;
        this.currentDescriptor = null;
        this.currentBehaviorElapsedTicks = 0;
    }

    public void assignPlanBehavior(@Nonnull IBehavior<BaseVillager> behavior,
                                   @Nullable BehaviorPlanningMetadata descriptor) {
        this.currentBehavior = behavior;
        this.currentDescriptor = descriptor;
        // A freshly assigned behavior must always start its elapsed counter from zero,
        // regardless of whether clearCurrentBehavior was called before this assignment.
        this.currentBehaviorElapsedTicks = 0;
    }

    public boolean isBehaviorActive() {
        return this.currentBehavior != null;
    }

    public void incrementCurrentBehaviorElapsedTicks(int delta) {
        this.currentBehaviorElapsedTicks += delta;
    }

    public void armSlotStartRetryDelay(int ticks) {
        this.slotStartRetryDelayTicks = ticks;
    }

    public void decaySlotStartRetryDelay(int delta) {
        this.slotStartRetryDelayTicks = Math.max(0, this.slotStartRetryDelayTicks - delta);
    }

    public void markPlanExhausted() {
        this.planExhausted = true;
    }

    public void clearPendingFuture() {
        this.pendingFuture = null;
        this.pendingFutureSubmittedAtDayTime = -1L;
        this.pendingFutureWakeAtAbsoluteTick = -1L;
    }

    public void cancelPendingFuture() {
        if (this.pendingFuture != null) {
            this.pendingFuture.cancel(true);
        }
        this.clearPendingFuture();
    }

    public void clearPendingGeneration() {
        this.cancelPendingFuture();
        this.pendingArrivals.clear();
    }

    public DeltaResult advanceClock(long dayTime) {
        long previousPlanTick = this.previousPlanTickDayTime;
        if (previousPlanTick < 0L) {
            this.previousPlanTickDayTime = dayTime;
            return DeltaResult.builder()
                    .deltaTicks(1)
                    .backwardJump(false)
                    .firstTick(true)
                    .rawDelta(0L)
                    .previousDayTime(previousPlanTick)
                    .build();
        }

        long rawDelta = dayTime - previousPlanTick;
        this.previousPlanTickDayTime = dayTime;
        int deltaTicks = rawDelta <= 0L ? 0 : (int) Math.min(Integer.MAX_VALUE, rawDelta);
        return DeltaResult.builder()
                .deltaTicks(deltaTicks)
                .backwardJump(rawDelta < 0L)
                .firstTick(false)
                .rawDelta(rawDelta)
                .previousDayTime(previousPlanTick)
                .build();
    }

}
