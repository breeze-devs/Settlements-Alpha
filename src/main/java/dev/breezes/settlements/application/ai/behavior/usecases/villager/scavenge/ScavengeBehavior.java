package dev.breezes.settlements.application.ai.behavior.usecases.villager.scavenge;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
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
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.HarvestCropAnimations;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.data.scavenge.ScavengeYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class ScavengeBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();

    private static final int DEFAULT_FORAGE_BUDGET = 3;

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, FORAGE, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final BlockMatcher floraMatcher;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final ScavengeConfig config;
    private final ScavengeYieldDataManager yieldData;
    private final BlockMemoryTargetResolver targetResolver;

    public ScavengeBehavior(@Nonnull ScavengeConfig config,
                            @Nonnull BehaviorSupport support,
                            @Nonnull ScavengeYieldDataManager yieldData) {
        // XP is awarded through the AWARD stage's AwardExperienceStep, not the base reward path,
        // so the experience-reward constructor overload is deliberately not used here.
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                support);
        this.config = config;
        this.yieldData = yieldData;
        this.targetResolver = support.getBlockMemoryTargetResolver();
        this.floraMatcher = BlockMatchers.SCAVENGEABLE_FLORA;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;
        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.SCAVENGEABLE_FLORA_SITES)
                .matcher(this.floraMatcher)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .completionRange(1)
                .description("Known scavengeable flora sites")
                .build());

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(1.5)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(1)
                        .unreachableTransition(Stage.PICK_TARGET)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtFlora")
                        .action(ctx -> StepResult.transition(Stage.FORAGE))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(Stage.PICK_TARGET)
                .build());
        stageMap.put(Stage.FORAGE, this.createForageStep());
        stageMap.put(Stage.SETTLE, WaitStep.<BaseVillager>builder()
                .waitTime(SETTLE_DURATION.asTickable())
                .nextStage(Stage.PICKUP)
                .build());
        stageMap.put(Stage.PICKUP, PickupItemsStep.builder()
                .name("PickupForageDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("ScavengeLoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> forageBudget(ctx.getInitiator().getExpertise()))
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardScavengeXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("ScavengeBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickForageTarget")
                .action(ctx -> {
                    boolean resolved = this.targetResolver.resolveBlockTarget(ctx, MemoryTypeRegistry.SCAVENGEABLE_FLORA_SITES,
                            this.floraMatcher, this.confirmBox, this.maxConfirms, 1);
                    if (!resolved) {
                        log.behaviorStatus("No additional forage targets found, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }

                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createForageStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("ForageFlora")
                .withTickable(ClockTicks.of(HarvestCropAnimations.HARVEST_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.HARVEST);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(HarvestCropAnimations.HARVEST_AT_TICK), ctx -> {
                    Optional<BlockPos> target = TargetQueries.firstBlockPos(ctx);
                    if (target.isEmpty()) {
                        return StepResult.noOp();
                    }
                    BaseVillager villager = ctx.getInitiator();
                    Level world = villager.level();
                    BlockPos pos = target.get();
                    BlockState state = world.getBlockState(pos);

                    // Guard against the foliage being removed or replaced while the villager was approaching it
                    if (AabbBlockScan.findFirst(pos, BlockScanBox.self(), this.floraMatcher, world).isEmpty()) {
                        return StepResult.noOp();
                    }

                    Location effectLocation = Location.of(pos, world).center(true).add(0, 0.5, 0, true);
                    ParticleRegistry.harvestBlock(effectLocation, state);
                    effectLocation.playSound(SoundEvents.GRASS_BREAK, 0.8f, 1.0f, SoundSource.BLOCKS);

                    String harvestedBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    String expertiseName = villager.getExpertise().getConfigName();
                    List<ItemStack> drops = this.yieldData.rollDrops(expertiseName, harvestedBlockId);

                    List<ItemEntity> spawned = effectLocation.dropItems(drops, true);
                    spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

                    ctx.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));
                    ctx.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                            .ifPresent(visitedSites -> visitedSites.addSite(GlobalPos.of(world.dimension(), pos)));
                    ctx.primaryDeed()
                            .ifPresent(outcome -> outcome.recordYield(totalItemCount(drops)));
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(Stage.SETTLE))
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "forage"));
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
    }

    private int forageBudget(@Nonnull Expertise expertise) {
        return this.config.expertiseForageBudget().getOrDefault(expertise.getConfigName(), DEFAULT_FORAGE_BUDGET);
    }

    private static int totalItemCount(@Nonnull List<ItemStack> drops) {
        return drops.stream().mapToInt(ItemStack::getCount).sum();
    }

}
