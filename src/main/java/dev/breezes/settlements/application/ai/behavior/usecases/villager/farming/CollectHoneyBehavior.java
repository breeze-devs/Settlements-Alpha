package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.AwardExperienceStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class CollectHoneyBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation GLASS_BOTTLE_ID = ResourceLocation.withDefaultNamespace("glass_bottle");
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();

    private enum CollectStage implements StageKey {
        PICK_TARGET,
        APPROACH,
        COLLECT_HONEY,
        LOOP,
        AWARD,
        END;
    }

    private final BlockMatcher fullHiveMatcher;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final CollectHoneyConfig config;
    private final CollectHoneyYieldDataManager yieldData;
    private final BlockMemoryTargetResolver targetResolver;

    public CollectHoneyBehavior(@Nonnull CollectHoneyConfig config,
                                @Nonnull BehaviorSupport support,
                                @Nonnull CollectHoneyYieldDataManager yieldData) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());
        this.config = config;
        this.yieldData = yieldData;
        this.targetResolver = support.getBlockMemoryTargetResolver();
        this.fullHiveMatcher = BlockMatchers.FULL_HIVE;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;

        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.FULL_HIVE_SITES)
                .matcher(this.fullHiveMatcher)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .completionRange(1)
                .description("Known full hive sites")
                .build());
        this.preconditions.add(support.getDemandSignalService().requireItem(new ItemMatch.ItemRef(GLASS_BOTTLE_ID), 1, 50, this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), CollectStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(CollectStage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(CollectStage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(1)
                        .unreachableTransition(CollectStage.PICK_TARGET)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtHoneyHive")
                        .action(context -> StepResult.transition(CollectStage.COLLECT_HONEY))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(CollectStage.PICK_TARGET)
                .build());
        stageMap.put(CollectStage.COLLECT_HONEY, this.createCollectStep());
        stageMap.put(CollectStage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("CollectHoneyLoopBack")
                .loopBackTo(CollectStage.PICK_TARGET)
                .completionTransition(CollectStage.AWARD)
                .maxIterationsResolver(context -> this.config.expertiseHarvestLimit()
                        .getOrDefault(context.getInitiator().getExpertise().getConfigName(), 1))
                .build());
        stageMap.put(CollectStage.AWARD, AwardExperienceStep.builder()
                .name("AwardCollectHoneyXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(CollectStage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("CollectHoneyBehavior")
                .initialStage(CollectStage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(CollectStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickHoneyHiveTarget")
                .action(context -> {
                    BaseVillager villager = context.getInitiator();
                    if (!this.canHarvest(villager)) {
                        return StepResult.transition(CollectStage.AWARD);
                    }

                    boolean resolved = this.targetResolver.resolveBlockTarget(context, MemoryTypeRegistry.FULL_HIVE_SITES,
                            this.fullHiveMatcher, this.confirmBox, this.maxConfirms, 1);
                    if (!resolved) {
                        log.behaviorStatus("No additional full hive targets found, ending honey collection");
                        return StepResult.transition(CollectStage.AWARD);
                    }

                    return StepResult.transition(CollectStage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createCollectStep() {
        TimeBasedStep<BaseVillager> collectStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(2).asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.GLASS_BOTTLE.getDefaultInstance());
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), this::performHarvest)
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return StepResult.transition(CollectStage.LOOP);
                })
                .build();
        return collectStep;
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "honey bottles"));
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());

        if (!this.canHarvest(entity)) {
            this.requestStop("Precondition check at behavior start failed");
        }
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();
    }

    private boolean canHarvest(@Nonnull BaseVillager villager) {
        VillagerInventory inventory = villager.getSettlementsInventory();
        return inventory.containsOrBypassed(Items.GLASS_BOTTLE, GeneralConfig.bypassInventoryRequirements);
    }

    private StepResult performHarvest(@Nonnull BehaviorContext<BaseVillager> context) {
        Optional<BlockPos> target = TargetQueries.firstBlockPos(context);
        if (target.isEmpty()) {
            return StepResult.noOp();
        }

        BaseVillager villager = context.getInitiator().getMinecraftEntity();
        Level level = villager.level();
        BlockPos pos = target.get();
        BlockState state = level.getBlockState(pos);

        // The hive can be emptied by another actor while the villager is walking to it.
        if (AabbBlockScan.findFirst(pos, BlockScanBox.self(), this.fullHiveMatcher, level).isEmpty()) {
            return StepResult.noOp();
        }

        VillagerInventory inventory = villager.getSettlementsInventory();
        if (!inventory.containsOrBypassed(Items.GLASS_BOTTLE, GeneralConfig.bypassInventoryRequirements)) {
            return StepResult.noOp();
        }
        if (!inventory.consumeIfRequired(Items.GLASS_BOTTLE, 1, GeneralConfig.bypassInventoryRequirements)) {
            return StepResult.noOp();
        }

        String harvestedBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String expertiseName = villager.getExpertise().getConfigName();
        int collectedCount = 0;
        for (ItemStack drop : this.yieldData.rollDrops(expertiseName, harvestedBlockId)) {
            if (!drop.isEmpty()) {
                inventory.add(drop);
                collectedCount += drop.getCount();
            }
        }

        level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
        SoundRegistry.COLLECT_HONEY.playGlobally(Location.of(pos, level), SoundSource.BLOCKS);
        context.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                .ifPresent(visitedSites -> visitedSites.addSite(GlobalPos.of(level.dimension(), pos)));
        int finalCollectedCount = collectedCount;
        context.primaryDeed()
                .ifPresent(outcome -> outcome.recordYield(finalCollectedCount));
        return StepResult.noOp();
    }

}
