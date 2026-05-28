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
import dev.breezes.settlements.domain.ai.conditions.NearbyRipeCropCondition;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class HarvestRipeCropsBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, HARVEST, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final HarvestRipeCropsConfig config;
    private final NearbyRipeCropCondition<BaseVillager> nearbyCropCondition;

    public HarvestRipeCropsBehavior(@Nonnull HarvestRipeCropsConfig config,
                                    @Nonnull HungerConfig hungerConfig) {
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                hungerConfig,
                config.experienceReward());
        this.config = config;
        this.nearbyCropCondition = NearbyRipeCropCondition.<BaseVillager>builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyCropCondition);

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(this.config.closeEnoughDistance())
                .navigateStep(new NavigateToTargetStep<>(this.config.movementSpeed(), 1))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtCrop")
                        .action(ctx -> StepResult.transition(Stage.HARVEST))
                        .build())
                .timeoutTicks(this.config.approachTimeoutTicks())
                .timeoutTransition(Stage.PICK_TARGET)
                .build());
        stageMap.put(Stage.HARVEST, this.createHarvestStep());
        stageMap.put(Stage.SETTLE, WaitStep.<BaseVillager>builder()
                .waitTime(SETTLE_DURATION.asTickable())
                .nextStage(Stage.PICKUP)
                .build());
        stageMap.put(Stage.PICKUP, PickupItemsStep.builder()
                .name("PickupCropDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("RipeCropsLoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> 8 * ctx.getInitiator().getExpertise().getLevel())
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardRipeCropsXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("HarvestRipeCropsBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickRipeCropTarget")
                .action(ctx -> {
                    Level world = ctx.getInitiator().level();
                    List<BlockPos> candidates = this.nearbyCropCondition.getTargets().stream()
                            .filter(pos -> this.nearbyCropCondition.stillMatches(pos, world))
                            .toList();
                    if (candidates.isEmpty()) {
                        log.behaviorStatus("No additional ripe crop targets found, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }
                    BlockPos picked = RandomUtil.choice(candidates);
                    log.behaviorStatus("New ripe crop target found at {}, continuing behavior", picked.toShortString());

                    BlockState state = world.getBlockState(picked);
                    ctx.setState(BehaviorStateType.TARGET,
                            TargetState.of(Targetable.fromBlock(
                                    PhysicalBlock.of(Location.of(picked, world), state))));
                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("HarvestRipeCrop")
                .withTickable(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    // No held item for hand-pulling crops — bare-hand interaction.
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_PEAK_TICK), ctx -> {
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
                    // Guard against the crop being harvested by another agent between targeting and arrival.
                    if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
                        return StepResult.noOp();
                    }
                    Location effectLocation = Location.of(pos, world).center(true).add(0, 0.5, 0, true);
                    ParticleRegistry.harvestBlock(effectLocation, state);
                    effectLocation.playSound(state.getSoundType(world, pos, villager).getBreakSound(), 0.8f, 1.0f, SoundSource.BLOCKS);

                    List<ItemStack> drops = Block.getDrops(state, server, pos, null, villager, ItemStack.EMPTY);
                    List<ItemEntity> spawned = Location.of(pos, world).center(true).dropItems(drops, true);
                    spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

                    // Reset to age 0 so the crop regrows from seed rather than being destroyed outright
                    world.setBlockAndUpdate(pos, crop.getStateForAge(0));
                    ctx.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));
                    ctx.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.success());
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(Stage.SETTLE))
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
