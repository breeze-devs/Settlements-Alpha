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
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.conditions.NearbyEntityExistsCondition;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class TameWolfBehaviorV2 extends StateMachineBehavior {

    private static final double TAME_SUCCESS_CHANCE = 0.33;
    private static final int MAX_TAME_ATTEMPTS = 5;

    private enum TameStage implements StageKey {
        TAME_WOLF,
        END;
    }

    private final TameWolfConfig config;
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;

    private final NearbyEntityExistsCondition<BaseVillager, Wolf> nearbyUntamedWolfExistsCondition;

    private int attemptsRemaining;

    public TameWolfBehaviorV2(TameWolfConfig config) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable());

        this.config = config;
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.tame_wolf")
                .iconItemId(ResourceLocation.withDefaultNamespace("bone"))
                .displaySuffix(null)
                .build();

        this.attemptsRemaining = 0;

        // Precondition: at least one untamed wolf nearby
        this.nearbyUntamedWolfExistsCondition = new NearbyEntityExistsCondition<>(
                config.scanRangeHorizontal(),
                config.scanRangeVertical(),
                EntityType.WOLF,
                wolf -> wolf != null && !wolf.isTame(),
                1);
        this.preconditions.add(this.nearbyUntamedWolfExistsCondition);

        // Precondition: ownership below per-expertise limit
        // TODO: we should move this somewhere else
        ICondition<BaseVillager> ownershipBelowLimitCondition = villager -> {
            Expertise expertise = villager.getExpertise();
            int limit = this.config.expertiseWolfLimit().getOrDefault(expertise.getConfigName(), 1);

            // TODO: this logic should be replaced by memory-based systems and a direct level entity get if alive
            int owned = villager.level().getEntitiesOfClass(Wolf.class,
                            villager.getBoundingBox().inflate(48, 16, 48),
                            wolf -> wolf.isTame() && ownerMatches(villager, wolf))
                    .size();

            if (owned >= limit) {
                log.behaviorStatus("Owned wolves {} >= limit {}, aborting tame (precondition)", owned, limit);
                return false;
            }
            return true;
        };
        this.preconditions.add(ownershipBelowLimitCondition);

        this.initializeStateMachine(this.createControlStep(), TameStage.END);

    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("TameWolfBehaviorV2")
                .initialStage(TameStage.TAME_WOLF)
                .stageStepMap(Map.of(TameStage.TAME_WOLF, this.createTameWolfStep()))
                .nextStage(TameStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep createTameWolfStep() {
        TimeBasedStep attemptStep = TimeBasedStep.builder()
                .withTickable(Ticks.seconds(1).asTickable())
                .onStart(ctx -> {
                    // Consume one attempt when we start the action window
                    this.attemptsRemaining = Math.max(0, this.attemptsRemaining - 1);
                    return StepResult.noOp();
                })
                .everyTick(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.BONE.getDefaultInstance());
                    return StepResult.noOp();
                })
                .addKeyFrame(Ticks.seconds(0.5), ctx -> {
                    Optional<Wolf> wolfOptional = this.getTargetWolf(ctx);
                    if (wolfOptional.isEmpty()) {
                        return StepResult.complete();
                    }
                    Wolf wolf = wolfOptional.get();

                    // If already tamed somehow, end
                    if (wolf.isTame()) {
                        return StepResult.complete();
                    }

                    // Try to tame
                    log.behaviorStatus("Attempting to tame wolf {}", wolf.getUUID());

                    if (RandomUtil.chance(TAME_SUCCESS_CHANCE)) {
                        // Success effects
                        Location wolfLoc = Location.fromEntity(wolf, false);
                        wolfLoc.displayParticles(ParticleTypes.HEART, 7, 0.4, 0.5, 0.4, 0.01);
                        Location villagerHead = Location.fromEntity(ctx.getInitiator().getMinecraftEntity(), true);
                        villagerHead.displayParticles(ParticleTypes.HAPPY_VILLAGER, 8, 0.3, 0.2, 0.3, 0.01);
                        wolfLoc.playSound(SoundEvents.WOLF_AMBIENT, 0.6f, 1.5f, SoundSource.NEUTRAL);
                        wolfLoc.playSound(SoundEvents.WOLF_AMBIENT, 0.6f, 1.7f, SoundSource.NEUTRAL);

                        // Replace vanilla wolf with modded wolf
                        wolf.discard();

                        SettlementsWolf settlementsWolf = SettlementsWolf.spawn(wolfLoc);
                        settlementsWolf.setTame(true, true);
                        settlementsWolf.setOwnerUUID(ctx.getInitiator().getMinecraftEntity().getUUID());
                        settlementsWolf.setCollarColor(DyeColor.LIME);

                        log.behaviorStatus("Successfully tamed wolf {}", settlementsWolf.getUUID());

                        // Stop the behavior after success
                        return StepResult.complete();
                    } else {
                        // Failure effects
                        Location wolfLoc = Location.fromEntity(wolf, false);
                        wolfLoc.displayParticles(ParticleTypes.SMOKE, 6, 0.35, 0.5, 0.35, 0.01);
                        log.behaviorStatus("Failed to tame wolf");
                    }
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    // Clear held item
                    ctx.getInitiator().clearHeldItem();

                    // Retry only against the same locked target
                    if (this.attemptsRemaining > 0) {
                        Optional<Wolf> target = this.getTargetWolf(ctx);
                        if (target.isPresent() && target.get().isAlive() && !target.get().isTame()) {
                            return StepResult.transition(TameStage.TAME_WOLF);
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

        // Enforce wolf ownership limit per expertise
        Expertise expertise = entity.getExpertise();
        int limit = this.config.expertiseWolfLimit().getOrDefault(expertise.getConfigName(), 1);

        // TODO: this logic should be replaced by memory-based systems and a direct level entity get if alive
        int owned = entity.level().getEntitiesOfClass(Wolf.class,
                        entity.getBoundingBox().inflate(48, 16, 48),
                        wolf -> wolf.isTame() && ownerMatches(entity, wolf))
                .size();
        if (owned >= limit) {
            log.behaviorStatus("Owned wolves {} >= limit {}, aborting tame", owned, limit);
            this.requestStop();
            return;
        }

        if (!this.nearbyUntamedWolfExistsCondition.test(entity)) {
            this.requestStop();
            return;
        }

        Optional<Wolf> chosenWolf = this.nearbyUntamedWolfExistsCondition.getTargets().stream().findFirst();
        if (chosenWolf.isEmpty()) {
            this.requestStop();
            return;
        }
        context.setState(BehaviorStateType.TARGET, TargetState.of(List.of(Targetable.fromEntity(chosenWolf.get()))));
        this.attemptsRemaining = MAX_TAME_ATTEMPTS;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        entity.clearHeldItem();
    }

    private Optional<Wolf> getTargetWolf(@Nonnull BehaviorContext context) {
        return TargetQueries.firstEntity(context, EntityType.WOLF, Wolf.class);
    }

    private static boolean ownerMatches(@Nonnull BaseVillager villager, @Nonnull Wolf wolf) {
        try {
            return villager.getUUID().equals(wolf.getOwnerUUID());
        } catch (Throwable t) {
            // Fallback using getOwner, if available
            return wolf.getOwner() != null && wolf.getOwner().getUUID().equals(villager.getUUID());
        }
    }
}
