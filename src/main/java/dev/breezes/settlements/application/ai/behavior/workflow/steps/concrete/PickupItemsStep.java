package dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.items.ItemState;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Picks up every still-alive {@link ItemEntity} held in {@code BehaviorStateType.ITEMS_TO_PICK_UP},
 * then transitions to {@code nextStage}.
 * <p>
 * The state is not cleared after pickup — the next producer of the state ({@link ItemState#of(java.util.List)})
 * replaces the value via {@link BehaviorContext#setState(BehaviorStateType, dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState)}.
 * <p>
 * The explicit {@code nextStage} is mandatory rather than relying on {@link StepResult#complete()}
 * because inside a {@code StagedStep} a {@code complete()} unwinds the entire staged step to its
 * parent's next stage, skipping every sibling stage downstream of pickup (LOOP, AWARD, ...). Wiring
 * the destination at the call-site keeps the routing of the 7-stage harvest template legible.
 */
public class PickupItemsStep extends AbstractStep<BaseVillager> {

    private final StageKey nextStage;
    private int elapsedTicks;
    private boolean pickedUp;

    @Builder
    private PickupItemsStep(@Nonnull String name,
                            @Nonnull StageKey nextStage,
                            int timeoutTicks,
                            @Nullable StageKey timeoutTransition) {
        super(name, timeoutTicks, timeoutTransition);
        this.nextStage = nextStage;
        this.elapsedTicks = 0;
        this.pickedUp = false;
    }

    @Override
    protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
        this.elapsedTicks++;

        if (this.elapsedTicks == 1) {
            context.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
        }

        if (!this.pickedUp && this.elapsedTicks >= PickUpAnimations.PICK_UP_AT_TICK) {
            this.pickUpItems(context);
            this.pickedUp = true;
        }

        if (this.elapsedTicks >= PickUpAnimations.PICK_UP_DURATION_TICKS) {
            return StepResult.transition(this.nextStage);
        }

        return StepResult.noOp();
    }

    private void pickUpItems(@Nonnull BehaviorContext<BaseVillager> context) {
        ISettlementsVillager villager = context.getInitiator();
        context.getState(BehaviorStateType.ITEMS_TO_PICK_UP, ItemState.class)
                .ifPresent(items -> items.getItems().stream()
                        .filter(ItemEntity::isAlive)
                        .forEach(villager::pickUp));
    }

    @Override
    protected void doOnEnter() {
        this.elapsedTicks = 0;
        this.pickedUp = false;
    }

}
