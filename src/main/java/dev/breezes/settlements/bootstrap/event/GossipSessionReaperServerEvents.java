package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;

/**
 * Evicts timed-out gossip invites once per second.
 * Mirrors {@link CourtshipSessionReaperServerEvents}.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class GossipSessionReaperServerEvents {

    private final GossipSessionRegistry gossipSessionRegistry;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Rate-limit to once per second
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        this.gossipSessionRegistry.tickTimeouts(event.getServer().overworld().getGameTime());
    }

}
