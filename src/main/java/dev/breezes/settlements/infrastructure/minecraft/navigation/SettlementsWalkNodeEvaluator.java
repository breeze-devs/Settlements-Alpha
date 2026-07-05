package dev.breezes.settlements.infrastructure.minecraft.navigation;

import dev.breezes.settlements.domain.world.blocks.TraversableBarrier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import javax.annotation.Nonnull;

/**
 * Makes closed fence gates path-able exactly like closed wooden doors, and hardens the vanilla
 * door-tag logic (a closed door outside the operable tag would otherwise be routed into and never
 * opened -- a deadlock).
 * <p>
 * Only the instance {@link #getPathType(PathfindingContext, int, int, int)} is overridden -- not
 * {@code getPathTypeWithinMobBB}'s static classification -- so the converted {@code WALKABLE_DOOR}
 * is never written into the server-level {@code PathTypeCache} (which only stores raw static types),
 * and can never leak into iron golems or other mobs sharing that cache.
 * <p>
 * {@code findAcceptedNode}/{@code isNeighborValid}/{@code isDiagonalValid} are untouched --
 * {@code WALKABLE_DOOR} already routes a gate through the identical door machinery those methods use.
 */
public class SettlementsWalkNodeEvaluator extends WalkNodeEvaluator {

    // Reused across getPathType calls within a single pathfinding job to avoid a BlockPos
    // allocation per evaluated cell. Safe because one evaluator instance belongs to exactly one
    // Mob's navigation and is never touched concurrently by more than one path computation.
    private final BlockPos.MutableBlockPos reusablePos = new BlockPos.MutableBlockPos();

    @Override
    public PathType getPathType(@Nonnull PathfindingContext context, int x, int y, int z) {
        PathType base = super.getPathType(context, x, y, z);
        if (!this.canOpenDoors() || !this.canPassDoors()) {
            return base;
        }

        // Gate the extra getBlockState read behind these two base types -- they only occur near
        // fences/gates/walls/doors, so this never touches the general pathfinding hot path.
        // Expand to more types if needed in the future
        if (base == PathType.FENCE) {
            return convertClosedGate(context, x, y, z, base);
        }
        if (base == PathType.DOOR_WOOD_CLOSED) {
            return hardenOperableDoor(context, x, y, z, base);
        }
        return base;
    }

    private PathType convertClosedGate(PathfindingContext context, int x, int y, int z, PathType base) {
        BlockState state = context.getBlockState(this.reusablePos.set(x, y, z));
        TraversableBarrier barrier = TraversableBarrier.resolve(state);
        if (barrier == TraversableBarrier.FENCE_GATE && !barrier.isOpen(state)) {
            return PathType.WALKABLE_DOOR;
        }
        return base;
    }

    private PathType hardenOperableDoor(PathfindingContext context, int x, int y, int z, PathType base) {
        BlockState state = context.getBlockState(this.reusablePos.set(x, y, z));
        if (TraversableBarrier.resolve(state) == null) {
            return PathType.DOOR_IRON_CLOSED;
        }
        return base;
    }

}
