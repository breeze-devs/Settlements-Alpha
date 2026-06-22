package dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look.LookState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyWaterExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.FishingAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.fishing.FishCatchEntry;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.VillagerFishingHook;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class FishingBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation FISHING_ROD_ID = ResourceLocation.withDefaultNamespace("fishing_rod");

    private static final double NAVIGATION_CLOSE_ENOUGH_DISTANCE = 5.0;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 3;
    private static final int HOOK_MAX_LIFETIME_BUFFER_SECONDS = 15;
    private static final double COLLECT_DISTANCE_SQUARED = 9.0;
    private static final int MAX_CAST_RETRIES = 5;
    private static final int FIGHT_FISH_CYCLES = 2;
    private static final double DEFAULT_CATCH_RATE = 0.30;

    private enum FishingStage implements StageKey {
        NAVIGATE_TO_WATER,
        CAST_LINE,
        WAIT_FOR_BITE,
        FIGHT_FISH,
        REEL_IN,
        COLLECT_FISH,
        END;
    }

    private final FishingConfig config;

    private final NearbyWaterExistsCondition<BaseVillager> nearbyWaterExistsCondition;

    // Runtime state
    @Nullable
    private BlockPos waterTarget;
    @Nullable
    private VillagerFishingHook activeHook;
    @Nullable
    private Entity fishedEntity;
    @Nullable
    private FishCatchEntry caughtFishEntry;
    @Nullable
    private ItemStack caughtItem;
    @Nullable
    private String caughtFishSize;
    private int castRetryCount;

    public FishingBehavior(@Nonnull FishingConfig config,
                           @Nonnull HungerConfig hungerConfig,
                           @Nonnull DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.config = config;

        this.nearbyWaterExistsCondition = NearbyWaterExistsCondition.<BaseVillager>builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .completionRange(NAVIGATION_COMPLETION_DISTANCE)
                .build();
        this.preconditions.add(this.nearbyWaterExistsCondition);
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(FISHING_ROD_ID), 1, 50, this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), FishingStage.END);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.waterTarget = this.nearbyWaterExistsCondition.getWaterTarget().orElse(null);
        if (this.waterTarget == null) {
            log.behaviorWarn("Fishing behavior started without a water target; stopping");
            this.requestStop("Fishing behavior started without a water target");
            return;
        }

        Optional<BlockPos> shorePosition = this.nearbyWaterExistsCondition.getShorePosition();
        if (shorePosition.isEmpty()) {
            log.behaviorWarn("Fishing behavior started without a shore position for water target {}; stopping",
                    this.waterTarget);
            this.requestStop("Fishing behavior started without a shore position");
            return;
        }

        PhysicalBlock shoreBlock = PhysicalBlock.of(
                Location.of(shorePosition.get(), villager.level()),
                villager.level().getBlockState(shorePosition.get()));
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(shoreBlock)));

        // Gaze at the water rather than the shore block
        context.setState(BehaviorStateType.LOOK_TARGET, LookState.ofLocation(Location.of(this.waterTarget, villager.level())));

        log.behaviorStatus("Starting fishing behavior at shore position {} and water position {}",
                shorePosition, this.waterTarget);
        this.castRetryCount = 0;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.activeHook != null && !this.activeHook.isRemoved()) {
            this.activeHook.discard();
        }
        if (this.fishedEntity != null && !this.fishedEntity.isRemoved()) {
            this.fishedEntity.discard();
        }

        villager.setMotion(AnimationArchetype.IDLE);
        villager.clearHeldItem();
        villager.setBobberDeployed(false);

        this.waterTarget = null;
        this.activeHook = null;
        this.fishedEntity = null;
        this.caughtFishEntry = null;
        this.caughtItem = null;
        this.caughtFishSize = null;
        this.castRetryCount = 0;
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.activeHook == null) {
            return true;
        }
        if (!this.activeHook.isRemoved()) {
            return true;
        }
        return this.activeHook.isMissedWater() && this.castRetryCount < MAX_CAST_RETRIES;
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("FishingBehavior")
                .initialStage(FishingStage.NAVIGATE_TO_WATER)
                .stageStepMap(Map.of(
                        FishingStage.NAVIGATE_TO_WATER, this.createNavigateStep(),
                        FishingStage.CAST_LINE, this.createCastLineStep(),
                        FishingStage.WAIT_FOR_BITE, this.createWaitForBiteStep(),
                        FishingStage.FIGHT_FISH, this.createFightFishStep(),
                        FishingStage.REEL_IN, this.createReelInStep(),
                        FishingStage.COLLECT_FISH, this.createCollectFishStep()
                ))
                .nextStage(FishingStage.END)
                .build();
    }

    /**
     * Walk to the shore position, wait half a second, then cast.
     */
    private BehaviorStep<BaseVillager> createNavigateStep() {
        TimeBasedStep<BaseVillager> arrivedStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(0.5).asTickable())
                .onEnd(context -> StepResult.transition(FishingStage.CAST_LINE))
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(NAVIGATION_CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(arrivedStep)
                .build();
    }

    /**
     * Hold rod, spawn hook aimed at water, play cast sound.
     * Placeholder for animation (no-op); transitions to WAIT_FOR_BITE.
     */
    private BehaviorStep<BaseVillager> createCastLineStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(FishingAnimations.CAST_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    ISettlementsVillager villager = context.getInitiator();
                    context.getInitiator().triggerMotion(AnimationArchetype.CAST);
                    villager.setHeldItem(ItemRegistry.VILLAGER_FISHING_ROD.get().getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(FishingAnimations.CAST_IMPACT_TICK), context -> {
                    if (this.waterTarget == null) {
                        log.behaviorWarn("No water target set during cast — stopping");
                        return StepResult.fail("NO_WATER_TARGET");
                    }
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    return this.castHook(villager);
                })
                .onEnd(context -> StepResult.transition(FishingStage.WAIT_FOR_BITE))
                .build();
    }

    /**
     * Wait for the hook's catchingFish() logic to signal a bite.
     * Polls hasBitten() every 5 ticks. Times out after maxWaitTimeSeconds.
     */
    private BehaviorStep<BaseVillager> createWaitForBiteStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(config.maxWaitTimeSeconds()).asTickable())
                .onStart(context -> {
                    // Sustained idle-while-fishing loop holds the rod out until a bite (or a retry/timeout)
                    context.getInitiator().getMinecraftEntity().setMotion(AnimationArchetype.FISHING_WAIT);
                    return StepResult.noOp();
                })
                .addPeriodicStep(5, context -> {
                    if (this.activeHook != null && this.activeHook.isRemoved() && this.activeHook.isMissedWater()) {
                        if (this.castRetryCount < MAX_CAST_RETRIES) {
                            this.castRetryCount++;
                            log.behaviorStatus("Hook missed water (attempt {}/{}), retrying",
                                    this.castRetryCount, MAX_CAST_RETRIES);
                            this.activeHook = null;
                            context.getInitiator().getMinecraftEntity().setBobberDeployed(false);
                            return StepResult.transition(FishingStage.CAST_LINE);
                        }
                        log.behaviorWarn("Hook missed water, no retries left");
                        context.getInitiator().getMinecraftEntity().setBobberDeployed(false);
                        return StepResult.complete();
                    }

                    if (this.activeHook != null && this.activeHook.hasBitten()) {
                        SoundRegistry.FISHING_SPLASH.playGlobally(
                                Location.fromEntity(this.activeHook, false),
                                SoundSource.NEUTRAL);
                        log.behaviorStatus("Fish has bitten the hook!");
                        return StepResult.transition(FishingStage.FIGHT_FISH);
                    }

                    return StepResult.noOp();
                })
                .addPeriodicStep(ClockTicks.of(20).getTicksAsInt(), context -> {
                    context.getInitiator().setHeldItem(ItemRegistry.VILLAGER_FISHING_ROD.get().getDefaultInstance());
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    // No fish caught within the specified time
                    if (this.activeHook != null && !this.activeHook.isRemoved()) {
                        this.activeHook.discard();
                        this.activeHook = null;
                    }
                    context.getInitiator().getMinecraftEntity().setBobberDeployed(false);
                    return StepResult.complete();
                })
                .build();
    }

    /**
     * Celebrate the bite and keep the villager visually working the line before the final yank.
     */
    private BehaviorStep<BaseVillager> createFightFishStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(FishingAnimations.FIGHT_FISH_DURATION_TICKS * FIGHT_FISH_CYCLES).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    Location villagerHead = Location.fromEntity(villager, true);
                    villagerHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 8, 0.3, 0.2, 0.3, 0.01);
                    villagerHead.playSound(SoundEvents.VILLAGER_YES, 0.8f, 1.0f, SoundSource.NEUTRAL);

                    villager.setMotion(AnimationArchetype.REEL_OUT);
                    return StepResult.noOp();
                })
                .onEnd(context -> StepResult.transition(FishingStage.REEL_IN))
                .build();
    }

    /**
     * Reel hook, spawn flying visual entity, clear rod.
     */
    private BehaviorStep<BaseVillager> createReelInStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(FishingAnimations.REEL_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    villager.triggerMotion(AnimationArchetype.REEL_IN);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(FishingAnimations.REEL_IMPACT_TICK), context -> {
                    if (this.activeHook == null || this.activeHook.isRemoved()) {
                        context.getInitiator().getMinecraftEntity().setBobberDeployed(false);
                        return StepResult.noOp();
                    }

                    Location hookLocation = Location.fromEntity(this.activeHook, false);
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    if (this.isEmptyCatch(villager)) {
                        this.activeHook.discard();
                        this.fishedEntity = null;
                        this.caughtFishEntry = null;
                        this.caughtItem = null;
                        this.caughtFishSize = null;
                        this.showEmptyCatchSadness(villager);
                    } else {
                        this.fishedEntity = this.activeHook.reelIn().orElse(null);
                        this.caughtFishEntry = this.activeHook.getSelectedCatchEntry();
                        this.caughtItem = this.fishToItem(this.caughtFishEntry).orElse(null);
                        this.caughtFishSize = this.activeHook.getSelectedCatchSize();
                    }
                    this.activeHook = null;
                    context.getInitiator().getMinecraftEntity().setBobberDeployed(false);

                    SoundRegistry.FISHING_SPLASH.playGlobally(hookLocation, SoundSource.NEUTRAL);
                    SoundRegistry.FISHING_REEL.playGlobally(Location.fromEntity(villager, false), SoundSource.NEUTRAL);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().getMinecraftEntity().setBobberDeployed(false);
                    context.getInitiator().clearHeldItem();

                    // Add absorption to prevent too much pufferfish damage
                    context.getInitiator().addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ClockTicks.seconds(30).getTicksAsInt(), 9, true, true));
                    return StepResult.transition(FishingStage.COLLECT_FISH);
                })
                .build();
    }

    /**
     * Wait for the visual fish entity to arrive near the villager, then
     * store the corresponding fish item in the villager's inventory.
     */
    private BehaviorStep<BaseVillager> createCollectFishStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(3).asTickable())
                .addPeriodicStep(5, context -> {
                    if (this.fishedEntity == null || this.fishedEntity.isRemoved()) {
                        return collectAndComplete(context);
                    }
                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    if (this.fishedEntity.distanceToSqr(villager) <= COLLECT_DISTANCE_SQUARED) {
                        Location.fromEntity(this.fishedEntity, false).displayParticles(ParticleTypes.CLOUD,
                                3, 0.1, 0.1, 0.1, 0.1);
                        this.fishedEntity.discard();
                        return collectAndComplete(context);
                    }
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    if (this.fishedEntity != null && !this.fishedEntity.isRemoved()) {
                        Location.fromEntity(this.fishedEntity, false).displayParticles(ParticleTypes.CLOUD,
                                3, 0.1, 0.1, 0.1, 0.1);
                        this.fishedEntity.discard();
                    }
                    return collectAndComplete(context);
                })
                .build();
    }

    private StepResult castHook(@Nonnull BaseVillager villager) {
        if (this.waterTarget == null) {
            log.behaviorWarn("No water target set during cast, stopping behavior");
            return StepResult.fail("NO_WATER_TARGET");
        }

        int baseMinBiteTicks = ClockTicks.seconds(this.config.minWaitTimeSeconds()).getTicksAsInt();
        int baseMaxBiteTicks = ClockTicks.seconds(this.config.maxWaitTimeSeconds()).getTicksAsInt();

        Expertise expertise = villager.getExpertise();
        double fishingTimeShortenScale = this.config.expertiseWaitTimeScale().getOrDefault(expertise.getConfigName(), 1.0);

        // Calculate bite & removal time
        int minBiteTicks = Math.max(1, (int) Math.round(baseMinBiteTicks * fishingTimeShortenScale));
        int maxBiteTicks = Math.max(minBiteTicks, (int) Math.round(baseMaxBiteTicks * fishingTimeShortenScale));
        int biteTimeTicks = RandomUtil.randomInt(minBiteTicks, maxBiteTicks, true);
        int maxLifetimeTicks = baseMaxBiteTicks + ClockTicks.seconds(HOOK_MAX_LIFETIME_BUFFER_SECONDS).getTicksAsInt();

        // Calculate hook velocity
        double retryProgress = (this.castRetryCount - 1) / (double) (MAX_CAST_RETRIES - 1);
        double extraHorizontalRandomness = this.castRetryCount <= 0
                ? 0.0
                : 0.05 + 0.2 * retryProgress;
        double extraVerticalVelocity = this.castRetryCount * 0.05;

        // Cast the hook
        this.activeHook = new VillagerFishingHook(villager, this.waterTarget, biteTimeTicks, maxLifetimeTicks,
                extraHorizontalRandomness, extraVerticalVelocity);
        this.activeHook.castIntoWorld();
        villager.setBobberDeployed(true);
        SoundRegistry.FISHING_CAST.playGlobally(Location.fromEntity(villager, true), SoundSource.NEUTRAL);

        log.behaviorStatus("Casted fishing hook, fish will appear in {} ticks", biteTimeTicks);
        return StepResult.noOp();
    }

    private boolean isEmptyCatch(@Nonnull BaseVillager villager) {
        Expertise expertise = villager.getExpertise();
        double catchRate = this.config.expertiseCatchRate().getOrDefault(expertise.getConfigName(), DEFAULT_CATCH_RATE);
        return !RandomUtil.chance(RandomUtil.clamp(catchRate, 0.0, 1.0));
    }

    private void showEmptyCatchSadness(@Nonnull BaseVillager villager) {
        Location villagerHead = Location.fromEntity(villager, true);
        villagerHead.displayParticles(ParticleTypes.ANGRY_VILLAGER, 6, 0.3, 0.2, 0.3, 0.01);
        villagerHead.playSound(SoundEvents.VILLAGER_NO, 0.8f, 1.0f, SoundSource.NEUTRAL);
        log.behaviorStatus("Villager reeled in nothing while fishing");
    }

    private StepResult collectAndComplete(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.caughtItem != null && !this.caughtItem.isEmpty()) {
            VillagerInventory inventory = context.getInitiator().getSettlementsInventory();
            inventory.add(this.caughtItem);
            this.rewardExperience(context.getInitiator().getMinecraftEntity());
            SoundRegistry.ITEM_POP_IN.playGlobally(
                    Location.fromEntity(context.getInitiator().getMinecraftEntity(), false),
                    SoundSource.NEUTRAL);
            log.behaviorStatus("Collected fish: {}", this.caughtItem);

            BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.FISH_CAUGHT, null);
            outcome.markSucceeded();
            outcome.recordDeedDetail(this.describeCaughtFish());
            context.declarePrimaryDeed(outcome);
        }
        this.caughtItem = null;
        this.fishedEntity = null;
        this.caughtFishEntry = null;
        this.caughtFishSize = null;
        return StepResult.complete();
    }

    private String describeCaughtFish() {
        String itemId = this.caughtItem == null ? "fish" : this.caughtItem.getItem().toString();
        if (this.caughtFishSize == null || this.caughtFishSize.isBlank()) {
            return itemId;
        }
        return this.caughtFishSize + " " + itemId;
    }

    /**
     * Resolves a caught item from the selected datapack entry.
     */
    private Optional<ItemStack> fishToItem(@Nullable FishCatchEntry catchEntry) {
        if (catchEntry == null) {
            return Optional.empty();
        }

        ResourceLocation itemId = ResourceLocation.tryParse(catchEntry.getItemId());
        if (itemId == null) {
            log.warn("Invalid fish catch item id '{}'", catchEntry.getItemId());
            return Optional.empty();
        }

        return BuiltInRegistries.ITEM.getOptional(itemId)
                .filter(item -> item != Items.AIR)
                .map(ItemStack::new);
    }

}
