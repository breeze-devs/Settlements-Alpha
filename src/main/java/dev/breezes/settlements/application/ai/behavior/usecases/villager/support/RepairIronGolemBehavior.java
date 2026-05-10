package dev.breezes.settlements.application.ai.behavior.usecases.villager.support;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyDamagedIronGolemExistsCondition;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class RepairIronGolemBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private enum RepairStage implements StageKey {
        REPAIR_GOLEM,
        END;
    }

    private final RepairIronGolemConfig config;
    private final NearbyDamagedIronGolemExistsCondition<BaseVillager> nearbyDamagedIronGolemExistsCondition;

    @Nullable
    private IronGolem targetToRepair;
    private int remainingRepairAttempts;

    public RepairIronGolemBehavior(RepairIronGolemConfig config,
                                   HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.config = config;

        // Create behavior preconditions
        this.nearbyDamagedIronGolemExistsCondition = new NearbyDamagedIronGolemExistsCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(), config.repairHpPercentage());
        this.preconditions.add(this.nearbyDamagedIronGolemExistsCondition);

        // Initialize variables
        this.targetToRepair = null;
        this.remainingRepairAttempts = 0;

        this.initializeStateMachine(this.createControlStep(), RepairStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("RepairIronGolemBehavior")
                .initialStage(RepairStage.REPAIR_GOLEM)
                .stageStepMap(Map.of(RepairStage.REPAIR_GOLEM, this.createRepairStep()))
                .nextStage(RepairStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createRepairStep() {
        TimeBasedStep<BaseVillager> repairTick = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(2).asTickable())
                .everyTick(ctx -> {
                    ctx.getInitiator().setHeldItem(new ItemStack(Items.IRON_INGOT));
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.targetToRepair == null || !this.targetToRepair.isAlive()) {
                        return StepResult.complete();
                    }

                    double healAmount = RandomUtil.randomDouble(3, 8);
                    this.targetToRepair.heal((float) healAmount);

                    Location targetLocation = Location.fromEntity(this.targetToRepair, false);
                    SoundRegistry.REPAIR_IRON_GOLEM.playGlobally(targetLocation, SoundSource.NEUTRAL);
                    ParticleRegistry.repairIronGolem(targetLocation);
                    log.behaviorTrace("Repaired iron golem for {} HP, {} attempts remaining", healAmount, this.remainingRepairAttempts - 1);

                    if (--this.remainingRepairAttempts <= 0) {
                        return StepResult.complete();
                    }
                    return StepResult.transition(RepairStage.REPAIR_GOLEM);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(0.5f, 1))
                .actionStep(repairTick)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        List<IronGolem> targets = this.nearbyDamagedIronGolemExistsCondition.getTargets();
        if (targets.isEmpty()) {
            this.requestStop("No damaged iron golem found within range");
            return;
        }

        this.targetToRepair = targets.getFirst();
        this.remainingRepairAttempts = RandomUtil.randomInt(1, 3, true); // TODO: this could be based on inventory, e.g. iron ingot count
        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(this.targetToRepair))));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.targetToRepair != null
                && this.targetToRepair.isAlive()
                && this.targetToRepair.getHealth() < this.targetToRepair.getMaxHealth() * this.config.repairHpPercentage();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.targetToRepair = null;
        this.remainingRepairAttempts = 0;
    }

}
