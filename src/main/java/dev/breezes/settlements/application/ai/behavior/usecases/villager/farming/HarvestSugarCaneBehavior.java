package dev.breezes.settlements.application.ai.behavior.usecases.villager.farming;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.ai.conditions.NearbySugarCaneExistsCondition;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Ticks;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class HarvestSugarCaneBehavior extends StateMachineBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_SUGAR_CANE,
        END;
    }

    @Nullable
    private BlockPos sugarCanePos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSugarCaneAroundVillager;
    private final NearbySugarCaneExistsCondition<BaseVillager> nearbySugarCaneExistsCondition;

    public HarvestSugarCaneBehavior(HarvestSugarCaneConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));

        this.nearbySugarCaneExistsCondition = NearbySugarCaneExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbySugarCaneExistsCondition);

        this.sugarCanePos = null;
        this.timeWorkedSoFar = 0;
        this.validSugarCaneAroundVillager = new ArrayList<>();

        this.initializeStateMachine(this.createControlStep(), HarvestStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("HarvestSugarCaneBehavior")
                .initialStage(HarvestStage.HARVEST_SUGAR_CANE)
                .stageStepMap(Map.of(HarvestStage.HARVEST_SUGAR_CANE, this.createHarvestStep()))
                .nextStage(HarvestStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep createHarvestStep() {
        return StayCloseStep.builder()
                .closeEnoughDistance(1.0)
                .navigateStep(new NavigateToTargetStep(0.5f, 0))
                .actionStep(context -> {
                    if (this.sugarCanePos == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = context.getInitiator().getMinecraftEntity();
                    villager.level().destroyBlock(this.sugarCanePos, true, villager);
                    return StepResult.complete();
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        this.timeWorkedSoFar = 0;

        this.validSugarCaneAroundVillager = new ArrayList<>(this.nearbySugarCaneExistsCondition.getTargets());
        if (this.validSugarCaneAroundVillager.isEmpty()) {
            this.requestStop();
            return;
        }

        this.sugarCanePos = getRandomPosition(world);
        context.setState(BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.sugarCanePos, world), world.getBlockState(this.sugarCanePos)))));
    }

    private BlockPos getRandomPosition(Level world) {
        return this.validSugarCaneAroundVillager
                .get(world.getRandom().nextInt(this.validSugarCaneAroundVillager.size()));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        this.timeWorkedSoFar = 0;
        this.sugarCanePos = null;
        this.validSugarCaneAroundVillager = new ArrayList<>();
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        this.timeWorkedSoFar += delta;
        return super.tickContinueConditions(delta, world, entity) && this.timeWorkedSoFar < 400;
    }

}
