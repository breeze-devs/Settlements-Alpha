package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.InteractionOutcomeState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.AwardExperienceStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.PickupItemsStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.WaitStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyRipePumpkinCondition;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.SwingAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class HarvestPumpkinBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, CHOP, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final HarvestPumpkinConfig config;
    private final NearbyRipePumpkinCondition<BaseVillager> nearbyPumpkinCondition;

    public HarvestPumpkinBehavior(@Nonnull HarvestPumpkinConfig config,
                                  @Nonnull HungerConfig hungerConfig) {
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                hungerConfig,
                config.experienceReward());
        this.config = config;
        this.nearbyPumpkinCondition = NearbyRipePumpkinCondition.<BaseVillager>builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyPumpkinCondition);

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(this.config.closeEnoughDistance())
                .navigateStep(new NavigateToTargetStep<>(this.config.movementSpeed(), 1))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtPumpkin")
                        .action(ctx -> StepResult.transition(Stage.CHOP))
                        .build())
                .timeoutTicks(this.config.approachTimeoutTicks())
                .timeoutTransition(Stage.PICK_TARGET)
                .build());
        stageMap.put(Stage.CHOP, this.createChopStep());
        stageMap.put(Stage.SETTLE, WaitStep.<BaseVillager>builder()
                .waitTime(SETTLE_DURATION.asTickable())
                .nextStage(Stage.PICKUP)
                .build());
        stageMap.put(Stage.PICKUP, PickupItemsStep.builder()
                .name("PickupPumpkinDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("PumpkinLoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> 4 * ctx.getInitiator().getExpertise().getLevel())
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardPumpkinXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("HarvestPumpkinBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickPumpkinTarget")
                .action(ctx -> {
                    Level world = ctx.getInitiator().level();
                    List<BlockPos> candidates = this.nearbyPumpkinCondition.getTargets().stream()
                            .filter(pos -> this.nearbyPumpkinCondition.stillMatches(pos, world))
                            .toList();
                    if (candidates.isEmpty()) {
                        log.behaviorStatus("No additional pumpkin targets found, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }
                    BlockPos picked = RandomUtil.choice(candidates);
                    log.behaviorStatus("New pumpkin target found at {}, continuing behavior", picked.toShortString());

                    BlockState state = world.getBlockState(picked);
                    ctx.setState(BehaviorStateType.TARGET,
                            TargetState.of(Targetable.fromBlock(
                                    PhysicalBlock.of(Location.of(picked, world), state))));
                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createChopStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("ChopPumpkin")
                .withTickable(ClockTicks.of(SwingAnimations.SWING_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.IRON_AXE.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.SWING_HEAVY);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(SwingAnimations.SWING_IMPACT_TICKS), ctx -> {
                    Optional<BlockPos> target = TargetQueries.firstBlockPos(ctx);
                    if (target.isEmpty()) {
                        return StepResult.noOp();
                    }
                    BaseVillager villager = ctx.getInitiator();
                    Level world = villager.level();
                    BlockPos pos = target.get();
                    BlockState state = world.getBlockState(pos);

                    if (!(world instanceof ServerLevel server)) {
                        return StepResult.noOp();
                    }
                    // Guard against the target being replaced while the villager was approaching it.
                    if (!state.is(Blocks.PUMPKIN)) {
                        return StepResult.noOp();
                    }
                    Location effectLocation = Location.of(pos, world).center(true).add(0, 0.5, 0, true);
                    ParticleRegistry.harvestBlock(effectLocation, state);
                    effectLocation.playSound(state.getSoundType(world, pos, villager).getBreakSound(), 0.8f, 1.0f, SoundSource.BLOCKS);

                    List<ItemStack> drops = Block.getDrops(state, server, pos, null, villager, ItemStack.EMPTY);
                    List<ItemEntity> spawned = Location.of(pos, world).center(true).dropItems(drops, true);
                    spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

                    world.removeBlock(pos, false);
                    ctx.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));
                    ctx.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.success());
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.transition(Stage.SETTLE);
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.empty());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
    }

}
