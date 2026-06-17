package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
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
     * Transient override slot. Non-null only while a reactive override behavior
     * is running. Ticked by PlanRunner before the plan slot.
     * Structured as a single slot here; the field could become a Deque later
     * to support a small priority stack without changing callers.
     */
    @Nullable
    private IBehavior<BaseVillager> overrideBehavior;

    /**
     * The catalog key of {@link #overrideBehavior}, retained for diagnostics.
     */
    @Nullable
    private BehaviorKey overrideBehaviorKey;

    @Setter
    @Nullable
    private DayPlan pendingNextPlan;

    private boolean planExhausted;

    @Setter
    @Nullable
    private CompletableFuture<DayPlan> pendingFuture;

    private final ConcurrentLinkedQueue<DayPlan> pendingArrivals;

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
        this.overrideBehavior = null;
        this.overrideBehaviorKey = null;
        this.clearPendingGeneration();
        this.pendingNextPlan = null;
        this.planExhausted = false;
        this.previousPlanTickDayTime = -1L;
    }

    public void reset(long dayTime) {
        this.reset();
        this.previousPlanTickDayTime = dayTime;
    }

    public void clearCurrentBehavior() {
        this.currentBehavior = null;
        this.currentDescriptor = null;
    }

    public void assignPlanBehavior(@Nonnull IBehavior<BaseVillager> behavior,
                                   @Nullable BehaviorPlanningMetadata descriptor) {
        this.currentBehavior = behavior;
        this.currentDescriptor = descriptor;
    }

    public boolean isOverrideActive() {
        return this.overrideBehavior != null;
    }

    public void installOverride(@Nonnull IBehavior<BaseVillager> behavior, @Nonnull BehaviorKey key) {
        this.overrideBehavior = behavior;
        this.overrideBehaviorKey = key;
    }

    public void clearOverride() {
        this.overrideBehavior = null;
        this.overrideBehaviorKey = null;
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
