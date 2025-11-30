package dev.breezes.settlements.models.behaviors;

import com.google.common.collect.Lists;
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
public class HarvestSugarCaneBehavior extends AbstractInteractAtTargetBehavior {

    private final HarvestSugarCaneConfig config;
    private BlockPos sugarCanePos;
    private int timeWorkedSoFar;
    private List<BlockPos> validSugarCaneAroundVillager = Lists.newArrayList();
    private final NearbySugarCaneExistsCondition<BaseVillager> nearbySugarCaneExistsCondition;

    public HarvestSugarCaneBehavior(HarvestSugarCaneConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())),
                Tickable.of(Ticks.seconds(0)));
        this.config = config;

        this.nearbySugarCaneExistsCondition = NearbySugarCaneExistsCondition.builder()
                .rangeHorizontal(config.scanRangeHorizontal())
                .rangeVertical(config.scanRangeVertical())
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
