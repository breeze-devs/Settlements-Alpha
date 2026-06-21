package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class CollectDemandedItemBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 1.5D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;

    private enum Stage implements StageKey {
        COLLECT,
        END
    }

    private final DemandedGroundItemCondition itemCondition;

    @Nullable
    private DemandedGroundItemCondition.Resolution resolution;

    public CollectDemandedItemBehavior(@Nonnull CollectDemandedItemConfig config,
                                       @Nonnull HungerConfig hungerConfig,
                                       @Nonnull DemandEvaluator demandEvaluator) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.itemCondition = new DemandedGroundItemCondition(demandEvaluator);
        this.preconditions.add(this.itemCondition);

        this.resolution = null;

        // Must be last — initializeStateMachine captures the step graph and calls reset() on all steps.
        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("CollectDemandedItemBehavior")
                .initialStage(Stage.COLLECT)
                .stageStepMap(Map.of(Stage.COLLECT, this.createCollectStep()))
                .nextStage(Stage.END)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.itemCondition.getResolution().isEmpty()) {
            this.requestStop("No demanded dropped items nearby");
            return;
        }

        this.resolution = this.itemCondition.getResolution().get();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.resolution.item())));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();
        this.resolution = null;
    }

    private BehaviorStep<BaseVillager> createCollectStep() {
        TimeBasedStep<BaseVillager> pickupStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(PickUpAnimations.PICK_UP_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for pickup");
                    }
                    // Item may have despawned or been picked up by another entity while walking over.
                    if (!this.resolution.item().isAlive()) {
                        return StepResult.fail("Target item is no longer present");
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(PickUpAnimations.PICK_UP_AT_TICK), ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for pickup");
                    }
                    // Re-validate at the keyframe: another villager or player may have grabbed the
                    // item between the time we entered the step and the animation impact frame.
                    if (!this.resolution.item().isAlive()) {
                        return StepResult.fail("Target item was taken before pickup could complete");
                    }
                    ctx.getInitiator().pickUp(this.resolution.item());
                    BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.ITEM_COLLECTED, null);
                    outcome.markSucceeded();
                    outcome.recordDeedDetail(this.resolution.item().getItem().toString());
                    ctx.declarePrimaryDeed(outcome);
                    return StepResult.noOp();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(pickupStep)
                .build();
    }

}
