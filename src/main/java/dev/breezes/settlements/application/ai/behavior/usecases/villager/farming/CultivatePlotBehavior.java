package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks.VisitedBlockSitesState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
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
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.conditions.KnownBlockSitesPrecondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.TillAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.farming.CultivationCellCategory;
import dev.breezes.settlements.domain.farming.CultivationCellResult;
import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import dev.breezes.settlements.domain.farming.CultivationZoneCategorizer;
import dev.breezes.settlements.domain.tags.SettlementsBlockTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.BlockMatchers;
import dev.breezes.settlements.domain.world.blocks.BlockMemorySiteConfirmer;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.blocks.totem.TotemOfCultivationBlockEntity;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class CultivatePlotBehavior extends VillagerStateMachineBehavior {

    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final ResourceLocation IRON_HOE_ID = ResourceLocation.withDefaultNamespace("iron_hoe");

    // Under inventory bypass with no filter set, we fall back to wheat
    private static final ResourceLocation CANONICAL_CROP = ResourceLocation.withDefaultNamespace("wheat");

    private enum Stage implements StageKey {
        PICK_CELL, APPROACH_CELL, CULTIVATE, LOOP, AWARD, END
    }

    private final CultivatePlotConfig config;
    private final BlockMemoryTargetResolver targetResolver;
    private final CultivationCropRegistry cropRegistry;
    private final BlockScanBox confirmBox;
    private final int maxConfirms;

    @Nullable
    private BlockPos selectedTotemPos;
    @Nullable
    private BlockPos currentCellPos;
    private final Deque<CultivationCellResult> pendingCells = new ArrayDeque<>();

    public CultivatePlotBehavior(@Nonnull CultivatePlotConfig config,
                                 @Nonnull HungerConfig hungerConfig,
                                 @Nonnull BlockMemoryTargetResolver targetResolver,
                                 @Nonnull DemandSignalService demandSignalService,
                                 @Nonnull CultivationCropRegistry cropRegistry) {
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                hungerConfig);
        this.config = config;
        this.targetResolver = targetResolver;
        this.cropRegistry = cropRegistry;
        this.confirmBox = BlockScanBox.confirm();
        this.maxConfirms = BlockMemorySiteConfirmer.DEFAULT_MAX_CONFIRMS;

        List<ItemMatch> allSeedMatches = cropRegistry.all().stream()
                .map(def -> (ItemMatch) new ItemMatch.ItemRef(def.seedItem()))
                .toList();

        this.preconditions.add(KnownBlockSitesPrecondition.builder()
                .memoryType(MemoryTypeRegistry.CULTIVATION_TOTEM_SITES)
                .matcher(BlockMatchers.CULTIVATION_TOTEM_NEEDS_WORK)
                .confirmBox(this.confirmBox)
                .maxSitesToConfirm(this.maxConfirms)
                .completionRange(1)
                .description("Known cultivation totem sites needing work")
                .build());
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(IRON_HOE_ID), 1, 50,
                this.getClass().getSimpleName()));

        // requireAny throws on an empty match list, which an empty/misconfigured crop datapack would
        // produce. Skip the seed gate in that case — the behavior bails gracefully once it finds no
        // crop to plant, rather than failing every villager's behavior construction.
        if (!allSeedMatches.isEmpty()) {
            this.preconditions.add(demandSignalService.requireAny(allSeedMatches, 1, 50,
                    this.getClass().getSimpleName()));
        }

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_CELL, this.createPickCellStep());
        stageMap.put(Stage.APPROACH_CELL, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(1.5)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(1)
                        .unreachableTransition(Stage.PICK_CELL)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtCell")
                        .action(ctx -> this.onArrivedAtCell())
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(Stage.PICK_CELL)
                .build());
        stageMap.put(Stage.CULTIVATE, this.createCultivateStep());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("CultivatePlotLoopBack")
                .loopBackTo(Stage.PICK_CELL)
                .completionTransition(Stage.AWARD)
                .maxIterationsResolver(ctx -> 16 * ctx.getInitiator().getExpertise().getLevel())
                .build());
        stageMap.put(Stage.AWARD, AwardExperienceStep.builder()
                .name("AwardCultivatePlotXp")
                .experienceAmount(this.config.experienceReward())
                .nextStage(Stage.END)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("CultivatePlotBehavior")
                .initialStage(Stage.PICK_CELL)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickCellStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickNextCultivationCell")
                .action(ctx -> {
                    if (this.selectedTotemPos == null) {
                        log.behaviorStatus("No totem selected, ending behavior");
                        return StepResult.transition(Stage.AWARD);
                    }

                    BaseVillager villager = ctx.getInitiator();
                    Level level = villager.level();

                    // Refill the queue on first call or when it's drained
                    if (this.pendingCells.isEmpty()) {
                        BlockEntity be = level.getBlockEntity(this.selectedTotemPos);
                        if (!(be instanceof TotemOfCultivationBlockEntity totem) || !totem.isValid()) {
                            log.behaviorStatus("Totem at {} is no longer valid", this.selectedTotemPos);
                            return StepResult.transition(Stage.AWARD);
                        }

                        List<CultivationCellResult> cells = CultivationZoneCategorizer.categorize(
                                totem.streamZoneCells(this.selectedTotemPos), level, totem.getCropFilter());
                        cells.stream()
                                .filter(r -> CultivationZoneCategorizer.isActionable(r.category()))
                                .forEach(this.pendingCells::add);

                        if (this.pendingCells.isEmpty()) {
                            log.behaviorStatus("Totem zone has no actionable cells, ending behavior");
                            return StepResult.transition(Stage.AWARD);
                        }
                    }

                    BlockEntity blockEntity = level.getBlockEntity(this.selectedTotemPos);
                    if (!(blockEntity instanceof TotemOfCultivationBlockEntity totem)) {
                        return StepResult.transition(Stage.AWARD);
                    }

                    // Find the next cell we can actually handle (seed check for NEEDS_REPLANT / NEEDS_PLANT)
                    while (!this.pendingCells.isEmpty()) {
                        CultivationCellResult candidate = this.pendingCells.poll();

                        // Live re-verify the cell hasn't changed since we built the queue
                        CultivationCellCategory liveCategory = CultivationZoneCategorizer.categorizeCell(
                                level, candidate.cellPos(), totem.getCropFilter());
                        if (!CultivationZoneCategorizer.isActionable(liveCategory)) {
                            continue;
                        }

                        // Stop if we have nothing to plant with. This single check also gates the
                        // NEEDS_REPLANT path: under a filter it resolves to the filter crop only when
                        // the villager carries that seed (or bypass is on), so a scythe is never
                        // started without the means to replant.
                        if (resolveCropToPlant(villager, totem).isEmpty()) {
                            log.behaviorStatus("Villager has no matching seeds, ending behavior");
                            return StepResult.transition(Stage.AWARD);
                        }

                        this.currentCellPos = candidate.cellPos();

                        // Point the navigation TARGET at the canopy block above the cell so the
                        // villager walks to ground level at that position
                        BlockPos approachPos = candidate.cellPos().above();
                        ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                                PhysicalBlock.of(Location.of(approachPos, level), level.getBlockState(approachPos)))));
                        return StepResult.transition(Stage.APPROACH_CELL);
                    }

                    log.behaviorStatus("No more actionable cells, ending behavior");
                    return StepResult.transition(Stage.AWARD);
                })
                .build();
    }

    private StepResult onArrivedAtCell() {
        if (this.currentCellPos == null) {
            return StepResult.transition(Stage.PICK_CELL);
        }
        return StepResult.transition(Stage.CULTIVATE);
    }

    /**
     * Tills, clears, and plants a single cell in one motion.
     * <p>
     * Earlier this was three separate passes (till / scythe / plant), but to the player the villager
     * just swings a hoe once. On the animation's impact frame we play the till sound, clear whatever
     * sits above the cell, ensure the ground is farmland, and seed the resolved crop.
     */
    private BehaviorStep<BaseVillager> createCultivateStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("CultivateCell")
                .withTickable(ClockTicks.of(TillAnimations.TILL_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.IRON_HOE.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.TILL);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(TillAnimations.TILL_IMPACT_TICK), ctx -> {
                    if (this.currentCellPos == null || this.selectedTotemPos == null) {
                        return StepResult.noOp();
                    }
                    BaseVillager villager = ctx.getInitiator();
                    Level level = villager.level();
                    if (!(level instanceof ServerLevel)) {
                        return StepResult.noOp();
                    }
                    BlockEntity be = level.getBlockEntity(this.selectedTotemPos);
                    if (!(be instanceof TotemOfCultivationBlockEntity totem)) {
                        return StepResult.noOp();
                    }

                    BlockPos cellPos = this.currentCellPos;
                    BlockPos canopyPos = cellPos.above();

                    Location tillLocation = Location.of(cellPos, level).center(true).add(0, 0.5, 0, true);
                    tillLocation.playSound(SoundEvents.HOE_TILL, 0.8f, 1.0f, SoundSource.BLOCKS);

                    // Clear whatever occupies the canopy
                    BlockState canopy = level.getBlockState(canopyPos);
                    if (!canopy.isAir()) {
                        ParticleRegistry.harvestBlock(Location.of(canopyPos, level).center(true), canopy);
                        level.destroyBlock(canopyPos, true);
                    }

                    // The crop can only take root on farmland; convert tillable ground, but bail if it is
                    // neither farmland nor tillable (the world changed under us since the cell was picked)
                    BlockState ground = level.getBlockState(cellPos);
                    boolean farmland = ground.is(Blocks.FARMLAND);
                    if (!farmland && ground.is(SettlementsBlockTags.TILLABLE)) {
                        ParticleRegistry.harvestBlock(tillLocation, ground);
                        level.setBlockAndUpdate(cellPos, Blocks.FARMLAND.defaultBlockState());
                        ctx.primaryDeed().ifPresent(outcome -> outcome.recordDeedDetail("tilled"));
                        farmland = true;
                    }
                    if (!farmland) {
                        return StepResult.noOp();
                    }

                    // Seed the resolved crop on the fresh farmland, consuming a seed (bypass-aware)
                    Optional<CultivationCropDefinition> cropDef = resolveCropToPlant(villager, totem);
                    if (cropDef.isEmpty()) {
                        return StepResult.noOp();
                    }
                    CultivationCropDefinition def = cropDef.get();
                    Block cropBlock = BuiltInRegistries.BLOCK.get(def.cropBlock());
                    if (!(cropBlock instanceof CropBlock crop)) {
                        return StepResult.noOp();
                    }
                    Item seedItem = BuiltInRegistries.ITEM.get(def.seedItem());
                    boolean consumed = villager.getSettlementsInventory().consumeIfRequired(
                            seedItem, 1, GeneralConfig.bypassInventoryRequirements);
                    if (!consumed) {
                        log.behaviorStatus("Could not consume seed, skipping plant");
                        return StepResult.noOp();
                    }

                    level.setBlockAndUpdate(canopyPos, crop.getStateForAge(0));
                    ctx.primaryDeed().ifPresent(outcome -> outcome.recordYield(1));
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    ctx.getInitiator().setMotion(AnimationArchetype.IDLE);
                    return StepResult.transition(Stage.LOOP);
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.FARMLAND_CULTIVATED, "farmland blocks"));
        context.setState(BehaviorStateType.VISITED_BLOCK_SITES, VisitedBlockSitesState.empty());

        // Resolve which totem to work on before the state machine starts
        boolean resolved = this.targetResolver.resolveBlockTarget(context,
                MemoryTypeRegistry.CULTIVATION_TOTEM_SITES,
                BlockMatchers.CULTIVATION_TOTEM_NEEDS_WORK,
                this.confirmBox,
                this.maxConfirms,
                1);
        if (!resolved) {
            this.requestStop("No valid totem needing cultivation found at behavior start");
            return;
        }

        Optional<BlockPos> totemPos = TargetQueries.firstBlockPos(context);
        if (totemPos.isEmpty()) {
            this.requestStop("Could not read totem position from target state");
            return;
        }

        this.selectedTotemPos = totemPos.get();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.selectedTotemPos = null;
        this.currentCellPos = null;
        this.pendingCells.clear();
    }

    /**
     * Resolves which crop the villager should plant on the given totem, or empty if it cannot plant.
     * <p>
     * Filter set: the filter crop, gated on the villager carrying that seed (bypass lifts the gate).
     * No filter: the first registered seed the villager carries; under bypass there is no carried
     * seed to imply a crop, so we default to wheat ({@link #CANONICAL_CROP}).
     * The returned definition supplies both the crop block to place and the seed item to consume.
     */
    private Optional<CultivationCropDefinition> resolveCropToPlant(@Nonnull BaseVillager villager,
                                                                   @Nonnull TotemOfCultivationBlockEntity totem) {
        boolean bypass = GeneralConfig.bypassInventoryRequirements;
        ResourceLocation filterCrop = totem.getCropFilter();

        if (filterCrop != null) {
            Optional<CultivationCropDefinition> def = this.cropRegistry.resolveByCropBlock(filterCrop);
            if (def.isEmpty()) {
                return Optional.empty();
            }
            if (bypass || villagerHasSeed(villager, def.get().seedItem())) {
                return def;
            }
            return Optional.empty();
        }

        if (bypass) {
            return defaultBypassCrop();
        }
        for (CultivationCropDefinition def : this.cropRegistry.all()) {
            if (villagerHasSeed(villager, def.seedItem())) {
                return Optional.of(def);
            }
        }
        return Optional.empty();
    }

    /**
     * The crop planted under inventory bypass when no filter narrows the choice — wheat if the
     * datapack defines it, otherwise any configured crop so bypass planting still functions.
     */
    private Optional<CultivationCropDefinition> defaultBypassCrop() {
        Optional<CultivationCropDefinition> wheat = this.cropRegistry.resolveByCropBlock(CANONICAL_CROP);
        if (wheat.isPresent()) {
            return wheat;
        }
        return this.cropRegistry.all().stream().findFirst();
    }

    private boolean villagerHasSeed(@Nonnull BaseVillager villager, @Nonnull ResourceLocation seedId) {
        return villager.getSettlementsInventory().findFirst(stack ->
                !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(seedId)).isPresent();
    }

}
