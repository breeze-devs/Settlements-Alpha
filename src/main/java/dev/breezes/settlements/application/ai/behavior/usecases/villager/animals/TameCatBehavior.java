package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
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
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.SettlementsCat;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class TameCatBehavior extends VillagerStateMachineBehavior {

    private static final ResourceLocation COD_ID = ResourceLocation.withDefaultNamespace("cod");

    private static final double TAME_SUCCESS_CHANCE = 0.33;
    private static final int MAX_TAME_ATTEMPTS = 5;

    private enum TameStage implements StageKey {
        TAME_CAT,
        END;
    }

    private final TameCatConfig config;

    private int attemptsRemaining;
    private boolean shouldRewardExperience;

    public TameCatBehavior(TameCatConfig config,
                           BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.config = config;

        this.attemptsRemaining = 0;
        this.shouldRewardExperience = false;

        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, Cat>builder()
                .entityType(Cat.class)
                .filter((villager, cat) -> !cat.isTame())
                .completionRange(2)
                .build());

        // Precondition: has at least one cod
        this.preconditions.add(support.getDemandSignalService().requireItem(new ItemMatch.ItemRef(COD_ID), 1, 50, this.getClass().getSimpleName()));

        this.preconditions.add(ICondition.named("OwnershipBelowLimit", villager -> {
            Expertise expertise = villager.getExpertise();
            int limit = this.config.expertiseCatLimit().getOrDefault(expertise.getConfigName(), 1);

            int owned = villager.level().getEntitiesOfClass(Cat.class,
                            villager.getBoundingBox().inflate(48, 16, 48),
                            cat -> cat.isTame() && ownerMatches(villager, cat))
                    .size();

            if (owned >= limit) {
                log.behaviorStatus("Owned cats {} >= limit {}, aborting tame (precondition)", owned, limit);
                return false;
            }
            return true;
        }));

        this.initializeStateMachine(this.createControlStep(), TameStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("TameCatBehavior")
                .initialStage(TameStage.TAME_CAT)
                .stageStepMap(Map.of(TameStage.TAME_CAT, this.createTameCatStep()))
                .nextStage(TameStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createTameCatStep() {
        TimeBasedStep<BaseVillager> attemptStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(ctx -> {
                    this.attemptsRemaining = Math.max(0, this.attemptsRemaining - 1);
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    ctx.getInitiator().setHeldItem(Items.COD.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), ctx -> {
                    Optional<Cat> catOptional = this.getTargetCat(ctx);
                    if (catOptional.isEmpty()) {
                        return StepResult.complete();
                    }
                    Cat cat = catOptional.get();

                    if (cat.isTame()) {
                        return StepResult.complete();
                    }

                    // Consume a cod for each attempt
                    if (!ctx.getInitiator().getMinecraftEntity().getSettlementsInventory()
                            .consumeIfRequired(Items.COD, 1, GeneralConfig.bypassInventoryRequirements)) {
                        return StepResult.complete();
                    }

                    log.behaviorStatus("Attempting to tame cat {}", cat.getUUID());

                    Location catLoc = Location.fromEntity(cat, false);
                    if (RandomUtil.chance(TAME_SUCCESS_CHANCE)) {
                        catLoc.displayParticles(ParticleTypes.HEART, 7, 0.4, 0.5, 0.4, 0.01);
                        catLoc.playSound(SoundEvents.CAT_EAT, 0.8f, 1.0f, SoundSource.NEUTRAL);

                        // Preserve the cat's coat variant before discarding so the SettlementsCat
                        // entity doesn't silently reroll to a different skin on tame.
                        Holder<CatVariant> variant = cat.getVariant();
                        boolean wasBaby = cat.isBaby();
                        cat.discard();

                        SettlementsCat settlementsCat = SettlementsCat.spawn(catLoc);
                        settlementsCat.setVariant(variant);
                        if (wasBaby) {
                            settlementsCat.setBaby(true);
                        }
                        settlementsCat.setTame(true, true);
                        settlementsCat.setOwnerUUID(ctx.getInitiator().getMinecraftEntity().getUUID());
                        settlementsCat.setCollarColor(DyeColor.LIME);
                        this.shouldRewardExperience = true;

                        BehaviorOutcome tameOutcome = BehaviorOutcome.forDeed(WorldEventType.ANIMAL_TAMED, null);
                        tameOutcome.recordDeedDetail("a cat");
                        ctx.declarePrimaryDeed(tameOutcome);

                        log.behaviorStatus("Successfully tamed cat {}", settlementsCat.getUUID());
                        return StepResult.complete();
                    }

                    catLoc.displayParticles(ParticleTypes.SMOKE, 6, 0.35, 0.5, 0.35, 0.01);
                    log.behaviorStatus("Failed to tame cat");
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();

                    if (this.attemptsRemaining > 0) {
                        Optional<Cat> target = this.getTargetCat(ctx);
                        if (target.isPresent() && target.get().isAlive() && !target.get().isTame()) {
                            return StepResult.transition(TameStage.TAME_CAT);
                        }
                    }

                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(2.5)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                .actionStep(attemptStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        Expertise expertise = villager.getExpertise();
        int limit = this.config.expertiseCatLimit().getOrDefault(expertise.getConfigName(), 1);

        int owned = villager.level().getEntitiesOfClass(Cat.class,
                        villager.getBoundingBox().inflate(48, 16, 48),
                        cat -> cat.isTame() && ownerMatches(villager, cat))
                .size();
        if (owned >= limit) {
            log.behaviorStatus("Owned cats {} >= limit {}, aborting tame", owned, limit);
            this.requestStop("Owned cats greater than taming limit");
            return;
        }

        Optional<Cat> chosenCat = this.findClosestReachableUntamedCat(villager);
        if (chosenCat.isEmpty()) {
            this.requestStop("No reachable untamed cats found");
            return;
        }
        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(chosenCat.get()))));
        int codAvailable = villager.getSettlementsInventory().count(Items.COD);
        this.attemptsRemaining = GeneralConfig.bypassInventoryRequirements
                ? MAX_TAME_ATTEMPTS
                : Math.min(MAX_TAME_ATTEMPTS, codAvailable);
        if (this.attemptsRemaining <= 0) {
            this.requestStop("No cod available at behavior start");
            return;
        }
        this.shouldRewardExperience = false;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.shouldRewardExperience = false;
    }

    private Optional<Cat> getTargetCat(@Nonnull BehaviorContext<BaseVillager> context) {
        return TargetQueries.firstEntity(context, EntityType.CAT, Cat.class);
    }

    private Optional<Cat> findClosestReachableUntamedCat(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .closest(Cat.class, cat -> !cat.isTame()
                        && villager.getNavigationManager().canReach(Location.fromEntity(cat, false), 2), villager);
    }

    private static boolean ownerMatches(@Nonnull BaseVillager villager, @Nonnull Cat cat) {
        try {
            return villager.getUUID().equals(cat.getOwnerUUID());
        } catch (Throwable t) {
            return cat.getOwner() != null && cat.getOwner().getUUID().equals(villager.getUUID());
        }
    }

}
