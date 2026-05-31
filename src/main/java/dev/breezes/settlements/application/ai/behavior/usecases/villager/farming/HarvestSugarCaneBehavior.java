package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.InteractionOutcomeState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
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
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class HarvestSugarCaneBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, HARVEST, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final BlockMatcher sugarCaneMatcher;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final HarvestSugarCaneConfig config;
    private final BlockMemoryTargetResolver targetResolver;

    public HarvestSugarCaneBehavior(@Nonnull HarvestSugarCaneConfig config,
                                    @Nonnull HungerConfig hungerConfig,
                                    @Nonnull BlockMemoryTargetResolver targetResolver) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());
        this.config = config;
        this.targetResolver = targetResolver;
        this.sugarCaneMatcher = BlockMatchers.HARVESTABLE_SUGARCANE;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;
        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.HARVESTABLE_SUGARCANE_SITES)
                .matcher(this.sugarCaneMatcher)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .description("Known harvestable sugar cane sites")
                .build());

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.5)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtSugarCane")
                        .action(ctx -> StepResult.transition(Stage.HARVEST))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(Stage.PICK_TARGET)
                .build());
        stageMap.put(Stage.HARVEST, this.createHarvestStep());
        stageMap.put(Stage.SETTLE, WaitStep.<BaseVillager>builder()
                .waitTime(SETTLE_DURATION.asTickable())
                .nextStage(Stage.PICKUP)
                .build());
        stageMap.put(Stage.PICKUP, PickupItemsStep.builder()
                .name("PickupSugarCaneDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("SugarCaneLoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> 4 * ctx.getInitiator().getExpertise().getLevel())
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardSugarCaneXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("HarvestSugarCaneBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickSugarCaneTarget")
                .action(ctx -> {
                    boolean resolved = this.targetResolver.resolveBlockTarget(ctx, MemoryTypeRegistry.HARVESTABLE_SUGARCANE_SITES,
                            this.sugarCaneMatcher, this.confirmBox, this.maxConfirms);
                    if (!resolved) {
                        log.behaviorStatus("No additional sugar cane targets found, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }

                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("HarvestSugarCane")
                .withTickable(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
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

                    if (!(world instanceof ServerLevel)) {
                        return StepResult.noOp();
                    }

                    // The second block convention keeps the root cane intact even when modded cane grows taller.
                    if (AabbBlockScan.findFirst(pos, BlockScanBox.self(), this.sugarCaneMatcher, world).isEmpty()) {
                        return StepResult.noOp();
                    }

                    List<BlockPos> harvestedPositions = this.collectSugarCaneBlocksToHarvest(world, pos);
                    if (harvestedPositions.isEmpty()) {
                        return StepResult.noOp();
                    }

                    Location effectLocation = Location.of(pos, world).center(true).add(0, 0.5, 0, true);
                    ParticleRegistry.harvestBlock(effectLocation, state);
                    effectLocation.playSound(SoundEvents.GRASS_BREAK, 0.8f, 1.0f, SoundSource.BLOCKS);

                    List<ItemStack> drops = this.createSugarCaneDropStacks(harvestedPositions.size());
                    List<ItemEntity> spawned = Location.of(pos, world).center(true).dropItems(drops, true);
                    spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

                    harvestedPositions.forEach(harvestedPos -> world.removeBlock(harvestedPos, false));
                    ctx.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                            .ifPresent(visitedSites -> visitedSites.addSite(GlobalPos.of(world.dimension(), pos)));
                    ctx.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));
                    ctx.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.success());
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(Stage.SETTLE))
                .build();
    }

    private List<BlockPos> collectSugarCaneBlocksToHarvest(@Nonnull Level world, @Nonnull BlockPos start) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos current = start;
        for (int scanned = 0; scanned < this.config.maxHarvestBlocks(); scanned++) {
            if (!world.getBlockState(current).is(Blocks.SUGAR_CANE)) {
                break;
            }

            positions.add(current);
            current = current.above();
        }
        return positions;
    }

    private List<ItemStack> createSugarCaneDropStacks(int harvestedBlockCount) {
        List<ItemStack> drops = new ArrayList<>();
        int remaining = harvestedBlockCount;
        int maxStackSize = Items.SUGAR_CANE.getDefaultInstance().getMaxStackSize();
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            drops.add(new ItemStack(Items.SUGAR_CANE, stackSize));
            remaining -= stackSize;
        }
        return drops;
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.empty());
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
    }

}
