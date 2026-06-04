package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class CourtshipSessionReaperServerEvents {

    private final CourtshipSessionRegistry sessionRegistry;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Rate-limit to ~once per second to avoid scanning all sessions every tick.
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        this.sessionRegistry.tickTimeouts(event.getServer().overworld().getGameTime());
    }

}
