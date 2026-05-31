package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
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
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
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
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
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
public class HarvestHoneycombBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation SHEARS_ITEM_ID = ResourceLocation.withDefaultNamespace("shears");
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final float MOVEMENT_SPEED = 0.55F;

    private enum HarvestStage implements StageKey {
        PICK_TARGET,
        APPROACH,
        HARVEST_HONEYCOMB,
        LOOP,
        AWARD,
        END;
    }

    private final BlockMatcher fullHiveMatcher;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final HarvestHoneycombConfig config;
    private final HarvestHoneycombYieldDataManager yieldData;
    private final BlockMemoryTargetResolver targetResolver;

    public HarvestHoneycombBehavior(@Nonnull HarvestHoneycombConfig config,
                                    @Nonnull HungerConfig hungerConfig,
                                    @Nonnull HarvestHoneycombYieldDataManager yieldData,
                                    @Nonnull DemandSignalService demandSignalService,
                                    @Nonnull BlockMemoryTargetResolver targetResolver) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());
        this.config = config;
        this.yieldData = yieldData;
        this.targetResolver = targetResolver;
        this.fullHiveMatcher = BlockMatchers.FULL_HIVE;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;

        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.FULL_HIVE_SITES)
                .matcher(this.fullHiveMatcher)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .description("Known full hive sites")
                .build());
        this.preconditions.add(demandSignalService.requireItem(
                new ItemMatch.ItemRef(SHEARS_ITEM_ID), 1, 50, this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), HarvestStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(HarvestStage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(HarvestStage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep<>(MOVEMENT_SPEED, 1))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtHoneycombHive")
                        .action(context -> StepResult.transition(HarvestStage.HARVEST_HONEYCOMB))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(HarvestStage.PICK_TARGET)
                .build());
        stageMap.put(HarvestStage.HARVEST_HONEYCOMB, this.createHarvestStep());
        stageMap.put(HarvestStage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("HarvestHoneycombLoopBack")
                .loopBackTo(HarvestStage.PICK_TARGET)
                .completionTransition(HarvestStage.AWARD)
                .maxIterationsResolver(context -> this.config.expertiseHarvestLimit()
                        .getOrDefault(context.getInitiator().getExpertise().getConfigName(), 1))
                .build());
        stageMap.put(HarvestStage.AWARD, AwardExperienceStep.builder()
                .name("AwardHarvestHoneycombXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(HarvestStage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("HarvestHoneycombBehavior")
                .initialStage(HarvestStage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(HarvestStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickHoneycombHiveTarget")
                .action(context -> {
                    BaseVillager villager = context.getInitiator();
                    if (!this.canHarvest(villager)) {
                        return StepResult.transition(HarvestStage.AWARD);
                    }

                    boolean resolved = this.targetResolver.resolveBlockTarget(context, MemoryTypeRegistry.FULL_HIVE_SITES,
                            this.fullHiveMatcher, this.confirmBox, this.maxConfirms);
                    if (!resolved) {
                        log.behaviorStatus("No additional full hive targets found, ending honeycomb harvest");
                        return StepResult.transition(HarvestStage.AWARD);
                    }

                    return StepResult.transition(HarvestStage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        TimeBasedStep<BaseVillager> harvestStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), this::performHarvest)
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return StepResult.transition(HarvestStage.LOOP);
                })
                .build();
        return harvestStep;
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.setState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.empty());
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());

        if (!this.canHarvest(villager)) {
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
        return inventory.containsOrBypassed(Items.SHEARS, GeneralConfig.bypassInventoryRequirements);
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

        String harvestedBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String expertiseName = villager.getExpertise().getConfigName();
        for (ItemStack drop : this.yieldData.rollDrops(expertiseName, harvestedBlockId)) {
            if (!drop.isEmpty()) {
                villager.getSettlementsInventory().add(drop);
            }
        }

        level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
        SoundRegistry.HARVEST_HONEYCOMB.playGlobally(Location.of(pos, level), SoundSource.BLOCKS);
        context.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                .ifPresent(visitedSites -> visitedSites.addSite(GlobalPos.of(level.dimension(), pos)));
        context.getState(BehaviorStateType.INTERACTION_OUTCOME, InteractionOutcomeState.class)
                .ifPresent(InteractionOutcomeState::markSuccess);
        return StepResult.noOp();
    }

}
