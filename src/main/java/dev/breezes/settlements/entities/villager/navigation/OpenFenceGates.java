package dev.breezes.settlements.entities.villager.navigation;

import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.sounds.SoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nonnull;
import java.util.*;

public class OpenFenceGates extends OneShot<Villager> {
    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
    private static final double SKIP_CLOSING_IF_FURTHER_THAN = 3.0D;
    // private static final double MAX_DISTANCE_TO_HOLD_GATE_OPEN_FOR_OTHER_MOBS = 2.0D;

    private Node currentNode;
    private int cooldown;

    public OpenFenceGates() {
        this.currentNode = null;
        this.cooldown = 0;
    }

    @Override
    public boolean trigger(@Nonnull ServerLevel level, Villager villager, long time) {
        // Get current path
        Brain<Villager> brain = villager.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.PATH))
            return false;
        Path path = brain.getMemory(MemoryModuleType.PATH).get();

        // Check path
        if (path.notStarted() || path.isDone())
            return false;

        // Check cooldown
        if (--this.cooldown > 0) {
            return false;
        }

        // Increment node if needed
        if (Objects.equals(path.getNextNode(), this.currentNode)) {
            this.cooldown = COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE;
        }
        this.currentNode = path.getNextNode();

        boolean triggered = false;

        // Check if the previous and next nodes are fence gates
        List<Node> toCheck = new ArrayList<>(Arrays.asList(path.getPreviousNode(), path.getNextNode()));

        // If applicable, add the next-next node to check
        if (path.getNextNodeIndex() + 1 < path.getNodeCount()) {
            toCheck.add(path.getNode(path.getNextNodeIndex() + 1));
        }

        for (Node node : toCheck) {
            if (node == null)
                continue;

            BlockPos pos = node.asBlockPos();
            BlockState state = level.getBlockState(pos);

            // Ignore if not a fence gate
            if (isNotFenceGate(state))
                continue;

            // Open the fence gate & remember it
            if (!isFenceGateOpen(state)) {
                setFenceGateOpen(villager, level, state, pos, true);
                triggered = true;
            }
            // rememberFenceGateToClose(level, (BaseVillager) villager, pos);
        }

        // Close relevant fence gates
        // closeRelevantFenceGates(level, villager, toCheck);
        return triggered;
    }

    /* TODO this memory system isn't implemented
    private static void rememberFenceGateToClose(ServerLevel level, BaseVillager villager, BlockPos toClose) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), toClose);
        Brain<Villager> brain = villager.getBrain();

        // If there are no memory of fence gates, create new memory
        if (!brain.hasMemoryValue(VillagerMemoryType.FENCE_GATE_TO_CLOSE)) {
            Set<GlobalPos> memory = Sets.newHashSet(globalPos);
            brain.setMemory(VillagerMemoryType.FENCE_GATE_TO_CLOSE, Optional.of(memory));
            return;
        }

        // Otherwise, add to the existing memory
        Set<GlobalPos> memory = villager.getBrain().getMemory(VillagerMemoryType.FENCE_GATE_TO_CLOSE).get();
        memory.add(globalPos);
    }

    public static void closeRelevantFenceGates(ServerLevel level, Villager villager, @Nonnull List<Node> nodes) {
        Brain<Villager> brain = villager.getBrain();

        // If there are no memory, ignore
        if (!brain.hasMemoryValue(VillagerMemoryType.FENCE_GATE_TO_CLOSE))
            return;

        // Loop through each remembered position
        Set<GlobalPos> memory = villager.getBrain().getMemory(VillagerMemoryType.FENCE_GATE_TO_CLOSE).get();
        Iterator<GlobalPos> iterator = memory.iterator();
        while (iterator.hasNext()) {
            GlobalPos pos = iterator.next();
            BlockPos blockPos = pos.pos();

            // Don't close the gate if the nodes are "current"
            if (nodes.stream().anyMatch((node) -> node.asBlockPos().equals(blockPos))) {
                continue;
            }

            if (isFenceGateTooFarAway(level, villager, GlobalPos.of(level.dimension(),blockPos))) {
                iterator.remove();
                continue;
            }

            BlockState state = level.getBlockState(blockPos);
            if (isNotFenceGate(state)) {
                iterator.remove();
                continue;
            }

            if (!isFenceGateOpen(state)) {
                iterator.remove();
                continue;
            }

            // Close the fence gate
            setFenceGateOpen(villager, level, state, blockPos, false);
            iterator.remove();
        }
    }
    */

    private static boolean isFenceGateTooFarAway(ServerLevel world, LivingEntity entity, GlobalPos gatePosition) {
        return gatePosition.dimension() != world.dimension() || !gatePosition.pos().closerToCenterThan(entity.position(), SKIP_CLOSING_IF_FURTHER_THAN);
    }

    private static boolean isNotFenceGate(BlockState state) {
        return !state.is(BlockTags.FENCE_GATES, stateBase -> stateBase.getBlock() instanceof FenceGateBlock);
    }

    private static boolean isFenceGateOpen(BlockState blockState) {
        return blockState.getValue(FenceGateBlock.OPEN);
    }

    private static void setFenceGateOpen(@Nonnull Entity entity, Level level, BlockState state, BlockPos pos, boolean toOpen) {
        if (isFenceGateOpen(state) == toOpen)
            return;

        level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, toOpen), 10);

        if(toOpen) SoundRegistry.OPEN_FENCE_GATE.playGlobally(Location.of(pos, level), SoundSource.BLOCKS);
        else SoundRegistry.CLOSE_FENCE_GATE.playGlobally(Location.of(pos, level), SoundSource.BLOCKS);

        level.gameEvent(entity, toOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);

        /* TODO once leashing animals gets implemented
        if (!toOpen)
            return;

        // If open, apply slowness to all entities nearby briefly
        // - this is to prevent animals from walking out of the fence gate
        for (Animal nearby : level.getEntitiesOfClass(Animal.class, entity.getBoundingBox().inflate(7, 4, 7))) {
            if (nearby == null || !nearby.isAlive() || nearby instanceof Wolf)
                continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TimeUtil.seconds(5), 20, false, false),
                    entity, EntityPotionEffectEvent.Cause.PLUGIN);
            nearby.getNavigation().stop();
        }
        */
    }

}
