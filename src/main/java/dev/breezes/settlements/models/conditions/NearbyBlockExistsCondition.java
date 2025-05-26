package dev.breezes.settlements.models.conditions;

import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@CustomLog
public class NearbyBlockExistsCondition<E extends Entity, B extends Block> implements ICondition<E> {
    private final double rangeHorizontal;
    private final double rangeVertical;
    private final Class<B> targetBlockClass;

    // IBlockCondition allows testing both itself and neighbours
    private final IBlockCondition extraBlockCondition;
    @Nonnull
    private List<BlockPos> targets;

    public NearbyBlockExistsCondition(double rangeHorizontal, double rangeVertical, @Nonnull Class<B> targetBlockClass, @Nullable IBlockCondition extraBlockCondition, int minimumTargetCount) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.targetBlockClass = targetBlockClass;
        this.extraBlockCondition = extraBlockCondition == null ? (blockPos, level) -> true : extraBlockCondition;
        this.targets = new ArrayList<>();

        if (minimumTargetCount < 1) {
            throw new IllegalArgumentException("Minimum target count must be at least 1");
        }
    }

    @Override
    public boolean test(@Nullable E entity) {
        if (entity == null) {
            this.targets = new ArrayList<>();
            return false;
        }
        BlockPos.MutableBlockPos mutableBlockPos = entity.blockPosition().mutable();
        Level level = entity.level();
        for (double x = -this.rangeHorizontal; x <= this.rangeHorizontal; x++) {
            for (double y = -this.rangeVertical; y <= this.rangeVertical; y++) {
                for (double z = -this.rangeHorizontal; z <= this.rangeHorizontal; z++) {
                    mutableBlockPos.set(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                    if (targetBlockClass.isInstance(level.getBlockState(mutableBlockPos).getBlock()) && this.extraBlockCondition.test(mutableBlockPos, entity.level())) {
                        targets.add(mutableBlockPos.immutable());
                    } else targets.remove(mutableBlockPos);
                }
            }
        }
        log.sensorStatus("Found {} blocks nearby", targets.size());
        return !targets.isEmpty();
    }

    public List<BlockPos> getTargets() {
        return Optional.ofNullable(this.targets)
                .orElse(Collections.emptyList());
    }
}
