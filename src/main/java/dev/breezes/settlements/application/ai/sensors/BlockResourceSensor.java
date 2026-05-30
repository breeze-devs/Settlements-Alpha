package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.AabbBlockScan;
import dev.breezes.settlements.domain.world.blocks.BlockMatcher;
import dev.breezes.settlements.domain.world.blocks.BlockScanBox;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BlockResourceSensor extends AbstractSensor<BaseVillager> {

    private final BlockResourceSensorConfig config;
    private final List<BlockResource> resources;
    private final List<BlockMatcher> matchers;

    public BlockResourceSensor(@Nonnull BlockResourceSensorConfig config, @Nonnull Set<BlockResource> resources) {
        super(List.of(), ClockTicks.seconds(config.scanIntervalSeconds()).asTickable());
        this.config = config;
        this.resources = List.copyOf(resources);
        this.matchers = this.resources.stream()
                .map(BlockResource::matcher)
                .toList();
    }

    @Override
    public List<MemoryWrite<?>> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        BlockScanBox scanBox = new BlockScanBox(this.config.scanRangeHorizontal(), this.config.scanRangeVertical());
        List<List<BlockPos>> hitsByResource = AabbBlockScan.scanMatchingEach(entity.blockPosition(), scanBox, this.matchers, world);

        List<MemoryWrite<?>> writes = new ArrayList<>(this.resources.size());
        for (int i = 0; i < this.resources.size(); i++) {
            writes.add(this.resources.get(i).toMemoryWrite(hitsByResource.get(i), world));
        }

        return writes;
    }

}
