package dev.breezes.settlements.application.ai.behavior.usecases.villager.mason;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
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
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.PickupItemsStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.WaitStep;
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.AnyOfCondition;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.DigAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.data.mason.ExcavateSubstrateYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class ExcavateSubstrateBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final ResourceLocation GRAVEL_BLOCK_ID = ResourceLocation.withDefaultNamespace("gravel");
    private static final ResourceLocation SAND_BLOCK_ID = ResourceLocation.withDefaultNamespace("sand");
    private static final ResourceLocation IRON_SHOVEL_ID = ResourceLocation.withDefaultNamespace("iron_shovel");
    private static final List<ExcavatableSubstrateSource> SOURCES = List.of(
            new ExcavatableSubstrateSource("gravel", MemoryTypeRegistry.GRAVEL_SITES, BlockMatchers.LOOSE_GRAVEL, GRAVEL_BLOCK_ID),
            new ExcavatableSubstrateSource("sand", MemoryTypeRegistry.SAND_SITES, BlockMatchers.LOOSE_SAND, SAND_BLOCK_ID)
    );

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, DIG, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final ExcavateSubstrateConfig config;
    private final ExcavateSubstrateYieldDataManager yieldData;
    private final BlockMemoryTargetResolver targetResolver;

    private ExcavatableSubstrateSource selectedSource;

    public ExcavateSubstrateBehavior(@Nonnull ExcavateSubstrateConfig config,
                                     @Nonnull HungerConfig hungerConfig,
                                     @Nonnull ExcavateSubstrateYieldDataManager yieldData,
                                     @Nonnull DemandSignalService demandSignalService,
                                     @Nonnull BlockMemoryTargetResolver targetResolver) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());
        this.config = config;
        this.yieldData = yieldData;
        this.targetResolver = targetResolver;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;

        this.preconditions.add(AnyOfCondition.<BaseVillager>builder()
                .conditions(SOURCES.stream().map(this::knownSitesPrecondition).toList())
                .description("Known substrate excavation sites")
                .build());
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(IRON_SHOVEL_ID), 1, 50,
                this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private IEntityCondition<BaseVillager> knownSitesPrecondition(@Nonnull ExcavatableSubstrateSource source) {
        return KnownBlockSitesPrecondition.builder()
                .memoryType(source.memoryType())
                .matcher(source.matcher())
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .description("Known " + source.name() + " sites")
                .build();
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(1.2)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtExcavationSite")
                        .action(ctx -> StepResult.transition(Stage.DIG))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(Stage.PICK_TARGET)
                .build());
        stageMap.put(Stage.DIG, this.createDigStep());
        stageMap.put(Stage.SETTLE, WaitStep.<BaseVillager>builder()
                .waitTime(SETTLE_DURATION.asTickable())
                .nextStage(Stage.PICKUP)
                .build());
        stageMap.put(Stage.PICKUP, PickupItemsStep.builder()
                .name("PickUpDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("LoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> this.config.expertiseHarvestLimit()
                        .getOrDefault(ctx.getInitiator().getExpertise().getConfigName(), 1))
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardExperience")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("ExcavateSubstrateBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickExcavationTarget")
                .action(ctx -> {
                    if (this.selectedSource == null) {
                        return StepResult.transition(Stage.AWARD);
                    }

                    boolean resolved = this.targetResolver.resolveBlockTarget(ctx, this.selectedSource.memoryType(),
                            this.selectedSource.matcher(), this.confirmBox, this.maxConfirms);
                    if (!resolved) {
                        log.behaviorStatus("Selected excavation site no longer has live sites, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }

                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createDigStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("ExcavateStep")
                .withTickable(ClockTicks.of(DigAnimations.DIG_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.IRON_SHOVEL.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.DIG);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(DigAnimations.DIG_IMPACT_TICKS), this::spawnDrops)
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.transition(Stage.SETTLE);
                })
                .build();
    }

    private StepResult spawnDrops(@Nonnull BehaviorContext<BaseVillager> context) {
        Optional<BlockPos> target = TargetQueries.firstBlockPos(context);
        if (target.isEmpty() || this.selectedSource == null) {
            return StepResult.noOp();
        }

        BaseVillager villager = context.getInitiator();
        Level level = villager.level();
        BlockPos pos = target.get();
        BlockState state = level.getBlockState(pos);

        if (AabbBlockScan.findFirst(pos, BlockScanBox.self(), this.selectedSource.matcher(), level).isEmpty()) {
            return StepResult.noOp();
        }

        Location effectLocation = Location.of(pos, level).center(true).add(0, 0.5, 0, true);
        ParticleRegistry.harvestBlock(effectLocation, state);
        effectLocation.playSound(state.getSoundType(level, pos, villager).getBreakSound(), 0.8F, 1.0F, SoundSource.BLOCKS);

        String harvestedBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String expertiseName = villager.getExpertise().getConfigName();
        List<ItemStack> drops = this.yieldData.rollDrops(expertiseName, harvestedBlockId);
        if (drops.isEmpty()) {
            return StepResult.noOp();
        }

        List<ItemEntity> spawned = effectLocation.dropItems(drops, true);
        spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

        // Loose ground is deliberately unconsumed, so the target must remain eligible for the session loop.
        context.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));

        context.getState(BehaviorStateType.BEHAVIOR_OUTCOME, BehaviorOutcome.class)
                .ifPresent(outcome -> {
                    outcome.recordYield(totalItemCount(drops));
                    // TODO: there's an issue with broadcasting when excavating multiple items, we'll get to it later
                    outcome.recordDeedDetail(drops.getFirst().getItem().toString());
                });

        return StepResult.noOp();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.setState(BehaviorStateType.BEHAVIOR_OUTCOME,
                BehaviorOutcome.forDeed(WorldEventType.RESOURCE_EXCAVATED, "substrate"));
        this.selectedSource = this.selectSource(villager).orElse(null);
        if (this.selectedSource == null) {
            this.requestStop("No excavation site available at behavior start");
        }
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.selectedSource = null;
    }

    private Optional<ExcavatableSubstrateSource> selectSource(@Nonnull BaseVillager villager) {
        Map<ExcavatableSubstrateSource, Double> weightsBySource = new LinkedHashMap<>();
        for (ExcavatableSubstrateSource source : SOURCES) {
            Optional<List<GlobalPos>> memory = villager.getSettlementsBrain().getMemory(source.memoryType());
            if (memory.isPresent() && !memory.get().isEmpty()) {
                weightsBySource.put(source, this.yieldData.selectionWeight(source.selectionBlockId().toString()));
            }
        }

        if (weightsBySource.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RandomUtil.weightedChoice(weightsBySource));
    }

    private static int totalItemCount(@Nonnull List<ItemStack> drops) {
        return drops.stream().mapToInt(ItemStack::getCount).sum();
    }

    private record ExcavatableSubstrateSource(@Nonnull String name,
                                              @Nonnull MemoryType<List<GlobalPos>> memoryType,
                                              @Nonnull BlockMatcher matcher,
                                              @Nonnull ResourceLocation selectionBlockId) {
    }

}
