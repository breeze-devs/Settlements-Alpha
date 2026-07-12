package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.petting;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
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
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.pet.Pettable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class PetAnimalBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(30).getTicksAsInt();

    private static final int PET_INTERVAL_TICKS = ClockTicks.seconds(1).getTicksAsInt();

    private enum PetStage implements StageKey {
        APPROACH,
        PET,
        END;
    }

    private final PetAnimalConfig config;
    private final NearbyPettableExistsCondition nearbyPettableCondition;

    @Nullable
    private LivingEntity cachedPet;

    public PetAnimalBehavior(@Nonnull PetAnimalConfig config,
                             @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);
        this.config = config;
        this.cachedPet = null;

        this.nearbyPettableCondition = new NearbyPettableExistsCondition(config.horizontalScanRange(), config.verticalScanRange());
        this.preconditions.add(this.nearbyPettableCondition);
        this.continueConditions.add(ICondition.named("CachedPetStillValid",
                villager -> this.cachedPet != null && this.cachedPet.isAlive() && !this.cachedPet.isRemoved()));

        this.initializeStateMachine(this.createControlStep(), PetStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("PetAnimalBehavior")
                .initialStage(PetStage.APPROACH)
                .stageStepMap(Map.of(
                        PetStage.APPROACH, this.createApproachStep(),
                        PetStage.PET, this.createPetStep()))
                .nextStage(PetStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createApproachStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(NAVIGATION_COMPLETION_DISTANCE)
                        .unreachableTransition(PetStage.END)
                        .build())
                .actionStep(ctx -> StepResult.transition(PetStage.PET))
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(PetStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPetStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(this.config.petCount()).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addPeriodicStep(PET_INTERVAL_TICKS, ctx -> {
                    this.performPet(ctx);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(PetStage.END))
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.cachedPet = null;

        if (!this.nearbyPettableCondition.test(villager)) {
            this.requestStop("No pettable animals within range");
            return;
        }

        this.cachedPet = this.nearbyPettableCondition.getTargets().stream().findFirst().orElse(null);
        if (this.cachedPet == null) {
            this.requestStop("Chosen pettable animal is null");
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.cachedPet)));
        context.setState(BehaviorStateType.LOOK_TARGET, LookState.ofEntity(this.cachedPet));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();

        this.cachedPet = null;
    }

    private void performPet(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.cachedPet == null || !this.cachedPet.isAlive() || this.cachedPet.isRemoved()) {
            return;
        }

        // Every scanned target implements Pettable by construction of NearbyPettableExistsCondition.
        ((Pettable) this.cachedPet).pet(context.getInitiator());

        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.ANIMAL_PETTED, null);
        outcome.markSucceeded();
        context.declarePrimaryDeed(outcome);
    }

}
