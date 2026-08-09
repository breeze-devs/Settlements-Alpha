package dev.breezes.settlements.infrastructure.rendering.debug;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class SettlementDebugOverlayState implements ClientSessionResettable {

    private static final ClockTicks OVERLAY_TTL = ClockTicks.seconds(60);

    @Nullable
    private ClientBoundSettlementDebugPacket current;
    private long expiresAtMillis;

    public void set(@Nonnull ClientBoundSettlementDebugPacket packet) {
        this.current = packet;
        this.expiresAtMillis = ClientMonotonicClock.deadlineFrom(OVERLAY_TTL);
    }

    public void clear() {
        this.current = null;
        this.expiresAtMillis = 0L;
    }

    @Nonnull
    public Optional<ClientBoundSettlementDebugPacket> get() {
        if (this.current == null) {
            return Optional.empty();
        }

        if (ClientMonotonicClock.nowMillis() >= this.expiresAtMillis) {
            // TTL keeps the overlay self-cleaning so debug state does not linger across normal play.
            this.clear();
            return Optional.empty();
        }

        return Optional.of(this.current);
    }

    @Override
    public void onClientSessionEnded() {
        this.clear();
    }

}
