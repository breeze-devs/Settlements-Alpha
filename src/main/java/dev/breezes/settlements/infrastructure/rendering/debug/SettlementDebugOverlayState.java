package dev.breezes.settlements.infrastructure.rendering.debug;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;

@ClientSide
@ClientScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class SettlementDebugOverlayState {

    private static final long OVERLAY_TTL_TICKS = 20L * 60L;

    @Nullable
    private ClientBoundSettlementDebugPacket current;
    private long expiresAtGameTime;

    public void set(@Nonnull ClientBoundSettlementDebugPacket packet) {
        this.current = packet;

        Minecraft minecraft = Minecraft.getInstance();
        long currentGameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        this.expiresAtGameTime = currentGameTime + OVERLAY_TTL_TICKS;
    }

    public void clear() {
        this.current = null;
        this.expiresAtGameTime = 0L;
    }

    @Nonnull
    public Optional<ClientBoundSettlementDebugPacket> get() {
        if (this.current == null) {
            return Optional.empty();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return Optional.of(this.current);
        }

        if (minecraft.level.getGameTime() >= this.expiresAtGameTime) {
            // TTL keeps the overlay self-cleaning so debug state does not linger across normal play.
            this.clear();
            return Optional.empty();
        }

        return Optional.of(this.current);
    }

}
