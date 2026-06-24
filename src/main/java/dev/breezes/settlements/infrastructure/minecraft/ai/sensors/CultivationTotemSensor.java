package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.blocks.totem.TotemOfCultivationBlockEntity;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovers nearby Totems of Cultivation and records the valid ones in memory.
 * <p>
 * Totems are block entities, so this iterates each loaded chunk's block-entity map rather than
 * brute-forcing the search cube — the same approach as {@link VillageChestsSensor}. That keeps the
 * scan cheap even at the deliberately large {@value #HORIZONTAL_RANGE}-block horizontal reach, which
 * lets a farmer tend plots well outside the tight range the generic block sensor uses.
 * <p>
 * Only the {@code valid} flag is checked here; whether a totem currently has actionable work is left
 * to the behavior's {@code CULTIVATION_TOTEM_NEEDS_WORK} precondition so the memory stays a stable
 * "known totems" list rather than churning as cells are tilled and planted.
 */
public class CultivationTotemSensor extends Sensor<Villager> {

    private static final int SCAN_INTERVAL_TICKS = ClockTicks.minutes(2).getTicksAsInt();
    private static final int HORIZONTAL_RANGE = 64;
    private static final int VERTICAL_RANGE = 8;

    public CultivationTotemSensor() {
        super(SCAN_INTERVAL_TICKS);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(MemoryTypeRegistry.CULTIVATION_TOTEM_SITES.getModuleType());
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

                // Iterate only positions that actually host a block entity rather than the full
                // 129×17×129 cube — a handful of candidates per chunk instead of millions of blocks.
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();

                    if (Math.abs(pos.getY() - center.getY()) > VERTICAL_RANGE
                            || Math.abs(pos.getX() - center.getX()) > HORIZONTAL_RANGE
                            || Math.abs(pos.getZ() - center.getZ()) > HORIZONTAL_RANGE) {
                        continue;
                    }

                    // The valid flag is cached by the totem's server tick — reading it is O(1).
                    if (!(entry.getValue() instanceof TotemOfCultivationBlockEntity totem) || !totem.isValid()) {
                        continue;
                    }

                    found.add(GlobalPos.of(dimension, pos.immutable()));
                }
            }
        }

        if (found.isEmpty()) {
            baseVillager.getBrain().eraseMemory(MemoryTypeRegistry.CULTIVATION_TOTEM_SITES.getModuleType());
        } else {
            baseVillager.getBrain().setMemory(MemoryTypeRegistry.CULTIVATION_TOTEM_SITES.getModuleType(), List.copyOf(found));
        }
    }

}
