package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.cuccos.CuccoEntity;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class ChaseChickensBehavior extends VillagerStateMachineBehavior {

    private static final float STRIKE_DAMAGE = 0.01F;
    private static final double CHASE_SPAWN_RADIUS = 4.0D;
    private static final double CHASE_REACH_DISTANCE = 1.5D;
    private static final int SPAWN_HEIGHT = 4;
    private static final int NORMAL_STRIKES = 2;
    private static final int STRIKE_DURATION_TICKS = 10;
    private static final int STRIKE_IMPACT_TICK = 4;
    private static final int CHASE_TIMEOUT_TICKS = ClockTicks.seconds(8).getTicksAsInt();
    private static final ClockTicks CHASE_CUCCO_LIFETIME = ClockTicks.seconds(30);

    private static final double SWARM_RADIUS = 2.0D;
    private static final int REVENGE_SPAWN_WINDOW = ClockTicks.seconds(5).getTicksAsInt();
    private static final int REVENGE_FALL_SOUND_INTERVAL = ClockTicks.seconds(0.5D).getTicksAsInt();
    private static final int SWARM_SPAWN_INTERVAL = 4;
    private static final ClockTicks SWARM_CUCCO_LIFETIME = ClockTicks.seconds(2);

    private enum Stage implements StageKey {
        SPAWN_TARGET,
        CHASE_MOVE,
        CHASE_LOOP,
        REVENGE_ROLL,
        REVENGE_STRIKE,
        REVENGE,
        END
    }

    private final double revengeSwarmChance;

    @Nullable
    private CuccoEntity targetChicken;
    private int normalStrikesLanded;

    public ChaseChickensBehavior(@Nonnull ChaseChickensConfig config, @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);
        this.revengeSwarmChance = config.revengeSwarmChance();
        this.targetChicken = null;
        this.normalStrikesLanded = 0;
        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ChaseChickensBehavior")
                .initialStage(Stage.SPAWN_TARGET)
                .stageStepMap(Map.of(
                        Stage.SPAWN_TARGET, this.createSpawnTargetStep(),
                        Stage.CHASE_MOVE, this.createChaseMoveStep(),
                        Stage.CHASE_LOOP, LoopBackStep.<BaseVillager>builder()
                                .name("ChickenChaseLoop")
                                .loopBackTo(Stage.CHASE_MOVE)
                                .completionTransition(Stage.REVENGE_ROLL)
                                .maxIterationsResolver(ctx -> NORMAL_STRIKES)
                                .build(),
                        Stage.REVENGE_ROLL, this.createRevengeRollStep(),
                        Stage.REVENGE_STRIKE, this.createRevengeStrikeStep(),
                        Stage.REVENGE, this.createRevengeStep()))
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createSpawnTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("ChickenSpawnTarget")
                .action(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    Location spawnLocation = this.pickSpawnLocation(villager, CHASE_SPAWN_RADIUS);

                    this.normalStrikesLanded = 0;
                    this.targetChicken = CuccoEntity.spawn(spawnLocation, CHASE_CUCCO_LIFETIME);
                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.targetChicken)));
                    return StepResult.transition(Stage.CHASE_MOVE);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createChaseMoveStep() {
        return this.createStrikeStep("ChickenStrike", Stage.CHASE_LOOP, this::strikeTargetChicken);
    }

    private BehaviorStep<BaseVillager> createRevengeStrikeStep() {
        return this.createStrikeStep("ChickenRevengeStrike", Stage.REVENGE, this::strikeAndPoofTargetChicken);
    }

    private BehaviorStep<BaseVillager> createStrikeStep(@Nonnull String name,
                                                        @Nonnull Stage nextStage,
                                                        @Nonnull BehaviorStep<BaseVillager> impactStep) {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CHASE_REACH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.RUN, 1))
                .actionStep(TimeBasedStep.<BaseVillager>builder()
                        .name(name)
                        .withTickable(ClockTicks.of(STRIKE_DURATION_TICKS).asTickable())
                        .onStart(ctx -> {
                            ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                            return StepResult.noOp();
                        })
                        .addKeyFrame(ClockTicks.of(STRIKE_IMPACT_TICK), impactStep)
                        .onEnd(ctx -> StepResult.transition(nextStage))
                        .build())
                .timeoutTicks(CHASE_TIMEOUT_TICKS)
                .timeoutTransition(nextStage)
                .build();
    }

    private StepResult strikeTargetChicken(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.targetChicken == null || !this.targetChicken.isAlive() || this.targetChicken.isRemoved()) {
            return StepResult.transition(Stage.CHASE_LOOP);
        }

        BaseVillager villager = context.getInitiator();
        this.targetChicken.hurt(villager.damageSources().mobAttack(villager), STRIKE_DAMAGE);

        Location targetLocation = Location.fromEntity(this.targetChicken, false);
        ParticleRegistry.featherPoof(targetLocation);

        this.normalStrikesLanded++;

        // Lay an egg
        if (this.normalStrikesLanded == NORMAL_STRIKES) {
            this.targetChicken.spawnAtLocation(Items.EGG);
            targetLocation.playSound(SoundEvents.CHICKEN_EGG, 1.0F, 1.0F, SoundSource.NEUTRAL);
        }

        if (context.primaryDeed().isEmpty()) {
            BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.CHICKENS_CHASED, null);
            outcome.markSucceeded();
            context.declarePrimaryDeed(outcome);
        }

        return StepResult.noOp();
    }

    private StepResult strikeAndPoofTargetChicken(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.targetChicken == null || !this.targetChicken.isAlive() || this.targetChicken.isRemoved()) {
            return StepResult.transition(Stage.REVENGE);
        }

        BaseVillager villager = context.getInitiator();
        this.targetChicken.hurt(villager.damageSources().mobAttack(villager), STRIKE_DAMAGE);

        Location targetLocation = Location.fromEntity(this.targetChicken, false);
        ParticleRegistry.featherPoof(targetLocation);
        this.targetChicken.discard();
        this.targetChicken = null;
        return StepResult.noOp();
    }

    private BehaviorStep<BaseVillager> createRevengeRollStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("ChickenRevengeRoll")
                .action(ctx -> {
                    if (RandomUtil.chance(this.revengeSwarmChance)) {
                        return StepResult.transition(Stage.REVENGE_STRIKE);
                    }
                    return StepResult.transition(Stage.END);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createRevengeStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("CuccoRevengeSwarm")
                .withTickable(ClockTicks.of(REVENGE_SPAWN_WINDOW).asTickable())
                .onStart(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    Location villagerLocation = Location.fromEntity(villager, false);

                    // Play the call before the cuccos arrive
                    SoundRegistry.CUCCO_CALL.playGlobally(villagerLocation, SoundSource.NEUTRAL);

                    // Stop navigation so the nitwit stands still and reads the storm rather than fleeing
                    villager.getNavigationManager().stop();
                    ctx.addSecondaryDeed(BehaviorOutcome.forDeed(WorldEventType.CHICKENS_REVENGED, null)).markSucceeded();
                    return StepResult.noOp();
                })
                .addPeriodicStep(REVENGE_FALL_SOUND_INTERVAL, ctx -> {
                    Location villagerLocation = Location.fromEntity(ctx.getInitiator(), false);
                    SoundRegistry.CUCCO_FALL.playGlobally(villagerLocation, SoundSource.NEUTRAL);
                    return StepResult.noOp();
                })
                .addPeriodicStep(SWARM_SPAWN_INTERVAL, ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    Location cuccoLocation = this.pickSpawnLocation(villager, SWARM_RADIUS);

                    CuccoEntity cucco = CuccoEntity.spawn(cuccoLocation, SWARM_CUCCO_LIFETIME);
                    cucco.setTarget(villager);

                    ParticleRegistry.featherPoof(cuccoLocation);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(Stage.END))
                .build();
    }

    private Location pickSpawnLocation(@Nonnull BaseVillager villager, double maxRadius) {
        double angle = RandomUtil.randomDouble(0, 2 * Math.PI);
        double radius = RandomUtil.randomDouble(0, maxRadius);
        return Location.of(villager.getX() + Math.cos(angle) * radius, villager.getY() + SPAWN_HEIGHT,
                villager.getZ() + Math.sin(angle) * radius, villager.level());
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return villager.isAlive() && !villager.isRemoved();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
        if (this.targetChicken != null && !this.targetChicken.isRemoved()) {
            this.targetChicken.discard();
        }
        this.targetChicken = null;
        this.normalStrikesLanded = 0;
    }

}
