package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

public record BlockResource(@Nonnull BlockMatcher matcher,
                            @Nonnull MemoryType<List<GlobalPos>> memoryType,
                            int maxSites) {

    private static final int DEFAULT_MAX_SITES = 32;

    public BlockResource(@Nonnull BlockMatcher matcher,
                         @Nonnull MemoryType<List<GlobalPos>> memoryType) {
        this(matcher, memoryType, DEFAULT_MAX_SITES);
    }

    public BlockResource {
        if (maxSites < 1) {
            throw new IllegalArgumentException("maxSites must be at least 1");
        }
    }

    public MemoryWrite<List<GlobalPos>> toMemoryWrite(@Nonnull List<BlockPos> hits,
                                                      @Nonnull BlockPos center,
                                                      @Nonnull Level level) {
        if (hits.isEmpty()) {
            return MemoryWrite.clear(this.memoryType);
        }

        return MemoryWrite.of(this.memoryType, this.nearestHits(hits, center).stream()
                .map(pos -> GlobalPos.of(level.dimension(), pos))
                .toList());
    }

    private List<BlockPos> nearestHits(@Nonnull List<BlockPos> hits, @Nonnull BlockPos center) {
        if (hits.size() <= this.maxSites) {
            return hits;
        }

        return hits.stream()
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                .limit(this.maxSites)
                .toList();
    }

}
