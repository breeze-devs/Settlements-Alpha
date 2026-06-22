package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.Builder;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Condition that checks for nearby water source blocks with an accessible shore position.
 * <p>
 * Finds water source blocks in range and identifies a walkable shore block adjacent to the water.
 * Both the water target (where to aim the hook) and shore position (where to stand) are exposed.
 */
@CustomLog
public class NearbyWaterExistsCondition<E extends Entity> implements ICondition<E> {

    private final int rangeHorizontal;
    private final int rangeVertical;
    private final int completionRange;

    @Nullable
    private BlockPos waterTarget;
    @Nullable
    private BlockPos shorePosition;

    @Builder
    public NearbyWaterExistsCondition(int rangeHorizontal, int rangeVertical, int completionRange) {
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.completionRange = completionRange;

        this.waterTarget = null;
        this.shorePosition = null;
    }

    @Override
    public boolean test(@Nullable E entity) {
        this.waterTarget = null;
        this.shorePosition = null;

        if (entity == null) {
            return false;
        }

        Level level = entity.level();
        BlockPos entityPos = entity.blockPosition();
        int originX = entityPos.getX();
        int originY = entityPos.getY();
        int originZ = entityPos.getZ();

        BlockPos.MutableBlockPos waterMutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos shoreMutable = new BlockPos.MutableBlockPos();

        for (int horizontalRadius = 0; horizontalRadius <= this.rangeHorizontal; horizontalRadius++) {
            for (int verticalOffsetIndex = 0; verticalOffsetIndex <= this.rangeVertical * 2; verticalOffsetIndex++) {
                int yOffset = getVerticalOffset(verticalOffsetIndex);
                if (Math.abs(yOffset) > this.rangeVertical) {
                    continue;
                }

                if (horizontalRadius == 0) {
                    if (checkWaterCandidate(entity, level, originX, originY + yOffset, originZ, waterMutable, shoreMutable)) {
                        return true;
                    }
                    continue;
                }

                int minX = originX - horizontalRadius;
                int maxX = originX + horizontalRadius;
                int minZ = originZ - horizontalRadius;
                int maxZ = originZ + horizontalRadius;

                for (int x = minX; x <= maxX; x++) {
                    if (checkWaterCandidate(entity, level, x, originY + yOffset, minZ, waterMutable, shoreMutable)) {
                        return true;
                    }
                    if (checkWaterCandidate(entity, level, x, originY + yOffset, maxZ, waterMutable, shoreMutable)) {
                        return true;
                    }
                }

                for (int z = minZ + 1; z < maxZ; z++) {
                    if (checkWaterCandidate(entity, level, minX, originY + yOffset, z, waterMutable, shoreMutable)) {
                        return true;
                    }
                    if (checkWaterCandidate(entity, level, maxX, originY + yOffset, z, waterMutable, shoreMutable)) {
                        return true;
                    }
                }
            }
        }

        log.sensorStatus("No accessible water found nearby");
        return false;
    }

    private boolean checkWaterCandidate(@Nonnull Entity entity,
                                        @Nonnull Level level,
                                        int x,
                                        int y,
                                        int z,
                                        @Nonnull BlockPos.MutableBlockPos waterMutable,
                                        @Nonnull BlockPos.MutableBlockPos shoreMutable) {
        waterMutable.set(x, y, z);

        FluidState fluidState = level.getFluidState(waterMutable);
        if (!fluidState.is(FluidTags.WATER) || !fluidState.isSource()) {
            return false;
        }

        if (!findShorePosition(entity, level, waterMutable, shoreMutable)) {
            return false;
        }

        if (entity instanceof ISettlementsBrainEntity brainEntity
                && !brainEntity.getNavigationManager().canReach(Location.of(shoreMutable, level), this.completionRange)) {
            return false;
        }

        this.waterTarget = waterMutable.immutable();
        this.shorePosition = shoreMutable.immutable();

        log.sensorStatus("Found water at {} with shore at {}", this.waterTarget, this.shorePosition);
        return true;
    }

    private static int getVerticalOffset(int index) {
        if (index == 0) {
            return 0;
        }
        int distance = (index + 1) / 2;
        return index % 2 == 1 ? distance : -distance;
    }

    /**
     * Finds a walkable shore position adjacent to the given water block.
     * A valid shore position has a solid block below and air above (villager can stand there).
     */
    private static boolean findShorePosition(@Nonnull Entity entity,
                                             @Nonnull Level level,
                                             @Nonnull BlockPos.MutableBlockPos waterPos,
                                             @Nonnull BlockPos.MutableBlockPos shoreMutable) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // Set the shoreMutable to the block directly next to the water
            shoreMutable.setWithOffset(waterPos, direction);

            // Check floor
            if (!level.getBlockState(shoreMutable).entityCanStandOn(level, shoreMutable, entity)) {
                continue;
            }

            // Move mutable UP and check the first headroom block (Legs)
            shoreMutable.move(Direction.UP);
            if (!level.getBlockState(shoreMutable).getCollisionShape(level, shoreMutable).isEmpty()) {
                continue;
            }

            // Move mutable UP and check the second headroom block (Head)
            shoreMutable.move(Direction.UP);
            if (!level.getBlockState(shoreMutable).getCollisionShape(level, shoreMutable).isEmpty()) {
                continue;
            }

            // We found a valid shore!
            // shoreMutable is currently at Head level. Move it DOWN once so it represents
            // the block the entity stands in (Leg level), which is what we want to return.
            shoreMutable.move(Direction.DOWN);
            return true;
        }
        return false;
    }

    public Optional<BlockPos> getWaterTarget() {
        return Optional.ofNullable(this.waterTarget);
    }

    public Optional<BlockPos> getShorePosition() {
        return Optional.ofNullable(this.shorePosition);
    }

}
