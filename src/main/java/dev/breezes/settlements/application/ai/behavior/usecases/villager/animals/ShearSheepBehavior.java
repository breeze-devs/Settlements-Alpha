package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
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
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.SpriteRef;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Behavior that makes villagers shear nearby sheep
 */
@CustomLog
public class ShearSheepBehavior extends VillagerStateMachineBehavior {

    private static final String BUBBLE_OWNER_KEY = "behavior:shear_sheep";
    private static final ClockTicks BUBBLE_TTL = ClockTicks.seconds(30);
    private static final ResourceLocation SHEARS_ITEM_ID = ResourceLocation.withDefaultNamespace("shears");

    private static final Map<DyeColor, ItemLike> WOOL_COLOR_MAP = Map.ofEntries(
            Map.entry(DyeColor.WHITE, Items.WHITE_WOOL),
            Map.entry(DyeColor.ORANGE, Items.ORANGE_WOOL),
            Map.entry(DyeColor.MAGENTA, Items.MAGENTA_WOOL),
            Map.entry(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL),
            Map.entry(DyeColor.YELLOW, Items.YELLOW_WOOL),
            Map.entry(DyeColor.LIME, Items.LIME_WOOL),
            Map.entry(DyeColor.PINK, Items.PINK_WOOL),
            Map.entry(DyeColor.GRAY, Items.GRAY_WOOL),
            Map.entry(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL),
            Map.entry(DyeColor.CYAN, Items.CYAN_WOOL),
            Map.entry(DyeColor.PURPLE, Items.PURPLE_WOOL),
            Map.entry(DyeColor.BLUE, Items.BLUE_WOOL),
            Map.entry(DyeColor.BROWN, Items.BROWN_WOOL),
            Map.entry(DyeColor.GREEN, Items.GREEN_WOOL),
            Map.entry(DyeColor.RED, Items.RED_WOOL),
            Map.entry(DyeColor.BLACK, Items.BLACK_WOOL));

    private enum ShearStage implements StageKey {
        SHEAR_SHEEP,
        PICKUP_WOOL,
        END;
    }

    private final ShearSheepConfig config;

    private final AtomicInteger shearCount;
    private boolean shouldRewardExperience;

    public ShearSheepBehavior(@Nonnull ShearSheepConfig config,
                              @Nonnull HungerConfig hungerConfig,
                              @Nonnull DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.config = config;
        this.shearCount = new AtomicInteger(0);
        this.shouldRewardExperience = false;

        // Create behavior preconditions
        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, Sheep>builder()
                .entityType(Sheep.class)
                .filter((villager, sheep) -> isShearable(sheep))
                .build());
        this.preconditions.add(demandSignalService.requireItem(new ItemMatch.ItemRef(SHEARS_ITEM_ID), 1, 50, this.getClass().getSimpleName()));

