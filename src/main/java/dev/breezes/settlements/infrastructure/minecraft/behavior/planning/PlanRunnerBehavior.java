package dev.breezes.settlements.infrastructure.minecraft.behavior.planning;

import dev.breezes.settlements.application.ai.override.OverrideArbiter;
import dev.breezes.settlements.application.ai.planning.PlanRunner;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Vanilla Brain behavior that keeps the Settlements plan runner and override arbiter alive inside
 * the brain tick loop.
 * <p>
 * This wrapper must not use vanilla's randomized behavior timeout.
 * It stays running and delegates interruption decisions to {@link PlanRunner} and
 * {@link OverrideArbiter}.
 */
@CustomLog
public class PlanRunnerBehavior extends Behavior<Villager> {

    private static final Set<Activity> DAY_PLAN_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    private final PlanRunner planRunner;
    private final OverrideArbiter overrideArbiter;

    @Inject
    public PlanRunnerBehavior(@Nonnull PlanRunner planRunner, @Nonnull OverrideArbiter overrideArbiter) {
        super(Map.of());
        this.planRunner = planRunner;
        this.overrideArbiter = overrideArbiter;
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        return villager instanceof BaseVillager;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected boolean canStillUse(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        // Returning false here would let vanilla stop the wrapper when activities switch. The runner
        // must stay alive so it can suspend/resume inner behavior explicitly across PANIC/RAID/etc.
        return true;
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        Brain<?> brain = villager.getBrain();

        // Override evaluation runs BEFORE the activity gate, so reactive accepts fire first
        if (this.overrideArbiter.tick(level, baseVillager)) {
            // Override lane occupied the villager this tick; do not also tick the plan.
            this.syncPlanActiveMemory(baseVillager);
            return;
        }

        // Danger needs no check of its own: it arrives as a reactive activity, and this gate suspends the day plan
        // outside the day-plan activities
        Optional<Activity> activeActivity = brain.getActiveNonCoreActivity();
        if (activeActivity.isEmpty() || !DAY_PLAN_ACTIVITIES.contains(activeActivity.get())) {
            if (this.planRunner.suspendIfActive(level, baseVillager)) {
                // Retried once the plan resumes, as after an override, so a brief panic does not cost the chore
                this.planRunner.reAttemptInterruptedSlot(baseVillager);
            }
            this.planRunner.ensureValidPlan(level, baseVillager);
            this.syncPlanActiveMemory(baseVillager);
            return;
        }

        this.planRunner.tick(level, baseVillager);
        this.syncPlanActiveMemory(baseVillager);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (villager instanceof BaseVillager baseVillager) {
            this.planRunner.forceStop(level, baseVillager);
            this.overrideArbiter.forceStop(level, baseVillager);
            this.syncPlanActiveMemory(baseVillager);
        }
    }

    /**
     * The single write path for PLAN_BEHAVIOR_ACTIVE memory that gates ambient behaviors.
     */
    private void syncPlanActiveMemory(@Nonnull BaseVillager villager) {
        Brain<?> brain = villager.getBrain();
        MemoryModuleType<Boolean> memory = MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType();
        boolean occupied = villager.hasActiveExecution();
        // Runs every tick for every adult, so write only on a change of state.
        if (occupied == brain.hasMemoryValue(memory)) {
            return;
        }

        if (occupied) {
            brain.setMemory(memory, true);
        } else {
            brain.eraseMemory(memory);
        }
    }

}
