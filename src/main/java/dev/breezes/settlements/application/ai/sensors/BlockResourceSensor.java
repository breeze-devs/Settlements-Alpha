package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlockResourceSensor extends AbstractSensor<BaseVillager> {

    private final BlockResourceSensorConfig config;
    // Immutable copy taken once at construction and passed straight to queryAll each sense — the
    // resource set is fixed, so copying it per-tick would allocate on the hot per-villager scan path.
    private final Set<BlockResource> resources;
    private final WorldResourceIndex index;

    public BlockResourceSensor(@Nonnull BlockResourceSensorConfig config,
                               @Nonnull Set<BlockResource> resources,
                               @Nonnull WorldResourceIndex index) {
        super(List.of(), Tickable.staggered(ClockTicks.seconds(config.scanIntervalSeconds())));
        this.config = config;
        this.resources = Set.copyOf(resources);
        this.index = index;
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return List.of();
        }

        BlockPos anchor = entity.blockPosition();
        long nowTick = serverLevel.getGameTime();
        long ttlTicks = ClockTicks.seconds(this.config.scanIntervalSeconds()).getTicks();

        // One multi-resource sweep: sections are enumerated once, hits fanned into per-resource buckets in a single pass
        Map<BlockResource, BlockResourceQueryResult> results = this.index.queryAll(serverLevel, this.resources, anchor,
                this.config.scanRangeHorizontal(), this.config.scanRangeVertical(), nowTick, ttlTicks);

        int horizontal = this.config.scanRangeHorizontal();
        int vertical = this.config.scanRangeVertical();

        List<IMemoryWrite> writes = new ArrayList<>(this.resources.size());
        for (BlockResource resource : this.resources) {
            BlockResourceQueryResult result = results.get(resource);
            if (result == null) {
                continue;
            }

            resource.toMemoryWrite(result, anchor, nowTick, horizontal, vertical)
                    .ifPresent(writes::add);
        }

        return writes;
    }

}
