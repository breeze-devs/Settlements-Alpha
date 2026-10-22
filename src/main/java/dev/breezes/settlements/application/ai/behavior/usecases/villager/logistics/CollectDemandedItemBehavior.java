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
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CustomLog(topic = "behavior")
public class CollectDemandedItemBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 1.5D;
    public static final int NAVIGATION_COMPLETION_DISTANCE = 0;

    private static final ClockTicks COLLECT_TIMEOUT = ClockTicks.seconds(30);
    private static final int MAX_ITEMS_PER_RUN = 5;

    public static final ClockTicks MAX_RUN_DURATION = ClockTicks.of(COLLECT_TIMEOUT.getTicks() * 2);

    private enum Stage implements StageKey {
        RESOLVE, COLLECT, LOOP, END
    }

    private final DemandEvaluator demandEvaluator;
    private final Map<String, Integer> collectedByItem = new HashMap<>();

    @Nullable
    private ItemEntity target;

    public CollectDemandedItemBehavior(@Nonnull CollectDemandedItemConfig config,
                                       @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);

        this.demandEvaluator = support.getDemandEvaluator();
        this.preconditions.add(ICondition.named("DemandedGroundItemsRemembered", villager -> villager != null
                && villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS).isPresent()));

        this.target = null;

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
                    this.target = this.takeNextRemembered(ctx.getInitiator());
                    if (this.target == null) {
                        return StepResult.transition(Stage.END);
                    }

                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    return StepResult.transition(Stage.COLLECT);
                })
                .build();
    }

    /**
     * Removes the next valid demanded item from the remembered list and returns it, or null once none is left.
     * An item is removed whether it is collected.
     */
    @Nullable
    private ItemEntity takeNextRemembered(@Nonnull BaseVillager villager) {
        List<ItemEntity> remembered = new ArrayList<>(villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS)
                .orElse(List.of()));

        // Resolved on every take, since an earlier pickup this run may have filled the demand that made an item wanted
        List<ActiveDemand> demands = remembered.isEmpty() ? List.of() : this.demandEvaluator.resolve(villager);
        ItemEntity next = null;
        while (next == null && !remembered.isEmpty()) {
            ItemEntity candidate = remembered.removeFirst();
            if (candidate.isAlive() && isDemanded(candidate, demands)) {
                next = candidate;
            }
        }

        if (remembered.isEmpty()) {
            villager.getSettlementsBrain().clearMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS);
        } else {
            villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS, List.copyOf(remembered));
        }
        return next;
    }

    private static boolean isDemanded(@Nonnull ItemEntity item, @Nonnull List<ActiveDemand> demands) {
        for (ActiveDemand demand : demands) {
            if (ItemMatches.test(demand.match(), item.getItem())) {
                return true;
            }
        }
        return false;
    }

    private BehaviorStep<BaseVillager> createCollectStep() {
        TimeBasedStep<BaseVillager> pickupStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(PickUpAnimations.PICK_UP_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    if (this.target == null || !this.target.isAlive()) {
                        return StepResult.transition(Stage.LOOP);
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.PICK_UP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(PickUpAnimations.PICK_UP_AT_TICK), ctx -> {
                    // Re-validate at the keyframe: another villager or player may have grabbed the
                    // item between the time we entered the step and the animation impact frame.
                    if (this.target == null || !this.target.isAlive()) {
                        return StepResult.transition(Stage.LOOP);
                    }
                    ItemEntity item = this.target;
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
                        .unreachableTransition(Stage.LOOP)
                        .build())
                .actionStep(pickupStep)
                .targetLostTransition(Stage.LOOP)
                .timeoutTicks(COLLECT_TIMEOUT.getTicksAsInt())
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
        this.target = null;
        this.collectedByItem.clear();
    }

}
