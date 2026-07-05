package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
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
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@CustomLog
public class CollectDemandedItemBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 1.5D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;

    private static final int COLLECT_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final int MAX_ITEMS_PER_RUN = 5;

    private enum Stage implements StageKey {
        RESOLVE, COLLECT, LOOP, END
    }

    private final DemandedGroundItemCondition itemCondition;
    private final Map<String, Integer> collectedByItem = new HashMap<>();

    @Nullable
    private DemandedGroundItemCondition.Resolution resolution;

    public CollectDemandedItemBehavior(@Nonnull CollectDemandedItemConfig config,
                                       @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);

        this.itemCondition = new DemandedGroundItemCondition(support.getDemandEvaluator(), NAVIGATION_COMPLETION_DISTANCE);
        this.preconditions.add(this.itemCondition);

        this.resolution = null;

        // Must be last — initializeStateMachine captures the step graph and calls reset() on all steps.
        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.RESOLVE, this.createResolveStep());
        stageMap.put(Stage.COLLECT, this.createCollectStep());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("CollectDemandedItemLoopBack")
                .loopBackTo(Stage.RESOLVE)
                .completionTransition(Stage.END)
                .maxIterationsResolver(ctx -> MAX_ITEMS_PER_RUN)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("CollectDemandedItemBehavior")
                .initialStage(Stage.RESOLVE)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createResolveStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("ResolveDemandedItem")
                .action(ctx -> {
                    // Re-run fresh every iteration
                    if (!this.itemCondition.test(ctx.getInitiator())) {
                        return StepResult.transition(Stage.END);
                    }

                    this.resolution = this.itemCondition.getResolution().orElse(null);
                    if (this.resolution == null) {
                        return StepResult.transition(Stage.END);
                    }

                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.resolution.item())));
                    return StepResult.transition(Stage.COLLECT);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createCollectStep() {
        TimeBasedStep<BaseVillager> pickupStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(PickUpAnimations.PICK_UP_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    if (this.resolution == null || !this.resolution.item().isAlive()) {
                        return StepResult.transition(Stage.LOOP);
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(PickUpAnimations.PICK_UP_AT_TICK), ctx -> {
                    // Re-validate at the keyframe: another villager or player may have grabbed the
                    // item between the time we entered the step and the animation impact frame.
                    if (this.resolution == null || !this.resolution.item().isAlive()) {
                        return StepResult.transition(Stage.LOOP);
                    }
                    ItemEntity item = this.resolution.item();
                    ctx.getInitiator().pickUp(item);
                    this.recordPickup(ctx, item);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(Stage.LOOP))
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(NAVIGATION_COMPLETION_DISTANCE)
                        .unreachableTransition(Stage.END)
                        .build())
                .actionStep(pickupStep)
                .timeoutTicks(COLLECT_TIMEOUT_TICKS)
                .timeoutTransition(Stage.END)
                .build();
    }

    /**
     * Folds one pickup into the run's single ITEM_COLLECTED deed, declaring it lazily on first use.
     * Each loop iteration can resolve a different demanded item, so this accumulates a per-item
     * tally onto one deed rather than declaring a new primary deed per pickup.
     */
    private void recordPickup(@Nonnull BehaviorContext<BaseVillager> ctx, @Nonnull ItemEntity item) {
        String itemId = BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath();
        int count = item.getItem().getCount();
        this.collectedByItem.merge(itemId, count, Integer::sum);

        BehaviorOutcome outcome = ctx.primaryDeed().orElseGet(() ->
                ctx.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.ITEM_COLLECTED, null)));
        outcome.putDetailField("items", formatItemList(this.collectedByItem));
        outcome.recordYield(count);
    }

    /**
     * Compact {@code "<count> <itemId>"} list, most-collected first, delimited by {@code "; "}.
     */
    private static String formatItemList(@Nonnull Map<String, Integer> byItem) {
        return byItem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining("; "));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();
        this.resolution = null;
        this.collectedByItem.clear();
    }

}
