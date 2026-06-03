package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.DropLeashObligation;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
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
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.NearbyBreedableAnimalPairExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.tags.EntityTag;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

@CustomLog
public class BreedAnimalsBehavior extends VillagerStateMachineBehavior {

    private enum BreedStage implements StageKey {
        FEED_FIRST,
        FEED_SECOND,
        WAITING_FOR_BREEDING,
        END;
    }

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private final BreedSpecies species;
    private final NearbyBreedableAnimalPairExistsCondition<BaseVillager> nearbyBreedableAnimalPairExistsCondition;

    @Nullable
    private ItemStack heldItem;
    @Nullable
    private Animal breedTarget1;
    @Nullable
    private Animal breedTarget2;
    private boolean shouldRewardExperience;

    public BreedAnimalsBehavior(BreedAnimalsConfig config,
                                HungerConfig hungerConfig,
                                BreedSpecies species,
                                DemandSignalService demandSignalService) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.species = species;
        this.nearbyBreedableAnimalPairExistsCondition = new NearbyBreedableAnimalPairExistsCondition<>(
                config.scanRangeHorizontal(),
                config.scanRangeVertical(),
                species.getType());
        this.preconditions.add(this.nearbyBreedableAnimalPairExistsCondition);
        this.preconditions.add(demandSignalService.requireAny(
                List.of(new ItemMatch.TagRef(species.getFoodTag())),
                1, 50, this.getClass().getSimpleName()));

        this.heldItem = null;
        this.breedTarget1 = null;
        this.breedTarget2 = null;
        this.shouldRewardExperience = false;

