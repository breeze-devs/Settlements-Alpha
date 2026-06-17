package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.shared.util.ReputationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.inject.Inject;

/**
 * Applies credibility score decay to all per-observer {@link dev.breezes.settlements.domain.ai.credibility.CredibilityStore}
 * instances every server tick.
 * <p>
 * Running decay every tick keeps the math simple (decay exponent is per-tick) and the
 * per-tick cost is negligible — it is a single multiply per known source per observer.
 * Mirrors the {@link WorldEventBusReaperServerEvents} pattern for server-tick subscription.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class CredibilityDecayServerEvents {

    private final ReputationUtil reputationUtil;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Rate limit to per second
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        this.reputationUtil.tickDecay(20L);
    }

}
