package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetQueries;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.conditions.NearbyEntityExistsCondition;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.SettlementsCat;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class TameCatBehavior extends StateMachineBehavior {

    private static final double TAME_SUCCESS_CHANCE = 0.33;
    private static final int MAX_TAME_ATTEMPTS = 5;

    private enum TameStage implements StageKey {
        TAME_CAT,
        END;
    }

    private final TameCatConfig config;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;

    private final NearbyEntityExistsCondition<BaseVillager, Cat> nearbyUntamedCatExistsCondition;

    private int attemptsRemaining;

    public TameCatBehavior(TameCatConfig config,
                           HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.config = config;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.tame_cat")
                .iconItemId(ResourceLocation.withDefaultNamespace("cod"))
                .displaySuffix(null)
                .build();

        this.attemptsRemaining = 0;

        this.nearbyUntamedCatExistsCondition = new NearbyEntityExistsCondition<>(
                config.scanRangeHorizontal(),
                config.scanRangeVertical(),
                EntityType.CAT,
                cat -> cat != null && !cat.isTame(),
                1);
        this.preconditions.add(this.nearbyUntamedCatExistsCondition);

        ICondition<BaseVillager> ownershipBelowLimitCondition = villager -> {
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
        };
        this.preconditions.add(ownershipBelowLimitCondition);

        this.initializeStateMachine(this.createControlStep(), TameStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("TameCatBehavior")
                .initialStage(TameStage.TAME_CAT)
                .stageStepMap(Map.of(TameStage.TAME_CAT, this.createTameCatStep()))
                .nextStage(TameStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep createTameCatStep() {
        TimeBasedStep attemptStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(ctx -> {
                    this.attemptsRemaining = Math.max(0, this.attemptsRemaining - 1);
                    return StepResult.noOp();
                })
                .everyTick(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.COD.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(Ticks.seconds(0.5), ctx -> {
                    Optional<Cat> catOptional = this.getTargetCat(ctx);
                    if (catOptional.isEmpty()) {
                        return StepResult.complete();
                    }
                    Cat cat = catOptional.get();

                    if (cat.isTame()) {
                        return StepResult.complete();
                    }

                    log.behaviorStatus("Attempting to tame cat {}", cat.getUUID());

                    Location catLoc = Location.fromEntity(cat, false);
                    if (RandomUtil.chance(TAME_SUCCESS_CHANCE)) {
                        catLoc.displayParticles(ParticleTypes.HEART, 7, 0.4, 0.5, 0.4, 0.01);
                        catLoc.playSound(SoundEvents.CAT_EAT, 0.8f, 1.0f, SoundSource.NEUTRAL);

                        cat.discard();

                        SettlementsCat settlementsCat = SettlementsCat.spawn(catLoc);
                        settlementsCat.setTame(true, true);
                        settlementsCat.setOwnerUUID(ctx.getInitiator().getMinecraftEntity().getUUID());
                        settlementsCat.setCollarColor(DyeColor.LIME);

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

        return StayCloseStep.builder()
                .closeEnoughDistance(2.5)
                .navigateStep(new NavigateToTargetStep(0.55f, 2))
                .actionStep(attemptStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        Expertise expertise = entity.getExpertise();
        int limit = this.config.expertiseCatLimit().getOrDefault(expertise.getConfigName(), 1);

        int owned = entity.level().getEntitiesOfClass(Cat.class,
                        entity.getBoundingBox().inflate(48, 16, 48),
                        cat -> cat.isTame() && ownerMatches(entity, cat))
                .size();
        if (owned >= limit) {
            log.behaviorStatus("Owned cats {} >= limit {}, aborting tame", owned, limit);
            this.requestStop();
            return;
        }

        if (!this.nearbyUntamedCatExistsCondition.test(entity)) {
            this.requestStop();
            return;
        }

        Optional<Cat> chosenCat = this.nearbyUntamedCatExistsCondition.getTargets().stream().findFirst();
        if (chosenCat.isEmpty()) {
            this.requestStop();
            return;
        }
        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(chosenCat.get()))));
        this.attemptsRemaining = MAX_TAME_ATTEMPTS;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        entity.clearHeldItem();
    }

    private Optional<Cat> getTargetCat(@Nonnull BehaviorContext context) {
        return TargetQueries.firstEntity(context, EntityType.CAT, Cat.class);
    }

    private static boolean ownerMatches(@Nonnull BaseVillager villager, @Nonnull Cat cat) {
        try {
            return villager.getUUID().equals(cat.getOwnerUUID());
        } catch (Throwable t) {
            return cat.getOwner() != null && cat.getOwner().getUUID().equals(villager.getUUID());
        }
    }

}
