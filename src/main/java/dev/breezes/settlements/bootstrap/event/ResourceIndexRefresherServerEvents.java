package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.sensors.ResourceIndexRefresher;
import dev.breezes.settlements.application.ai.sensors.WorldResourceIndex;
import dev.breezes.settlements.di.ServerScope;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.inject.Inject;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ResourceIndexRefresherServerEvents {

    private final ResourceIndexRefresher refresher;
    private final WorldResourceIndex index;

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        // TODO:CONFIRM should this be throttled??
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            this.refresher.refresh(serverLevel);
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        // Per-chunk eviction prevents index memory leak and drops stale in-flight markers
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ChunkPos pos = event.getChunk().getPos();
            this.index.evictChunk(serverLevel, pos.x, pos.z);
            this.refresher.evictChunk(serverLevel, pos.x, pos.z);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            this.index.clear(serverLevel);
            this.refresher.clearLevel(serverLevel);
        }
    }

}
