package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Evicts stale events from the {@link WorldEventBus} on a fixed server-tick cadence.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class WorldEventBusReaperServerEvents {

    private final WorldEventBus worldEventBus;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        this.worldEventBus.evict(event.getServer().overworld().getGameTime());
    }

}
