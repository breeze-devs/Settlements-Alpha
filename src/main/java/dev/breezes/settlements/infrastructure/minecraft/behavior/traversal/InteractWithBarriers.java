package dev.breezes.settlements.infrastructure.minecraft.behavior.traversal;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.world.blocks.TraversableBarrier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Unified door + fence-gate traversal behavior, generalizing vanilla {@code InteractWithDoor}
 * through {@link TraversableBarrier} and fixing two leaks along the way:
 * <ul>
 *     <li>vanilla only remembers a <em>next</em>-node door inside its {@code !isOpen} branch, so an
 *     already-open traversed door is never closed -- here every scanned barrier is remembered
 *     unconditionally, open or closed;</li>
 *     <li>vanilla's close-sweep only runs while a path is present and off cooldown, so a door left
 *     open when the path ends or is replaced is stranded -- this sweep runs every tick regardless.</li>
 * </ul>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InteractWithBarriers {

    private static final ClockTicks STATIONARY_RESCAN_INTERVAL = ClockTicks.seconds(0.5);

    // Mirrors vanilla InteractWithDoor's SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN.
    private static final double FORGET_BARRIER_BEYOND_DISTANCE = 3.0D;

    // Mirrors vanilla InteractWithDoor's MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS.
    private static final double HAND_OFF_DISTANCE = 2.0D;

    public static BehaviorControl<LivingEntity> create() {
        ScanThrottle throttle = new ScanThrottle();
        return BehaviorBuilder.create(instance -> instance.group(
                        instance.registered(MemoryModuleType.PATH),
                        instance.registered(MemoryTypeRegistry.BARRIERS_TO_CLOSE.getModuleType()),
                        instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES))
                .apply(instance, (pathMemory, barriersMemory, nearestLivingEntitiesMemory) -> (level, entity, gameTime) ->
                        tick(level, entity, throttle,
                                instance.tryGet(pathMemory),
                                barriersMemory,
                                instance.tryGet(barriersMemory),
                                instance.tryGet(nearestLivingEntitiesMemory))));
    }

    private static boolean tick(ServerLevel level,
                                LivingEntity entity,
                                ScanThrottle throttle,
                                Optional<Path> pathOptional,
                                MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> barriersMemory,
                                Optional<Set<GlobalPos>> barrierPositions,
                                Optional<List<LivingEntity>> nearestLivingEntities) {
        boolean activeTraversal = false;
        Node previous = null;
        Node next = null;

        if (pathOptional.isPresent() && !pathOptional.get().notStarted() && !pathOptional.get().isDone()) {
            Path path = pathOptional.get();
            activeTraversal = true;
            previous = path.getPreviousNode();
            next = path.getNextNode();

            int nodeIndex = path.getNextNodeIndex();
            if (throttle.shouldScan(nodeIndex)) {
                barrierPositions = scanNode(level, entity, previous, barriersMemory, barrierPositions);
                barrierPositions = scanNode(level, entity, next, barriersMemory, barrierPositions);
                if (nodeIndex + 1 < path.getNodeCount()) {
                    barrierPositions = scanNode(level, entity, path.getNode(nodeIndex + 1), barriersMemory, barrierPositions);
                }
            }
        }

        // Always close barriers even when it was previously open
        boolean anyClosed = closeBarriersTraversed(level, entity, previous, next, barrierPositions, nearestLivingEntities);

        return activeTraversal || anyClosed;
    }

    private static Optional<Set<GlobalPos>> scanNode(ServerLevel level,
                                                     LivingEntity entity,
                                                     @Nullable Node node,
                                                     MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> barriersMemory,
                                                     Optional<Set<GlobalPos>> barrierPositions) {
        if (node == null) {
            return barrierPositions;
        }

        BlockPos pos = node.asBlockPos();
        BlockState state = level.getBlockState(pos);
        TraversableBarrier barrier = TraversableBarrier.resolve(state);
        if (barrier == null) {
            return barrierPositions;
        }

        if (!barrier.isOpen(state)) {
            barrier.setOpen(entity, level, state, pos, true);
        }

        // Remember barriers unconditionally, open or closed
        return Optional.of(rememberBarrierToClose(barriersMemory, barrierPositions, level, pos));
    }

    private static Set<GlobalPos> rememberBarrierToClose(MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> barriersMemory,
                                                         Optional<Set<GlobalPos>> barrierPositions,
                                                         ServerLevel level,
                                                         BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos);
        return barrierPositions.map(set -> {
            set.add(globalPos);
            return set;
        }).orElseGet(() -> {
            Set<GlobalPos> set = Sets.newHashSet(globalPos);
            barriersMemory.set(set);
            return set;
        });
    }

    /**
     * Ports vanilla's {@code closeDoorsThatIHaveOpenedOrPassedThrough}, generalized via {@link TraversableBarrier}.
     */
    private static boolean closeBarriersTraversed(ServerLevel level,
                                                  LivingEntity entity,
                                                  @Nullable Node previous,
                                                  @Nullable Node next,
                                                  Optional<Set<GlobalPos>> barrierPositions,
                                                  Optional<List<LivingEntity>> nearestLivingEntities) {
        if (barrierPositions.isEmpty()) {
            return false;
        }

        boolean anyClosed = false;
        Iterator<GlobalPos> iterator = barrierPositions.get().iterator();
        while (iterator.hasNext()) {
            GlobalPos globalPos = iterator.next();
            BlockPos pos = globalPos.pos();

            // Still the node the villager is standing between -- pos compare only, no block read
            if ((previous != null && previous.asBlockPos().equals(pos)) || (next != null && next.asBlockPos().equals(pos))) {
                continue;
            }

            // Ignore positions too far away
            if (isTooFarAway(level, entity, globalPos)) {
                iterator.remove();
                continue;
            }

            // Only now read the block
            BlockState state = level.getBlockState(pos);
            TraversableBarrier barrier = TraversableBarrier.resolve(state);
            if (barrier == null) {
                iterator.remove();
                continue;
            }

            // Already closed (e.g. a player closed it)
            if (!barrier.isOpen(state)) {
                iterator.remove();
                continue;
            }

            // Hand off to a following same-type mob mid-traversal instead of shutting the gate on it
            if (isAnotherMobComingThrough(entity, pos, nearestLivingEntities)) {
                iterator.remove();
                continue;
            }

            // Nobody's coming through -- close it
            barrier.setOpen(entity, level, state, pos, false);
            iterator.remove();
            anyClosed = true;
        }

        return anyClosed;
    }

    private static boolean isTooFarAway(ServerLevel level, LivingEntity entity, GlobalPos pos) {
        return pos.dimension() != level.dimension() || !pos.pos().closerToCenterThan(entity.position(), FORGET_BARRIER_BEYOND_DISTANCE);
    }

    private static boolean isAnotherMobComingThrough(LivingEntity entity, BlockPos pos, Optional<List<LivingEntity>> nearestLivingEntities) {
        return nearestLivingEntities.map(livingEntities -> livingEntities.stream()
                .filter(other -> other.getType() == entity.getType())
                .filter(other -> pos.closerToCenterThan(other.position(), HAND_OFF_DISTANCE))
                .anyMatch(other -> isMobMidTraversal(other.getBrain(), pos))).orElse(false);
    }

    private static boolean isMobMidTraversal(Brain<?> brain, BlockPos pos) {
        if (!brain.hasMemoryValue(MemoryModuleType.PATH)) {
            return false;
        }

        Path path = brain.getMemory(MemoryModuleType.PATH).get();
        if (path.isDone()) {
            return false;
        }

        Node previous = path.getPreviousNode();
        if (previous == null) {
            return false;
        }

        Node next = path.getNextNode();
        return pos.equals(previous.asBlockPos()) || pos.equals(next.asBlockPos());
    }

    /**
     * Deterministic scan throttle: scans on every path-node advance, plus a throttled re-scan
     */
    private static final class ScanThrottle {

        private final ITickable stationaryRescan = STATIONARY_RESCAN_INTERVAL.asTickable();
        private int lastScannedNodeIndex = -1;

        boolean shouldScan(int nodeIndex) {
            if (nodeIndex != this.lastScannedNodeIndex) {
                this.lastScannedNodeIndex = nodeIndex;
                this.stationaryRescan.reset();
                return true;
            }

            return this.stationaryRescan.tickCheckAndReset(1);
        }

    }

}
