package dev.breezes.settlements.models.behaviors.steps.gg.wiggle;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.BaseVillagerBehavior;
import dev.breezes.settlements.models.behaviors.StopBehaviorException;
import dev.breezes.settlements.models.behaviors.stages.ControlStages;
import dev.breezes.settlements.models.behaviors.stages.SimpleStage;
import dev.breezes.settlements.models.behaviors.stages.Stage;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetableType;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.TimeBasedStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.conditions.ICondition;
import dev.breezes.settlements.models.conditions.IEntityCondition;
import dev.breezes.settlements.models.conditions.NearbyItemExistsCondition;
import dev.breezes.settlements.models.conditions.NearbyShearableSheepExistsCondition;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@CustomLog
public class ShearSheepBehaviorV2 extends BaseVillagerBehavior {

    private static final Stage SHEAR_SHEEP = new SimpleStage("SHEAR_SHEEP");
    private static final Stage PICK_UP_WOOL = new SimpleStage("PICK_UP_WOOL");

    private final StagedStep controlStep;
    private final NavigateToTargetStep navigateToTargetStep;
    private final BehaviorStep shearSheepStep;

    private final NearbyShearableSheepExistsCondition<BaseVillager> nearbyShearableSheepExistsCondition;
    private final NearbyItemExistsCondition<BaseVillager> nearbyWoolExistsCondition;

    @Nullable
    private BehaviorContext context;

    public ShearSheepBehaviorV2() {
        super(log, Ticks.seconds(5).asTickable(), Ticks.seconds(5).asTickable());

        // Create behavior preconditions
        this.nearbyShearableSheepExistsCondition = new NearbyShearableSheepExistsCondition<>(10, 4);
        this.preconditions.add(this.nearbyShearableSheepExistsCondition);

        // Create wool condition (not a precondition, evaluated during behavior execution)
        IEntityCondition<ItemEntity> isWoolCondition = itemEntity -> itemEntity != null && itemEntity.getItem().is(ItemTags.WOOL);
        this.nearbyWoolExistsCondition = new NearbyItemExistsCondition<>(2, 1, isWoolCondition, 1);

        // Create steps
        this.navigateToTargetStep = new NavigateToTargetStep(0.4f, 1);
        this.shearSheepStep = this.createShearSheepStep();

        this.controlStep = StagedStep.builder()
                .name("ShearSheepBehaviorV2")
                .initialStage(SHEAR_SHEEP)
                .stageStepMap(Map.of(
                        SHEAR_SHEEP, this.createShearSheepStep(),
                        PICK_UP_WOOL, this.createPickUpItemStep()
                ))
                .nextStage(ControlStages.STEP_END)
                .build();

        this.context = null;
    }

    private BehaviorStep createShearSheepStep() {
        TimeBasedStep shearStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .everyTick(context -> {
                    context.getInitiator().setHeldItem(Items.SHEARS.getDefaultInstance());
                    return Optional.empty();
                })
                .addKeyFrame(Ticks.seconds(0.5), context -> {
                    this.getTargetSheep(context)
                            .ifPresent(sheep -> {
                                sheep.shear(SoundSource.NEUTRAL);
                                log.behaviorStatus("Sheared sheep");
                            });
                    return Optional.empty();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();

                    // Determine if the villager should pick up wool
                    if (this.nearbyWoolExistsCondition.test(context.getInitiator().getMinecraftEntity())) {
                        List<Targetable> nearbyWoolItemTargets = this.nearbyWoolExistsCondition.getTargets().stream()
                                .map(Targetable::fromEntity)
                                .toList();
                        context.setState(BehaviorStateType.TARGET, TargetState.of(nearbyWoolItemTargets));

                        log.behaviorStatus("Found nearby wool item to pick up");
                        return Optional.of(PICK_UP_WOOL);
                    }

                    log.behaviorStatus("No nearby wool item to pick up");
                    return Optional.of(ControlStages.STEP_END);
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(this.navigateToTargetStep)
                .actionStep(shearStep)
                .build();
    }

    private BehaviorStep createPickUpItemStep() {
        return StayCloseStep.builder()
                .closeEnoughDistance(2.0)
                .navigateStep(this.navigateToTargetStep)
                .actionStep(context -> {
                    this.getTargetItem(context)
                            .ifPresent(item -> {
                                context.getInitiator().getMinecraftEntity().take(item, 999);
                                log.behaviorStatus("Picked up wool item");
                            });
                    return Optional.of(ControlStages.STEP_END);
                })
                .build();
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

    private Optional<ItemEntity> getTargetItem(@Nonnull BehaviorContext context) {
        ICondition<Targetable> isItemPredicate = (target) -> target != null
                && target.getType() == TargetableType.ENTITY
                && target.getAsEntity().getType() == EntityType.ITEM;

        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .map(targetState -> targetState.match(isItemPredicate))
                .flatMap(Stream::findFirst)
                .map(Targetable::getAsEntity)
                .map(ItemEntity.class::cast);
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.context = new BehaviorContext(entity);

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

}
