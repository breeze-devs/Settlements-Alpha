package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.di.ServerScope;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;

/**
 * Evicts timed-out trade invites and closes overdue trade sessions once per second.
 * Mirrors {@link CourtshipSessionReaperServerEvents}.
 * <p>
 * Without this reaper a trade session orphaned by its initiator (the initiator panics, unloads, or
 * is interrupted before driving the session to a terminal phase) would never close. The responder's
 * TradeAcceptBehavior override mirrors that session forever, holding the PLAN_BEHAVIOR_ACTIVE lock
 * and freezing the villager until something force-stops the override.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class TradeSessionReaperServerEvents {

    private final TradeSessionRegistry sessionRegistry;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Rate-limit to once per second to avoid scanning all sessions every tick.
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        this.sessionRegistry.tickTimeouts(event.getServer().overworld().getGameTime());
    }

}