        this.initializeStateMachine(this.createControlStep(), ShearStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ShearSheepBehavior")
                .onStart(context -> {
                    log.behaviorStatus("Creating speech bubble");
                    ISettlementsVillager villager = context.getInitiator();

                    BubbleMessage message = BubbleMessage.builder()
                            .priority(0)
                            .ttl(BUBBLE_TTL)
                            .sourceType("behavior")
                            .segments(List.of(
                                    BubbleSegment.Sprite.builder()
                                            .sprite(SpriteRef.SHEARS)
                                            .frameDuration(ClockTicks.seconds(0.5))
                                            .build(),
                                    BubbleSegment.Sprite.builder()
                                            .sprite(SpriteRef.SHEEP)
                                            .frameDuration(ClockTicks.seconds(0.6))
                                            .build()))
                            .build();
                    villager.upsertBubble(BubbleChannel.BEHAVIOR, BUBBLE_OWNER_KEY, message);
                    return StepResult.noOp();
                })
                .initialStage(ShearStage.SHEAR_SHEEP)
                .stageStepMap(Map.of(
                        ShearStage.SHEAR_SHEEP, this.createShearSheepStep(),
                        ShearStage.PICKUP_WOOL, this.createPickupWoolStep()))
                .nextStage(ShearStage.END)
                .onEnd(context -> {
                    log.behaviorStatus("Removing speech bubble");
                    ISettlementsVillager villager = context.getInitiator();
                    villager.removeBubbleByOwner(BubbleChannel.BEHAVIOR, BUBBLE_OWNER_KEY);
                    return StepResult.noOp();
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createShearSheepStep() {
        TimeBasedStep<BaseVillager> shearStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(context -> {
                    this.shearCount.decrementAndGet();
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), context -> {
                    Optional<Sheep> sheepOptional = this.getTargetSheep(context);
                    if (sheepOptional.isEmpty()) {
                        return StepResult.complete();
                    }
                    Sheep sheep = sheepOptional.get();

                    // Shear sheep
                    sheep.setSheared(true);
                    SoundRegistry.SHEAR_SHEEP.playGlobally(Location.fromEntity(sheep, false), SoundSource.NEUTRAL);

                    // Drop wool items
                    int dropCount = RandomUtil.randomInt(1, 3, true);
                    List<ItemEntity> woolItems = new ArrayList<>();
                    for (int i = 0; i < dropCount; i++) {
                        ItemEntity woolItem = sheep.spawnAtLocation(WOOL_COLOR_MAP.get(sheep.getColor()), 1);
                        if (woolItem == null) {
                            continue;
                        }
                        woolItem.setPickUpDelay(ClockTicks.seconds(3).getTicksAsInt());
                        woolItem.setDeltaMovement(woolItem.getDeltaMovement().add(
                                RandomUtil.randomDouble(-0.05F, 0.05F),
                                RandomUtil.randomDouble(0F, 0.05F),
                                RandomUtil.randomDouble(-0.05F, 0.05F)));
                        woolItems.add(woolItem);
                    }
                    context.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(woolItems));
                    context.getState(BehaviorStateType.BEHAVIOR_OUTCOME, BehaviorOutcome.class)
                            .ifPresent(outcome -> outcome.recordYield(woolItems.size()));
                    this.shouldRewardExperience = true;
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return StepResult.transition(ShearStage.PICKUP_WOOL);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(shearStep)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickupWoolStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(PickUpAnimations.PICK_UP_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    context.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(PickUpAnimations.PICK_UP_AT_TICK), context -> {
                    context.getState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.class)
                            .ifPresent(itemState -> itemState.getItems().stream()
                                    .filter(ItemEntity::isAlive)
                                    .forEach(itemEntity -> context.getInitiator().pickUp(itemEntity)));
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    // Determine if there are more sheep to shear; only scan when quota remains
                    if (this.shearCount.get() > 0) {
                        List<Targetable> nearbySheep = this.findShearableSheep(context.getInitiator().getMinecraftEntity()).stream()
                                .map(Targetable::fromEntity)
                                .toList();
                        if (!nearbySheep.isEmpty()) {
                            context.setState(BehaviorStateType.TARGET, TargetState.of(nearbySheep));
                            log.behaviorStatus("Found {} nearby sheep to shear, remaining {}", nearbySheep.size(), this.shearCount.get());
                            return StepResult.transition(ShearStage.SHEAR_SHEEP);
                        }
                    }

                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {

        context.setState(BehaviorStateType.BEHAVIOR_OUTCOME, BehaviorOutcome.forDeed(WorldEventType.SHEEP_SHEARED, "wool"));
        Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
        int limit = config.expertiseShearLimit().get(expertise.getConfigName());
        this.shearCount.set(limit);
        this.shouldRewardExperience = false;
        log.behaviorStatus("Villager is '{}' level, maximum shear count is {}", expertise.toString(), limit);

        if (!villager.getSettlementsInventory().containsOrBypassed(Items.SHEARS, GeneralConfig.bypassInventoryRequirements)) {
            this.requestStop("No shears available");
            return;
        }

        List<Targetable> targets = this.findShearableSheep(villager).stream()
                .map(Targetable::fromEntity)
                .toList();
        context.setState(BehaviorStateType.TARGET, TargetState.of(targets));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.shouldRewardExperience = false;
    }

    private Optional<Sheep> getTargetSheep(@Nonnull BehaviorContext<BaseVillager> context) {
        return TargetQueries.firstEntity(context, EntityType.SHEEP, Sheep.class)
                .filter(ShearSheepBehavior::isShearable);
    }

    private List<Sheep> findShearableSheep(@Nonnull BaseVillager villager) {
        return this.getPerceivedEntities(villager)
                .ofType(Sheep.class, ShearSheepBehavior::isShearable)
                .toList();
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

    private static boolean isShearable(@Nonnull Sheep sheep) {
        return sheep.readyForShearing();
    }

}
