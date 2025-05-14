package dev.breezes.settlements.entities.villager.navigation;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class VillagerPathNavigation extends GroundPathNavigation {
    public static final int FenceGateCostMalus = 2;
    public VillagerPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }
    @Override
    protected @Nonnull PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new VillagerNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);

        return new PathFinder(this.nodeEvaluator, range);
    }

    private static class VillagerNodeEvaluator extends WalkNodeEvaluator{
        private final Object2BooleanMap<AABB> collisionCache = new Object2BooleanOpenHashMap<>();

        @Override
        public void done() {
            this.collisionCache.clear();
            super.done();
        }

        @Override
        protected boolean isNeighborValid(@Nullable Node neighbor,@Nonnull Node originalNode) {
            boolean superValid = super.isNeighborValid(neighbor, originalNode);

            if (neighbor == null || neighbor.closed)
                return false;

            PathType blockPathTypes = this.getPathType(this.mob, originalNode.asBlockPos());
            if (blockPathTypes != PathType.FENCE)
                return superValid;

            BlockPos blockPos = new BlockPos(originalNode.x, originalNode.y, originalNode.z);
            if (isFenceGate(this.currentContext.getBlockState(blockPos)))
                return true;

            return superValid;
        }

        /**
         * Objective of addition to this method is to prevent villagers from using diagonal pathing when crossing fence gates
         */
        @Override
        protected boolean isDiagonalValid(Node root, @Nullable Node xNode, @Nullable Node zNode) {
            boolean isValid = super.isDiagonalValid(root, xNode, zNode);

            if (isValid && Stream.of(root, xNode, zNode).anyMatch((node -> node.type == PathType.FENCE && node.costMalus == FenceGateCostMalus))) {
                return false;
            }

            return isValid;
        }

        @Override
        @Nullable
        protected Node findAcceptedNode(int x, int y, int z, int maxYStep, double prevFeetY, Direction direction, PathType pathType) {
            Node node = null;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            double d = this.getFloorLevel(mutableBlockPos.set(x, y, z));
            if (d - prevFeetY > this.getMobJumpHeight()) {
                return null;
            }
            PathType currNodeType = this.getPathType(this.mob, mutableBlockPos);
            float penalty = this.mob.getPathfindingMalus(currNodeType);
            double e = (double) this.mob.getBbWidth() / 2.0D;
            if (penalty >= 0.0F) {
                node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, penalty);
            }

            if (doesBlockHavePartialCollision(pathType) && node != null && node.costMalus >= 0.0F && !this.canReachWithoutCollision(node)) {
                node = null;
            }

            // >> Custom code 1 begin -- recognize block opposite of fence gate
            // - aka allows the villager to see blocks "over" fence gates
            Direction opposite = direction.getOpposite();
            if (node == null && pathType == PathType.FENCE
                    && isFenceGate(this.currentContext.level().getBlockState(new BlockPos(x + opposite.getStepX(), y, z + opposite.getStepZ())))) {
                node = this.getNode(x, y, z);
                node.type = currNodeType;
                node.costMalus = currNodeType.getMalus();
            }
            // << Custom code 1 ends

            if (currNodeType != PathType.WALKABLE && (!this.isAmphibious() || currNodeType != PathType.WATER)) {
                if ((node == null || node.costMalus < 0.0F) && maxYStep > 0 && (currNodeType != PathType.FENCE || this.canWalkOverFences()) && currNodeType != PathType.UNPASSABLE_RAIL && currNodeType != PathType.TRAPDOOR && currNodeType != PathType.POWDER_SNOW) {
                    node = this.findAcceptedNode(x, y + 1, z, maxYStep - 1, prevFeetY, direction, pathType);
                    if (node != null && (node.type == PathType.OPEN || node.type == PathType.WALKABLE) && this.mob.getBbWidth() < 1.0F) {
                        double g = (double) (x - direction.getStepX()) + 0.5D;
                        double h = (double) (z - direction.getStepZ()) + 0.5D;
                        AABB aABB = new AABB(g - e, this.getFloorLevel(mutableBlockPos.set(g, y + 1, h)) + 0.001D, h - e, g + e,
                                (double) this.mob.getBbHeight() + this.getFloorLevel(mutableBlockPos.set(node.x, node.y, node.z)) - 0.002D, h + e);
                        if (this.hasCollisions(aABB)) {
                            node = null;
                        }
                    }
                }

                if (!this.isAmphibious() && currNodeType == PathType.WATER && !this.canFloat()) {
                    if (this.getPathType(this.mob, new BlockPos.MutableBlockPos(x, y - 1, z)) != PathType.WATER) {
                        return node;
                    }

                    while (y > this.mob.level().getMinBuildHeight()) {
                        --y;
                        currNodeType = this.getPathType(this.mob, mutableBlockPos);
                        if (currNodeType != PathType.WATER) {
                            return node;
                        }

                        node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, this.mob.getPathfindingMalus(currNodeType));
                    }
                }

                if (currNodeType == PathType.OPEN) {
                    int i = 0;
                    int j = y;

                    while (currNodeType == PathType.OPEN) {
                        --y;
                        if (y < this.mob.level().getMinBuildHeight()) {
                            return this.getBlockedNode(x, j, z);
                        }

                        if (i++ >= this.mob.getMaxFallDistance()) {
                            return this.getBlockedNode(x, y, z);
                        }

                        currNodeType = this.getPathType(this.mob, mutableBlockPos);
                        penalty = this.mob.getPathfindingMalus(currNodeType);
                        if (currNodeType != PathType.OPEN && penalty >= 0.0F) {
                            node = this.getNodeAndUpdateCostToMax(x, y, z, currNodeType, penalty);
                            break;
                        }

                        if (penalty < 0.0F) {
                            return this.getBlockedNode(x, y, z);
                        }
                    }
                }

                // >> Custom code 2 begin -- recognize fence gate as "walkable into"
                // - aka allows the villager to pathfind through fence gate blocks
                if (node == null && currNodeType == PathType.FENCE && isFenceGate(this.currentContext.level().getBlockState(new BlockPos(x, y, z)))) {
                    node = this.getNode(x, y, z);
                    node.type = PathType.FENCE;
                    node.costMalus = FenceGateCostMalus;
                }
                // << Custom code 2 ends

                if (doesBlockHavePartialCollision(currNodeType) && node == null) {
                    node = this.getNode(x, y, z);
                    node.closed = true;
                    node.type = currNodeType;
                    node.costMalus = currNodeType.getMalus();
                }

            }
            return node;
        }
        private double getMobJumpHeight() {
            return Math.max(1.125D, this.mob.maxUpStep());
        }

        private Node getNodeAndUpdateCostToMax(int x, int y, int z, PathType pathType, float malus) {
            Node node = this.getNode(x, y, z);
            node.type = pathType;
            node.costMalus = Math.max(node.costMalus, malus);
            return node;
        }

        private static boolean doesBlockHavePartialCollision(PathType pathType) {
            return pathType == PathType.FENCE || pathType == PathType.DOOR_WOOD_CLOSED || pathType == PathType.DOOR_IRON_CLOSED;
        }

        private boolean canReachWithoutCollision(Node node) {
            AABB aabb = this.mob.getBoundingBox();
            Vec3 vec3 = new Vec3((double)node.x - this.mob.getX() + aabb.getXsize() / (double)2.0F, (double)node.y - this.mob.getY() + aabb.getYsize() / (double)2.0F, (double)node.z - this.mob.getZ() + aabb.getZsize() / (double)2.0F);
            int i = Mth.ceil(vec3.length() / aabb.getSize());
            vec3 = vec3.scale(1.0F / (float)i);

            for(int j = 1; j <= i; ++j) {
                aabb = aabb.move(vec3);
                if (this.hasCollisions(aabb)) {
                    return false;
                }
            }

            return true;
        }

        private boolean hasCollisions(AABB boundingBox) {
            return this.collisionCache.computeIfAbsent(boundingBox, (p_330163_) -> !this.currentContext.level().noCollision(this.mob, boundingBox));
        }

        private Node getBlockedNode(int x, int y, int z) {
            Node node = this.getNode(x, y, z);
            node.type = PathType.BLOCKED;
            node.costMalus = -1.0F;
            return node;
        }

        private static boolean isFenceGate(BlockState state) {
            return state.is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
        }
    }
}
