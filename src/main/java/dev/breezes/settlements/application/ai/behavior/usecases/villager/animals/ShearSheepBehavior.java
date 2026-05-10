package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
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
import dev.breezes.settlements.domain.ai.conditions.NearbyShearableSheepExistsCondition;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
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
    private static final String DISPLAY_NAME_KEY = "ui.settlements.behavior.behavior.shear_sheep";
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
        END;
    }

    private final ShearSheepConfig config;
    private final DemandSignalService demandSignalService;

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;
    private final AtomicInteger shearCount;

    public ShearSheepBehavior(@Nonnull ShearSheepConfig config,
                              @Nonnull HungerConfig hungerConfig,
                              @Nonnull DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.config = config;
        this.demandSignalService = demandSignalService;
        this.shearCount = new AtomicInteger(0);

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = NearbyShearableSheepExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyShearableSheepExistsCondition);
        this.preconditions.add(villager -> villager != null && ensureShearsAvailable(villager));

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
                .stageStepMap(Map.of(ShearStage.SHEAR_SHEEP, this.createShearSheepStep()))
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
                    return StepResult.noOp();
                })
                .everyTick(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.seconds(0.5), context -> {
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
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();

                    // Pick up wool items
                    context.getState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.class)
                            .ifPresent(itemState -> itemState.getItems()
                                    .forEach(itemEntity -> context.getInitiator().pickUp(itemEntity)));

                    // Determine if there are more sheep to shear
                    if (this.shearCount.get() > 0 && this.nearbyShearableSheepExistsCondition.test(context.getInitiator().getMinecraftEntity())) {
                        List<Targetable> nearbySheep = this.nearbyShearableSheepExistsCondition.getTargets().stream()
                                .map(Targetable::fromEntity)
                                .toList();
                        context.setState(BehaviorStateType.TARGET, TargetState.of(nearbySheep));

                        log.behaviorStatus("Found {} nearby sheep to shear, remaining {}", nearbySheep.size(), this.shearCount.get());
                        return StepResult.transition(ShearStage.SHEAR_SHEEP);
                    }

                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep<>(0.55f, 1))
                .actionStep(shearStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {

        Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
        int limit = config.expertiseShearLimit().get(expertise.getConfigName());
        this.shearCount.set(limit);
        log.behaviorStatus("Villager is '{}' level, maximum shear count is {}", expertise.toString(), limit);

        if (!this.ensureShearsAvailable(entity)) {
            this.requestStop("No shears available");
            return;
        }

        List<Targetable> targets = this.nearbyShearableSheepExistsCondition.getTargets().stream()
                .map(Targetable::fromEntity)
                .toList();
        context.setState(BehaviorStateType.TARGET, TargetState.of(targets));
    }

    private boolean ensureShearsAvailable(@Nonnull BaseVillager villager) {
        VillagerInventory inventory = villager.getSettlementsInventory();
        if (inventory.containsItem(Items.SHEARS)) {
            return true;
        }

        log.behaviorStatus("Emitting demand signal for {}", SHEARS_ITEM_ID);
        this.demandSignalService.emit(villager, new ItemMatch.ItemRef(SHEARS_ITEM_ID), 1, 50, null,
                DISPLAY_NAME_KEY, villager.level().getGameTime());
        return false;
    }

    private Optional<Sheep> getTargetSheep(@Nonnull BehaviorContext<BaseVillager> context) {
        return TargetQueries.firstEntity(context, EntityType.SHEEP, Sheep.class);
    }

}
