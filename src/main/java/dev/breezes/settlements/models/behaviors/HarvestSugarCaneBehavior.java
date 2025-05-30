package dev.breezes.settlements.models.behaviors;

import com.google.common.collect.Lists;
import dev.breezes.settlements.annotations.configurations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbySugarCaneExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

@CustomLog
public class HarvestSugarCaneBehavior extends AbstractInteractAtTargetBehavior{
    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 60, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 240, min = 1)
    private static int behaviorCooldownMax;

    @IntegerConfig(identifier = "scan_range_horizontal",
            description = "Horizontal range (in blocks) to scan for nearby sugar cane",
            defaultValue = 4, min = 1, max = 16)
    private static int scanRangeHorizontal;
    @IntegerConfig(identifier = "scan_range_vertical",
            description = "Vertical range (in blocks) to scan for nearby sugar cane",
            defaultValue = 2, min = 0, max = 3)
    private static int scanRangeVertical;
    private BlockPos sugarCanePos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSugarCaneAroundVillager = Lists.newArrayList();
    private final NearbySugarCaneExistsCondition<BaseVillager> nearbySugarCaneExistsCondition;

    public HarvestSugarCaneBehavior() {
        super(log,
              RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin),
              Ticks.seconds(preconditionCheckCooldownMax)),
              RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin),
              Ticks.seconds(behaviorCooldownMax)),
              Tickable.of(Ticks.seconds(0)));

        this.nearbySugarCaneExistsCondition = NearbySugarCaneExistsCondition.builder()
                .rangeHorizontal(scanRangeHorizontal)
                .rangeVertical(scanRangeVertical)
                .build();
        this.preconditions.add(this.nearbySugarCaneExistsCondition);
        this.sugarCanePos = null;
    }

    @Override
    public void doStart(@Nonnull Level level, @Nonnull BaseVillager villager) {
        this.validSugarCaneAroundVillager = nearbySugarCaneExistsCondition.getTargets();
        this.sugarCanePos = getRandomPosition(level);
    }

    private BlockPos getRandomPosition(Level level) {
        return this.validSugarCaneAroundVillager.get(level.getRandom().nextInt(this.validSugarCaneAroundVillager.size()));
    }
    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosTracker(this.sugarCanePos), 0.5F, 0));
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(this.sugarCanePos));
    }

    @Override
    public void doStop(@Nonnull Level level, @Nonnull BaseVillager entity) {
        entity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.sugarCanePos = null;
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        level.destroyBlock(sugarCanePos, true, villager);
        this.requestStop();
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        this.timeWorkedSoFar++;
    }

    @Override
    protected boolean hasTarget(@Nonnull Level level, @Nonnull BaseVillager villager) {
        return this.sugarCanePos != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.sugarCanePos.closerToCenterThan(villager.getEyePosition(), 1.0);
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        return this.timeWorkedSoFar < 400;
    }
}
