package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

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
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.blocks.BlockRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.ChopAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.factory.ConfigFactory;
import dev.breezes.settlements.infrastructure.minecraft.blocks.OreRegenConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class HarvestOreBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks SETTLE_DURATION = ClockTicks.seconds(1);
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final ResourceLocation IRON_PICKAXE_ID = ResourceLocation.withDefaultNamespace("iron_pickaxe");

    private enum Stage implements StageKey {
        PICK_TARGET, APPROACH, HARVEST, SETTLE, PICKUP, LOOP, AWARD, END
    }

    private final BlockMatcher oreMatcher;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;
    private final HarvestOreConfig config;
    private final BlockMemoryTargetResolver targetResolver;

    public HarvestOreBehavior(@Nonnull HarvestOreConfig config,
                              @Nonnull HungerConfig hungerConfig,
                              @Nonnull DemandSignalService demandSignalService,
                              @Nonnull BlockMemoryTargetResolver targetResolver) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());
        this.config = config;
        this.targetResolver = targetResolver;
        this.oreMatcher = BlockMatchers.HARVESTABLE_ORE;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;
        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.ORE_SITES)
                .matcher(this.oreMatcher)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .completionRange(2)
                .description("Known ore sites")
                .build());
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(IRON_PICKAXE_ID), 1, 50,
                this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_TARGET, this.createPickTargetStep());
        stageMap.put(Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.5)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(2)
                        .unreachableTransition(Stage.PICK_TARGET)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtOre")
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
                .name("PickupOreDrops")
                .nextStage(Stage.LOOP)
                .build());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("OreLoopBack")
                .loopBackTo(Stage.PICK_TARGET)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> this.config.expertiseHarvestLimit()
                        .getOrDefault(ctx.getInitiator().getExpertise().getConfigName(), 1))
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardOreXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("HarvestOreBehavior")
                .initialStage(Stage.PICK_TARGET)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickTargetStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickOreTarget")
                .action(ctx -> {
                    boolean resolved = this.targetResolver.resolveBlockTarget(ctx, MemoryTypeRegistry.ORE_SITES,
                            this.oreMatcher, this.confirmBox, this.maxConfirms, 2);
                    if (!resolved) {
                        log.behaviorStatus("No additional ore targets found, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }

                    return StepResult.transition(Stage.APPROACH);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createHarvestStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("HarvestOre")
                .withTickable(ClockTicks.of(ChopAnimations.CHOP_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.IRON_PICKAXE.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.SWING_HEAVY);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(ChopAnimations.CHOP_IMPACT_TICKS), this::replaceOre)
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    ctx.getInitiator().setMotion(AnimationArchetype.IDLE);
                    return StepResult.transition(Stage.SETTLE);
                })
                .build();
    }

    private StepResult replaceOre(@Nonnull BehaviorContext<BaseVillager> context) {
        Optional<BlockPos> target = TargetQueries.firstBlockPos(context);
        if (target.isEmpty()) {
            return StepResult.noOp();
        }

        BaseVillager villager = context.getInitiator();
        Level world = villager.level();
        BlockPos pos = target.get();
        BlockState oreState = world.getBlockState(pos);

        if (!(world instanceof ServerLevel server)) {
            return StepResult.noOp();
        }

        // Ore can be mined by another actor while this villager is approaching the remembered site.
        if (AabbBlockScan.findFirst(pos, BlockScanBox.self(), this.oreMatcher, world).isEmpty()) {
            return StepResult.noOp();
        }

        Location effectLocation = Location.of(pos, world).center(true).add(0, 0.5, 0, true);
        ParticleRegistry.harvestBlock(effectLocation, oreState);
        effectLocation.playSound(oreState.getSoundType(world, pos, villager).getBreakSound(), 0.8F, 1.0F, SoundSource.BLOCKS);

        // Reserve item drops
        List<ItemStack> drops = Block.getDrops(oreState, server, pos, null, villager, ItemStack.EMPTY);
        List<ItemEntity> spawned = Location.of(pos, world).center(true).dropItems(drops, true);
        spawned.forEach(itemEntity -> itemEntity.setPickUpDelay(ClockTicks.seconds(5).getTicksAsInt()));

        // Replace block
        BlockState replacement = this.createReplacementState(oreState);
        world.setBlockAndUpdate(pos, replacement);

        context.getState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.class)
                .ifPresent(visitedSites -> visitedSites.addSite(GlobalPos.of(world.dimension(), pos)));
        context.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(spawned));
        context.primaryDeed()
                .ifPresent(outcome -> outcome.recordYield(totalItemCount(drops)));
        return StepResult.noOp();
    }

    private BlockState createReplacementState(@Nonnull BlockState oreState) {
        boolean isStone = oreState.is(Tags.Blocks.ORES_IN_GROUND_STONE);

        // When the feature is disabled, the block should not regenerate
        if (!ConfigFactory.create(OreRegenConfig.class).enabled()) {
            return isStone ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        }

        return isStone ? BlockRegistry.DORMANT_ORE.get().defaultBlockState() : BlockRegistry.DORMANT_DEEPSLATE_ORE.get().defaultBlockState();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "ore"));
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
    }

    private static int totalItemCount(@Nonnull List<ItemStack> drops) {
        return drops.stream().mapToInt(ItemStack::getCount).sum();
    }

}
