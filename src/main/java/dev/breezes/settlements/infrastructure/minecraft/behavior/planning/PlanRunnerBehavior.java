package dev.breezes.settlements.infrastructure.minecraft.behavior.planning;

import dev.breezes.settlements.application.ai.planning.PlanRunner;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Vanilla Brain behavior that keeps the Settlements plan runner alive inside
 * the brain tick loop.
 * <p>
 * This wrapper must not use vanilla's randomized behavior timeout.
 * It stays running and delegates interruption decisions to PlanRunner.
 */
@CustomLog
public class PlanRunnerBehavior extends Behavior<Villager> {

    private static final Set<Activity> MANAGED_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    private final PlanRunner planRunner;

    @Inject
    public PlanRunnerBehavior(@Nonnull PlanRunner planRunner) {
        super(Map.of());
        this.planRunner = planRunner;
    }

    @Override
    protected boolean checkExtraStartConditions(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        boolean isBaseVillager = villager instanceof BaseVillager;
        boolean isSafe = !VillagerPanicTrigger.isHurt(villager) && !VillagerPanicTrigger.hasHostile(villager);
        if (!isBaseVillager || !isSafe) {
            log.behaviorWarn("PlanRunnerBehavior start blocked: baseVillager={}, safe={}", isBaseVillager, isSafe);
        }
        return isBaseVillager && isSafe;
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
    protected void start(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        log.behaviorStatus("PlanRunnerBehavior STARTED for {}", villager.getUUID());
    }

    @Override
    protected void tick(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        Brain<?> brain = villager.getBrain();
        boolean isSafe = !VillagerPanicTrigger.isHurt(villager) && !VillagerPanicTrigger.hasHostile(villager);
        Optional<Activity> activeActivity = brain.getActiveNonCoreActivity();
        if (!isSafe) {
            this.planRunner.suspendIfActive(level, baseVillager);
            return;
        }

        if (activeActivity.isEmpty() || !MANAGED_ACTIVITIES.contains(activeActivity.get())) {
            this.planRunner.suspendIfActive(level, baseVillager);
            this.planRunner.ensurePlanForCurrentDay(level, baseVillager);
            return;
        }

        this.planRunner.tick(level, baseVillager);
    }

    @Override
    protected void stop(@Nonnull ServerLevel level, @Nonnull Villager villager, long gameTime) {
        log.behaviorStatus("PlanRunnerBehavior STOPPED for {}", villager.getUUID());
        if (villager instanceof BaseVillager baseVillager) {
            this.planRunner.forceStop(level, baseVillager);
        }
    }

}
