package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VillageChestsSensor extends Sensor<Villager> {

    private static final int SCAN_INTERVAL_TICKS = ClockTicks.minutes(2).getTicksAsInt();
    private static final int HORIZONTAL_RANGE = 32;
    private static final int VERTICAL_RANGE = 8;

    public VillageChestsSensor() {
        super(SCAN_INTERVAL_TICKS);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType());
    }

    @Override
    protected void doTick(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        List<GlobalPos> found = new ArrayList<>();
        ResourceKey<Level> dimension = level.dimension();
        BlockPos center = baseVillager.blockPosition();

        int minChunkX = (center.getX() - HORIZONTAL_RANGE) >> 4;
        int maxChunkX = (center.getX() + HORIZONTAL_RANGE) >> 4;
        int minChunkZ = (center.getZ() - HORIZONTAL_RANGE) >> 4;
        int maxChunkZ = (center.getZ() + HORIZONTAL_RANGE) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                // getChunkNow returns null for unloaded chunks. A sensor must never trigger
                // chunk load/generation — that would cause hitches and pull in terrain the
                // player hasn't visited.
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }

                // Iterate only positions that actually host a block entity. Reduces the
                // 65×17×65 brute-force cube to a few hundred candidates per scan.
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();

                    if (Math.abs(pos.getY() - center.getY()) > VERTICAL_RANGE
                            || Math.abs(pos.getX() - center.getX()) > HORIZONTAL_RANGE
                            || Math.abs(pos.getZ() - center.getZ()) > HORIZONTAL_RANGE) {
                        continue;
                    }

                    BlockState state = chunk.getBlockState(pos);
                    if (!state.is(Tags.Blocks.CHESTS)) {
                        continue;
                    }

                    // Double-chest deduplication: the IItemHandler capability fetched from the LEFT
                    // block exposes the full merged 54-slot inventory, so RIGHT halves are redundant.
                    if (state.hasProperty(ChestBlock.TYPE)
                            && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) {
                        continue;
                    }

                    found.add(GlobalPos.of(dimension, pos.immutable()));
                }
            }
        }

        if (found.isEmpty()) {
            baseVillager.getBrain().eraseMemory(MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType());
        } else {
            baseVillager.getBrain().setMemory(MemoryTypeRegistry.VILLAGE_CHESTS.getModuleType(), List.copyOf(found));
        }
    }

}
