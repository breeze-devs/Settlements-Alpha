package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.NearbySugarCaneExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CustomLog
public class HarvestSugarCaneBehavior extends BaseVillagerStagedBehavior {

    private enum HarvestStage implements StageKey {
        HARVEST_SUGAR_CANE,
        END;
    }

    private final HarvestSugarCaneConfig config;
    private final StagedStep controlStep;

    @Nullable
    private BlockPos sugarCanePos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSugarCaneAroundVillager;
    private final NearbySugarCaneExistsCondition<BaseVillager> nearbySugarCaneExistsCondition;

    @Nullable
    private BehaviorContext context;

    public HarvestSugarCaneBehavior(HarvestSugarCaneConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));
        this.config = config;

        this.nearbySugarCaneExistsCondition = NearbySugarCaneExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
                .build();
        this.preconditions.add(this.nearbySugarCaneExistsCondition);

        this.sugarCanePos = null;
        this.timeWorkedSoFar = 0;
        this.validSugarCaneAroundVillager = new ArrayList<>();
        this.context = null;

        this.controlStep = StagedStep.builder()
                .name("HarvestSugarCaneBehavior")
                .initialStage(HarvestStage.HARVEST_SUGAR_CANE)
                .stageStepMap(Map.of(
                        HarvestStage.HARVEST_SUGAR_CANE, this.createHarvestStep()
                ))
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
    public void doStart(@Nonnull Level level, @Nonnull BaseVillager villager) {
        this.context = new BehaviorContext(villager);
        this.timeWorkedSoFar = 0;

        this.validSugarCaneAroundVillager = new ArrayList<>(this.nearbySugarCaneExistsCondition.getTargets());
        if (this.validSugarCaneAroundVillager.isEmpty()) {
            this.requestStop();
            return;
        }

        this.sugarCanePos = getRandomPosition(level);
        this.context.setState(
                BehaviorStateType.TARGET,
                TargetState.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(this.sugarCanePos, level), level.getBlockState(this.sugarCanePos))))
        );
    }

    private BlockPos getRandomPosition(Level level) {
        return this.validSugarCaneAroundVillager.get(level.getRandom().nextInt(this.validSugarCaneAroundVillager.size()));
    }

    @Override
    public void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.context == null) {
            throw new StopBehaviorException("Behavior context is null");
        }

        this.timeWorkedSoFar += delta;
        StepResult result = this.controlStep.tick(this.context);
        this.handleStepResult(result, HarvestStage.END, "HarvestSugarCaneBehavior");
    }

    @Override
    public void doStop(@Nonnull Level level, @Nonnull BaseVillager entity) {
        entity.getNavigationManager().stop();
        this.timeWorkedSoFar = 0;
        this.sugarCanePos = null;
        this.validSugarCaneAroundVillager = new ArrayList<>();
        this.context = null;
        this.controlStep.reset();
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        return super.tickContinueConditions(delta, world, entity) && this.timeWorkedSoFar < 400;
    }
}
