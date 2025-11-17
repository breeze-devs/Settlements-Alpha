package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.bubbles.packet.ClientBoundDisplayBubblePacket;
import dev.breezes.settlements.bubbles.packet.ClientBoundRemoveBubblePacket;
import dev.breezes.settlements.bubbles.packet.DisplayBubbleRequest;
import dev.breezes.settlements.bubbles.packet.RemoveBubbleRequest;
import dev.breezes.settlements.bubbles.registry.BubbleType;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapEntry;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.ISettlementsVillager;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.SimpleStage;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.SpeechBubbleState;
import dev.breezes.settlements.models.behaviors.states.registry.items.ItemState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetableType;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.TimeBasedStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.conditions.NearbyShearableSheepExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.Expertise;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@CustomLog
public class ShearSheepBehaviorV2 extends BaseVillagerBehavior {

    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 60, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 240, min = 1)
    private static int behaviorCooldownMax;

    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to scan for nearby sheep to shear",
            defaultValue = 32, min = 5, max = 128)
    private static int scanRangeHorizontal;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to scan for nearby sheep to shear",
            defaultValue = 12, min = 1, max = 16)
    private static int scanRangeVertical;

    @MapConfig(type = ConfigurationType.BEHAVIOR,
            identifier = "expertise_shear_limit",
            description = "Map of expertise to the maximum number of sheep they can shear",
            deserializer = "StringToInteger",
            defaultValue = {
                    @MapEntry(key = "novice", value = "2"),
                    @MapEntry(key = "apprentice", value = "3"),
                    @MapEntry(key = "journeyman", value = "5"),
                    @MapEntry(key = "expert", value = "7"),
                    @MapEntry(key = "master", value = "10")
            })
    private static Map<String, Integer> expertiseShearLimit;

    private static final Stage SHEAR_SHEEP = new SimpleStage("SHEAR_SHEEP");

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
            Map.entry(DyeColor.BLACK, Items.BLACK_WOOL)
    );

    private final StagedStep controlStep;

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;

    private final AtomicInteger shearCount;

    @Nullable
    private BehaviorContext context;

    public ShearSheepBehaviorV2() {
        super(log, RandomRangeTickable.of(Ticks.of(preconditionCheckCooldownMin), Ticks.of(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.of(behaviorCooldownMin), Ticks.of(behaviorCooldownMax)));

        // Initialize variables
        this.shearCount = new AtomicInteger(0);
        this.context = null;

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = NearbyShearableSheepExistsCondition.builder()
                .rangeHorizontal(scanRangeHorizontal)
                .rangeVertical(scanRangeVertical)
                .build();
        this.preconditions.add(this.nearbyShearableSheepExistsCondition);

        // Create steps
        this.controlStep = StagedStep.builder()
                .name("ShearSheepBehaviorV2")
                .onStart(context -> {
                    log.behaviorStatus("Creating speech bubble");
                    ISettlementsVillager villager = context.getInitiator();
                    log.info("Loc1: {}", Location.fromEntity(villager.getMinecraftEntity(), false).serialize());
                    log.info("Loc21: {}", Location.deserialize("minecraft:overworld,10.03,-60.0,5.34,0.0,323.92"));
                    log.info("Loc31: {}", Location.deserialize("owouwu,10.03,-60.0,5.34,0.0,323.92"));
                    log.info("Loc41: {}", Location.deserialize("minecraft:nether,10,-1,5,0,0"));

                    UUID bubbleId = UUID.randomUUID();
                    context.setState(BehaviorStateType.SPEECH_BUBBLE, SpeechBubbleState.of(bubbleId));

                    DisplayBubbleRequest request = DisplayBubbleRequest.builder()
                            .entityId(villager.getNetworkingId())
                            .bubbleType(BubbleType.SHEAR_SHEEP)
                            .bubbleId(bubbleId)
                            .visibilityBlocks(20)
                            .lifetimeTicks(Ticks.seconds(60).getTicksAsInt()) // TODO: shorten this once packets are sent regularly
                            .build();

                    // Send the packet
                    // TODO: we should send this packet regularly throughout the behavior (e.g. for players who teleported to nearby)
                    PacketDistributor.sendToPlayersTrackingEntity(villager.getMinecraftEntity(), new ClientBoundDisplayBubblePacket(request));
                    return Optional.empty();
                })
                .initialStage(SHEAR_SHEEP)
                .stageStepMap(Map.of(
                        SHEAR_SHEEP, this.createShearSheepStep()
                ))
                .nextStage(ControlStages.STEP_END)
                .onEnd(context -> {
                    log.behaviorStatus("Removing speech bubble");
                    context.getState(BehaviorStateType.SPEECH_BUBBLE, SpeechBubbleState.class)
                            .map(SpeechBubbleState::getBubbleId)
                            .ifPresent(uuid -> {
                                ISettlementsVillager villager = context.getInitiator();
                                RemoveBubbleRequest request = RemoveBubbleRequest.builder()
                                        .entityId(villager.getNetworkingId())
                                        .bubbleId(uuid)
                                        .build();
                                PacketDistributor.sendToPlayersTrackingEntity(villager.getMinecraftEntity(), new ClientBoundRemoveBubblePacket(request));
                            });
                    return Optional.empty();
                })
                .build();
    }

    private BehaviorStep createShearSheepStep() {
        TimeBasedStep shearStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(context -> {
                    this.shearCount.decrementAndGet();
                    return Optional.empty();
                })
                .everyTick(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return Optional.empty();
                })
                .addKeyFrame(Ticks.seconds(0.5), context -> {
                    Optional<Sheep> sheepOptional = this.getTargetSheep(context);
                    if (sheepOptional.isEmpty()) {
                        return Optional.of(ControlStages.STEP_END);
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
                        woolItem.setPickUpDelay(Ticks.seconds(3).getTicksAsInt());
                        woolItem.setDeltaMovement(woolItem.getDeltaMovement().add(
                                RandomUtil.randomDouble(-0.05F, 0.05F),
                                RandomUtil.randomDouble(0F, 0.05F),
                                RandomUtil.randomDouble(-0.05F, 0.05F)
                        ));
                        woolItems.add(woolItem);
                    }
                    context.setState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.of(woolItems));
                    return Optional.empty();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();

                    // Pick up wool items
                    context.getState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.class)
                            .ifPresent(itemState -> itemState.getItems()
                                    .forEach(itemEntity -> context.getInitiator().getMinecraftEntity().take(itemEntity, 999)));

                    // Determine if there are more sheep to shear
                    if (this.shearCount.get() > 0 && this.nearbyShearableSheepExistsCondition.test(context.getInitiator().getMinecraftEntity())) {
                        List<Targetable> nearbySheep = this.nearbyShearableSheepExistsCondition.getTargets().stream()
                                .map(Targetable::fromEntity)
                                .toList();
                        context.setState(BehaviorStateType.TARGET, TargetState.of(nearbySheep));

                        log.behaviorStatus("Found {} nearby sheep to shear, remaining {}", nearbySheep.size(), this.shearCount.get());
                        return Optional.of(SHEAR_SHEEP);
                    }

                    return Optional.of(ControlStages.STEP_END);
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep(0.55f, 1))
                .actionStep(shearStep)
                .build();
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.context = new BehaviorContext(entity);

        Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
        int limit = expertiseShearLimit.get(expertise.getConfigName());
        this.shearCount.set(limit);
        log.behaviorStatus("Villager is '{}' level, maximum shear count is {}", expertise.toString(), limit);

        List<Targetable> targets = this.nearbyShearableSheepExistsCondition.getTargets().stream()
                .map(Targetable::fromEntity)
                .toList();
        this.context.setState(BehaviorStateType.TARGET, TargetState.of(targets));
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        if (controlStep.getCurrentStage() == ControlStages.STEP_END) {
            throw new StopBehaviorException("Behavior has ended");
        }

        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }

        this.controlStep.tick(this.context);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.context = null;
        this.controlStep.reset();
    }

    private Optional<Sheep> getTargetSheep(@Nonnull BehaviorContext context) {
        ICondition<Targetable> isSheepPredicate = (target) -> target != null
                && target.getType() == TargetableType.ENTITY
                && target.getAsEntity().getType() == EntityType.SHEEP;

        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .map(targetState -> targetState.match(isSheepPredicate))
                .flatMap(Stream::findFirst)
                .map(Targetable::getAsEntity)
                .map(Sheep.class::cast);
    }

}
