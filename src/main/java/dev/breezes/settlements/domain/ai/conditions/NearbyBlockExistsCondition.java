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
import java.util.function.Predicate;

@CustomLog
public class NearbyBlockExistsCondition<E extends Entity> implements ICondition<E> {

    private final double rangeHorizontal;
    private final double rangeVertical;
    private final int minimumTargetCount;

    /**
     * Unified block-match predicate. All constructors converge here so the scan loop and
     * stillMatches re-validation share one code path instead of branching on nullable fields.
     */
    private final Predicate<BlockState> blockMatcher;

    /**
     * Optional secondary world-dependent predicate (e.g. neighbor checks). Applied only when
     * blockMatcher passes. Defaults to always-true when the caller does not need neighbor access.
     */
    private final IBlockCondition extraBlockCondition;

    @Nonnull
    private List<BlockPos> targets;

    /**
     * Convenience constructor for a single block type with an optional additional world-dependent
     * predicate (e.g. checking neighboring blocks).
     */
    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical,
                                      @Nonnull Block targetBlock,
                                      @Nullable IBlockCondition extraBlockCondition,
                                      int minimumTargetCount) {
        this(rangeHorizontal, rangeVertical,
                state -> state.is(targetBlock),
                extraBlockCondition,
                minimumTargetCount);
    }

    /**
     * Convenience constructor for a block tag with an optional additional world-dependent predicate.
     */
    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical,
                                      @Nonnull TagKey<Block> targetTag,
                                      @Nullable IBlockCondition extraBlockCondition,
                                      int minimumTargetCount) {
        this(rangeHorizontal, rangeVertical,
                state -> state.is(targetTag),
                extraBlockCondition,
                minimumTargetCount);
    }

    /**
     * Predicate-based constructor for cases where the matching logic does not map to a single
     * block or tag — e.g. polymorphic subclass checks like {@code state.getBlock() instanceof CropBlock}.
     * No additional world-dependent condition is supported here; encode all matching logic in the
     * predicate itself.
     */
    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical,
                                      @Nonnull Predicate<BlockState> blockStatePredicate,
                                      int minimumTargetCount) {
        this(rangeHorizontal, rangeVertical, blockStatePredicate, null, minimumTargetCount);
    }

    private NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical,
                                       @Nonnull Predicate<BlockState> blockMatcher,
                                       @Nullable IBlockCondition extraBlockCondition,
                                       int minimumTargetCount) {
        if (minimumTargetCount < 1) {
            throw new IllegalArgumentException("Minimum target count must be at least 1");
        }
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.blockMatcher = blockMatcher;
        this.extraBlockCondition = extraBlockCondition != null ? extraBlockCondition : (pos, level) -> true;
        this.minimumTargetCount = minimumTargetCount;
        this.targets = new ArrayList<>();
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

                    if (this.blockMatcher.test(blockState)
                            && this.extraBlockCondition.test(mutableBlockPos, level)) {
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

    public List<BlockPos> getTargets() {
        return Collections.unmodifiableList(this.targets);
    }

    /**
     * Re-runs the block predicate against a single position against the current world state.
     * <p>
     * Use this from per-behavior pick-target steps to re-validate cached candidates between loop
     * iterations, since {@link #getTargets()} returns a snapshot captured during the last
     * {@link #test(Entity)} call and can include positions whose block has since been mutated
     * (harvested, replaced, etc.).
     */
    public boolean stillMatches(@Nonnull BlockPos pos, @Nonnull Level level) {
        BlockState blockState = level.getBlockState(pos);
        return this.blockMatcher.test(blockState) && this.extraBlockCondition.test(pos, level);
    }

}
