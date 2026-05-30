package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

public record BlockResource(@Nonnull BlockMatcher matcher,
                            @Nonnull MemoryType<List<GlobalPos>> memoryType) {

    public MemoryWrite<List<GlobalPos>> toMemoryWrite(@Nonnull List<BlockPos> hits, @Nonnull Level level) {
        if (hits.isEmpty()) {
            return MemoryWrite.clear(this.memoryType);
        }

        return MemoryWrite.of(this.memoryType, hits.stream()
                .map(pos -> GlobalPos.of(level.dimension(), pos))
                .toList());
    }

}
