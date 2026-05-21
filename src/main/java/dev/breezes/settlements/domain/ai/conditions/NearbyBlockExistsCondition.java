package dev.breezes.settlements.domain.ai.conditions;

import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CustomLog
public class NearbyBlockExistsCondition<E extends Entity> implements ICondition<E> {

    private final double rangeHorizontal;
    private final double rangeVertical;
    @Nullable
    private Block targetBlock;
    @Nullable
    private TagKey<Block> targetTag;
    private final int minimumTargetCount;

    // IBlockCondition allows testing both itself and neighbors
    private final IBlockCondition extraBlockCondition;

    @Nonnull
    private List<BlockPos> targets;

    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical, @Nonnull Block targetBlock, @Nullable IBlockCondition extraBlockCondition, int minimumTargetCount) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.targetBlock = targetBlock;
        this.extraBlockCondition = extraBlockCondition == null ? (blockPos, level) -> true : extraBlockCondition;
        this.minimumTargetCount = minimumTargetCount;
        this.targets = new ArrayList<>();

        if (minimumTargetCount < 1) {
            throw new IllegalArgumentException("Minimum target count must be at least 1");
        }
    }

    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical, @Nonnull TagKey<Block> targetBlocks, @Nullable IBlockCondition extraBlockCondition, int minimumTargetCount) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.targetTag = targetBlocks;
        this.extraBlockCondition = extraBlockCondition == null ? (blockPos, level) -> true : extraBlockCondition;
        this.minimumTargetCount = minimumTargetCount;
        this.targets = new ArrayList<>();

        if (minimumTargetCount < 1) {
            throw new IllegalArgumentException("Minimum target count must be at least 1");
        }
    }

    @Override
    public boolean test(@Nullable E entity) {
        this.targets = new ArrayList<>();
        if (entity == null) {
            return false;
        }
        BlockPos.MutableBlockPos mutableBlockPos = entity.blockPosition().mutable();
        Level level = entity.level();
        for (double x = -this.rangeHorizontal; x <= this.rangeHorizontal; x++) {
            for (double y = -this.rangeVertical; y <= this.rangeVertical; y++) {
                for (double z = -this.rangeHorizontal; z <= this.rangeHorizontal; z++) {
                    mutableBlockPos.set(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                    BlockState blockState = level.getBlockState(mutableBlockPos);

                    // Only one of targetBlock/targetTag is set depending on which constructor was used
                    boolean blockMatches = (this.targetBlock != null && blockState.is(this.targetBlock))
                            || (this.targetTag != null && blockState.is(this.targetTag));
                    if (blockMatches && this.extraBlockCondition.test(mutableBlockPos, entity.level())) {
                        targets.add(mutableBlockPos.immutable());
                        if (this.targets.size() >= this.minimumTargetCount) {
                            log.sensorStatus("Found at least {} blocks nearby", this.minimumTargetCount);
                            return true;
                        }
                    }
                }
            }
        }
        log.sensorStatus("Found {} blocks nearby", targets.size());
        return this.targets.size() >= this.minimumTargetCount;
    }

    @Nonnull
    public List<BlockPos> getTargets() {
        return Collections.unmodifiableList(this.targets);
    }

    /**
     * Re-runs the block predicate against a single position against the current world state.
     * <p>
     * Use this from per-behavior pick-target steps to re-validate cached candidates between loop iterations,
     * since {@link #getTargets()} returns a snapshot captured during the last {@link #test(Entity)} call
     * and can include positions whose block has since been mutated (harvested, replaced, etc.).
     */
    public boolean stillMatches(@Nonnull BlockPos pos, @Nonnull Level level) {
        BlockState blockState = level.getBlockState(pos);
        boolean blockMatches = (this.targetBlock != null && blockState.is(this.targetBlock))
                || (this.targetTag != null && blockState.is(this.targetTag));
        return blockMatches && this.extraBlockCondition.test(pos, level);
    }

}
