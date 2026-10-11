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
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Walks a fletcher to each stuck projectile its latest scan remembered, and picks it up as a player would.
 */
@CustomLog
public class RetrieveProjectilesBehavior extends VillagerStateMachineBehavior {

    // A projectile stuck in a wall can sit two blocks above the feet of a villager standing beneath it
    private static final double CLOSE_ENOUGH_DISTANCE = 2.5D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;

    private static final int COLLECT_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final int MAX_PROJECTILES_PER_RUN = 5;

    private enum Stage implements StageKey {
        RESOLVE, COLLECT, LOOP, END
    }

    private final Map<String, Integer> collectedByItem = new HashMap<>();

    @Nullable
    private AbstractArrow target;

    public RetrieveProjectilesBehavior(@Nonnull RetrieveProjectilesConfig config, @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);

        this.preconditions.add(ICondition.named("RetrievableProjectilesRemembered", villager -> villager != null
                && villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES).isPresent()));

        this.target = null;

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.RESOLVE, this.createResolveStep());
        stageMap.put(Stage.COLLECT, this.createCollectStep());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("RetrieveProjectilesLoopBack")
                .loopBackTo(Stage.RESOLVE)
                .completionTransition(Stage.END)
                .maxIterationsResolver(ctx -> MAX_PROJECTILES_PER_RUN)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("RetrieveProjectilesBehavior")
                .initialStage(Stage.RESOLVE)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createResolveStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("ResolveProjectile")
                .action(ctx -> {
                    this.target = takeNextRemembered(ctx.getInitiator());
                    if (this.target == null) {
                        return StepResult.transition(Stage.END);
                    }

                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    return StepResult.transition(Stage.COLLECT);
                })
                .build();
    }

    /**
     * Removes the next projectile that still exists from the remembered list and returns it, or null once none is left.
     * A projectile is removed regardless of if it's collected.
     */
    @Nullable
    private static AbstractArrow takeNextRemembered(@Nonnull BaseVillager villager) {
        List<AbstractArrow> remembered = new ArrayList<>(villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES)
                .orElse(List.of()));

        AbstractArrow next = null;
        while (next == null && !remembered.isEmpty()) {
            AbstractArrow candidate = remembered.removeFirst();
            if (candidate.isAlive()) {
                next = candidate;
            }
        }

        if (remembered.isEmpty()) {
            villager.getSettlementsBrain().clearMemory(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES);
        } else {
            villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES, List.copyOf(remembered));
        }
        return next;
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
                    // A player or another fletcher may have taken it during the animation
                    if (this.target == null || !this.target.isAlive()) {
                        return StepResult.transition(Stage.LOOP);
                    }
                    this.pickUp(ctx, this.target);
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
                .timeoutTicks(COLLECT_TIMEOUT_TICKS)
                .timeoutTransition(Stage.END)
                .build();
    }

    private void pickUp(@Nonnull BehaviorContext<BaseVillager> ctx, @Nonnull AbstractArrow projectile) {
        BaseVillager villager = ctx.getInitiator();
        ItemStack retrieved = projectile.getPickupItemStackOrigin().copy();
        int count = retrieved.getCount();
        villager.getSettlementsInventory().add(retrieved);
        // Taken before the discard, since no pickup is sent to clients for a removed entity
        villager.take(projectile, count);
        projectile.discard();

        String itemId = BuiltInRegistries.ITEM.getKey(retrieved.getItem()).getPath();
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
