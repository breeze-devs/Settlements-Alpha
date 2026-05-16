package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing;

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
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import dev.breezes.settlements.infrastructure.minecraft.mixins.WolfMixin;
import lombok.CustomLog;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Map;

@CustomLog
public class WashWolfBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.5D;
    private static final float NAVIGATION_SPEED = 0.55F;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 2;
    private static final ClockTicks WASH_DURATION = ClockTicks.seconds(5);
    private static final ClockTicks DRY_DURATION = ClockTicks.seconds(1);
    private static final int WASH_EFFECT_INTERVAL_TICKS = 5;
    private static final int INTERACT_RETRIGGER_INTERVAL_TICKS = 20;

    private enum WashStage implements StageKey {
        WASH_WOLF,
        DRY_WOLF,
        END;
    }

    private final WashWolfConfig config;
    private final OwnedDirtyWolfExistsCondition ownedDirtyWolfExistsCondition;

    private SettlementsWolf targetWolf;

    public WashWolfBehavior(@Nonnull WashWolfConfig config, @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);
        this.config = config;
        this.ownedDirtyWolfExistsCondition = new OwnedDirtyWolfExistsCondition(config.scanRangeHorizontal(), config.scanRangeVertical());
        this.targetWolf = null;

        this.preconditions.add(this.ownedDirtyWolfExistsCondition);

        this.initializeStateMachine(this.createControlStep(), WashStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("WashWolfBehavior")
                .initialStage(WashStage.WASH_WOLF)
                .stageStepMap(Map.of(
                        WashStage.WASH_WOLF, this.createWashWolfStep(),
                        WashStage.DRY_WOLF, this.createDryWolfStep()))
                .nextStage(WashStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createWashWolfStep() {
        TimeBasedStep<BaseVillager> washStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(WASH_DURATION.asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.WATER_BUCKET.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addPeriodicStep(WASH_EFFECT_INTERVAL_TICKS, this::playWashEffects)
                .addPeriodicStep(INTERACT_RETRIGGER_INTERVAL_TICKS, context -> {
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(context -> StepResult.transition(WashStage.DRY_WOLF))
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NAVIGATION_SPEED, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(washStep)
                .build();
    }

    private BehaviorStep<BaseVillager> createDryWolfStep() {
        TimeBasedStep<BaseVillager> dryStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(DRY_DURATION.asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.SPONGE.getDefaultInstance());
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(this::completeWash)
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NAVIGATION_SPEED, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(dryStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.targetWolf = null;

        if (!this.ownedDirtyWolfExistsCondition.test(villager)) {
            this.requestStop("No owned dirty wolves found within range");
            return;
        }

        this.targetWolf = this.ownedDirtyWolfExistsCondition.getTargets().stream().findFirst().orElse(null);
        if (this.targetWolf == null) {
            this.requestStop("Chosen dirty wolf is null");
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.targetWolf)));
        this.targetWolf.setOrderedToSit(false);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();

        this.targetWolf = null;
    }

    private StepResult playWashEffects(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.targetWolf == null || !this.targetWolf.isAlive()) {
            return StepResult.complete();
        }

        Location wolfLocation = Location.fromEntity(this.targetWolf, false);
        wolfLocation.displayParticles(ParticleTypes.SPLASH, 8, 0.35D, 0.5D, 0.35D, 0.02D);
        wolfLocation.playSound(SoundEvents.GENERIC_SPLASH, 0.35F, 1.2F, SoundSource.NEUTRAL);
        ((WolfMixin) this.targetWolf).setIsWet(true);
        return StepResult.noOp();
    }

    private StepResult completeWash(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.targetWolf == null || !this.targetWolf.isAlive()) {
            return StepResult.complete();
        }

        Location wolfLocation = Location.fromEntity(this.targetWolf, false);
        wolfLocation.displayParticles(ParticleTypes.CLOUD, 10, 0.4D, 0.5D, 0.4D, 0.02D);
        wolfLocation.playSound(SoundEvents.FIRE_EXTINGUISH, 0.45F, 1.4F, SoundSource.NEUTRAL);
        this.targetWolf.setDirty(false);
        return StepResult.complete();
    }

}