        this.initializeStateMachine(this.createControlStep(), BreedStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("BreedAnimalsBehavior")
                .initialStage(BreedStage.FEED_FIRST)
                .stageStepMap(Map.of(
                        BreedStage.FEED_FIRST, this.createFeedTargetStep(
                                "first",
                                BreedStage.FEED_SECOND,
                                () -> this.breedTarget1,
                                () -> this.breedTarget2),
                        BreedStage.FEED_SECOND, this.createFeedTargetStep(
                                "second",
                                BreedStage.WAITING_FOR_BREEDING,
                                () -> this.breedTarget2,
                                null),
                        BreedStage.WAITING_FOR_BREEDING, this.createWaitingStep()))
                .nextStage(BreedStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        Optional<NearbyBreedableAnimalPairExistsCondition.BreedablePair<?>> breedablePair =
                this.nearbyBreedableAnimalPairExistsCondition.getBreedablePair();
        if (breedablePair.isEmpty()) {
            this.requestStop("No breedable pair found, stopping behavior");
            return;
        }

        this.breedTarget1 = breedablePair.get().getFirst();
        this.breedTarget2 = breedablePair.get().getSecond();
        this.shouldRewardExperience = false;

        Optional<ItemStack> foundFood = findFirstMatchingFood(villager);
        if (foundFood.isPresent()) {
            this.heldItem = foundFood.get().copyWithCount(1);
        } else if (GeneralConfig.bypassInventoryRequirements) {
            this.heldItem = new ItemStack(this.species.getCanonicalFood());
        } else {
            this.requestStop("No food available for breeding at behavior start");
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.breedTarget1)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.breedTarget1 != null
                && this.breedTarget2 != null
                && this.breedTarget1.isAlive()
                && this.breedTarget2.isAlive();
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        // Only claim if we actually started feeding; otherwise we'd grab unrelated nearby babies
        if (this.breedTarget1 != null) {
            this.claimNearbyBabyAnimals(villager);
        }

        for (Animal target : new Animal[]{this.breedTarget1, this.breedTarget2}) {
            if (target == null) {
                continue;
            }
            // Leash drop is handled by DropLeashObligation in TeardownScope.
            target.setAge(6000); // reset breeding cooldown
        }

        this.heldItem = null;
        this.breedTarget1 = null;
        this.breedTarget2 = null;
        this.shouldRewardExperience = false;
    }

    private BehaviorStep<BaseVillager> createFeedTargetStep(@Nonnull String label,
                                                            @Nonnull BreedStage nextStage,
                                                            @Nonnull Supplier<Animal> targetSupplier,
                                                            @Nullable Supplier<Animal> nextTargetSupplier) {
        TimeBasedStep<BaseVillager> feedStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.ONE.asTickable())
                .onStart(ctx -> {
                    Animal target = targetSupplier.get();
                    if (target == null || this.heldItem == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().setHeldItem(this.heldItem);
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    Animal target = targetSupplier.get();
                    StepResult result = this.feedAnimal(ctx.getInitiator().getMinecraftEntity(), ctx, target, label);
                    if (!(result instanceof StepResult.NoOp)) {
                        return result;
                    }

                    target.setInLove(null);

                    // Pre-load the next stage's target into context so StayCloseStep
                    // navigates to the correct animal on its very first tick
                    if (nextTargetSupplier != null) {
                        Animal nextTarget = nextTargetSupplier.get();
                        if (nextTarget != null) {
                            ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(nextTarget)));
                        }
                    }

                    return StepResult.transition(nextStage);
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(feedStep)
                .build();
    }

    private BehaviorStep<BaseVillager> createWaitingStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(3).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.noOp();
                })
                .everyTick(ctx -> {
                    if (this.breedTarget1 == null || this.breedTarget2 == null) {
                        return StepResult.complete();
                    }

                    if (this.breedTarget1.getNavigation().isDone()) {
                        this.breedTarget1.getNavigation().moveTo(this.breedTarget2, 1.0D);
                    }
                    if (this.breedTarget2.getNavigation().isDone()) {
                        this.breedTarget2.getNavigation().moveTo(this.breedTarget1, 1.0D);
                    }

                    if (ctx.getInitiator().getNavigationManager().isNavigating()) {
                        ctx.getInitiator().getNavigationManager().stop();
                    }

                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.shouldRewardExperience) {
                        this.rewardExperience(ctx.getInitiator().getMinecraftEntity());
                    }
                    return StepResult.complete();
                })
                .build();
    }

    private StepResult feedAnimal(@Nonnull BaseVillager villager,
                                  @Nonnull BehaviorContext<BaseVillager> context,
                                  @Nullable Animal target,
                                  @Nonnull String label) {
        if (target == null || this.heldItem == null) {
            return StepResult.complete();
        }

        log.behaviorStatus("Feeding {} animal: '{}'", label, target);

        if (!villager.getSettlementsInventory().consumeIfRequired(this.heldItem.getItem(), 1, GeneralConfig.bypassInventoryRequirements)) {
            return StepResult.complete();
        }

        target.setLeashedTo(villager, true);
        context.getTeardownScope().track(new DropLeashObligation(target.getUUID(), villager.getUUID(), target.blockPosition()));

        Location targetLocation = Location.fromEntity(target, false);
        ParticleRegistry.breedHearts(targetLocation);
        ParticleRegistry.breedItemConsume(targetLocation, this.heldItem);
        SoundRegistry.FEED_ANIMAL.playGlobally(targetLocation, SoundSource.NEUTRAL);

        log.behaviorStatus("Fed {} animal", label);
        this.shouldRewardExperience = true;
        return StepResult.noOp();
    }

    private Optional<ItemStack> findFirstMatchingFood(@Nonnull BaseVillager villager) {
        return villager.getSettlementsInventory().findFirst(stack -> stack.is(this.species.getFoodTag()));
    }

    private void claimNearbyBabyAnimals(@Nonnull BaseVillager villager) {
        AABB scanBoundary = villager.getBoundingBox().inflate(6, 3, 6);
        Predicate<Entity> isBabyOfRightType = targetEntity -> targetEntity.getType() == this.species.getType()
                && ((Animal) targetEntity).isBaby();
        List<Entity> nearbyEntities = villager.level().getEntities(villager, scanBoundary, isBabyOfRightType);
        for (Entity nearbyEntity : nearbyEntities) {
            log.behaviorStatus("Claiming baby animal '{}' as village-owned", nearbyEntity);
            nearbyEntity.addTag(EntityTag.VILLAGE_OWNED_ANIMAL.getTag());
        }
    }

}
