package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.SpeechBubbleState;
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
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyShearableSheepExistsCondition;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundDisplayBubblePacket;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.ClientBoundRemoveBubblePacket;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.DisplayBubbleRequest;
import dev.breezes.settlements.infrastructure.rendering.bubbles.packet.RemoveBubbleRequest;
import dev.breezes.settlements.infrastructure.rendering.bubbles.registry.BubbleType;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Behavior that makes villagers shear nearby sheep
 */
@CustomLog
public class ShearSheepBehaviorV2 extends StateMachineBehavior {

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
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;
    private final AtomicInteger shearCount;

    public ShearSheepBehaviorV2(@Nonnull ShearSheepConfig config) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable());

        this.config = config;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.shear_sheep")
                .iconItemId(ResourceLocation.withDefaultNamespace("shears"))
                .displaySuffix(null)
                .build();
        this.shearCount = new AtomicInteger(0);

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = NearbyShearableSheepExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbyShearableSheepExistsCondition);

        this.initializeStateMachine(this.createControlStep(), ShearStage.END);

    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("ShearSheepBehaviorV2")
                .onStart(context -> {
                    log.behaviorStatus("Creating speech bubble");
                    ISettlementsVillager villager = context.getInitiator();

                    UUID bubbleId = UUID.randomUUID();
                    context.setState(BehaviorStateType.SPEECH_BUBBLE, SpeechBubbleState.of(bubbleId));

                    DisplayBubbleRequest request = DisplayBubbleRequest.builder()
                            .entityId(villager.getNetworkingId())
                            .bubbleType(BubbleType.SHEAR_SHEEP)
                            .bubbleId(bubbleId)
                            .visibilityBlocks(20)
                            // TODO: shorten this once packets are sent regularly
                            .lifetimeTicks(Ticks.seconds(60).getTicksAsInt())
                            .build();

                    // Send the packet
                    // TODO: we should send this packet regularly throughout the behavior (e.g. for players who teleported to nearby)
                    PacketDistributor.sendToPlayersTrackingEntity(villager.getMinecraftEntity(), new ClientBoundDisplayBubblePacket(request));
                    return StepResult.noOp();
                })
                .initialStage(ShearStage.SHEAR_SHEEP)
                .stageStepMap(Map.of(ShearStage.SHEAR_SHEEP, this.createShearSheepStep()))
                .nextStage(ShearStage.END)
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
                    return StepResult.noOp();
                })
                .build();
    }

    private BehaviorStep createShearSheepStep() {
        TimeBasedStep shearStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(context -> {
                    this.shearCount.decrementAndGet();
                    return StepResult.noOp();
                })
                .everyTick(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(Ticks.seconds(0.5), context -> {
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
                        woolItem.setPickUpDelay(Ticks.seconds(3).getTicksAsInt());
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

        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(new NavigateToTargetStep(0.55f, 1))
                .actionStep(shearStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {

        Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
        int limit = config.expertiseShearLimit().get(expertise.getConfigName());
        this.shearCount.set(limit);
        log.behaviorStatus("Villager is '{}' level, maximum shear count is {}", expertise.toString(), limit);

        List<Targetable> targets = this.nearbyShearableSheepExistsCondition.getTargets().stream()
                .map(Targetable::fromEntity)
                .toList();
        context.setState(BehaviorStateType.TARGET, TargetState.of(targets));
    }

    private Optional<Sheep> getTargetSheep(@Nonnull BehaviorContext context) {
        return TargetQueries.firstEntity(context, EntityType.SHEEP, Sheep.class);
    }

}
