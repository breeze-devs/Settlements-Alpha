package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

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
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.NearbyBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class RingBellBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2;

    private enum RingBellStage implements StageKey {
        RING_BELL,
        END;
    }

    private final NearbyBlockExistsCondition<BaseVillager> nearbyBellExistsCondition;

    @Nullable
    private BlockPos bellPos;

    public RingBellBehavior(@Nonnull RingBellConfig config,
                            @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.nearbyBellExistsCondition = new NearbyBlockExistsCondition<>(config.scanRangeHorizontal(), config.scanRangeVertical(),
                Blocks.BELL, null, 1);
        this.preconditions.add(this.nearbyBellExistsCondition);

        this.bellPos = null;

        this.initializeStateMachine(this.createControlStep(), RingBellStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("RingBellBehavior")
                .initialStage(RingBellStage.RING_BELL)
                .stageStepMap(Map.of(RingBellStage.RING_BELL, this.createRingBellStep()))
                .nextStage(RingBellStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createRingBellStep() {
        TimeBasedStep<BaseVillager> actionStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    context.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    if (this.bellPos == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    BlockState bellState = villager.level().getBlockState(this.bellPos);
                    if (!bellState.is(Blocks.BELL)) {
                        return StepResult.fail("Bell not found at " + this.bellPos);
                    }

                    ((BellBlock) bellState.getBlock()).attemptToRing(villager, villager.level(), this.bellPos, null);

                    BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.BELL_RUNG, null);
                    outcome.markSucceeded();
                    context.declarePrimaryDeed(outcome);
                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.RUN, 1))
                .actionStep(actionStep)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        List<BlockPos> bells = new ArrayList<>(this.nearbyBellExistsCondition.getTargets());
        if (bells.isEmpty()) {
            this.requestStop("No bell found within range");
            return;
        }

        this.bellPos = bells.get(world.getRandom().nextInt(bells.size()));
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                PhysicalBlock.of(Location.of(this.bellPos, world), world.getBlockState(this.bellPos)))));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.bellPos != null && world.getBlockState(this.bellPos).is(Blocks.BELL);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
        this.bellPos = null;
    }

}
