package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementEnterEvent;
import lombok.NoArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Surfaces region transitions to players using vanilla title packets.
 * <p>
 * We keep this handler server-side and event-driven so future consumers can
 * adjust presentation without touching the tracking/query layers.
 */
@NoArgsConstructor(onConstructor_ = @Inject)
public final class RegionSubtitleHandler {

    private static final String REGION_WELCOME_TITLE_KEY = "overlay.settlements.region.enter.title";

    private static final int TITLE_FADE_IN_TICKS = 5;
    private static final int TITLE_STAY_TICKS = 40;
    private static final int TITLE_FADE_OUT_TICKS = 10;

    @SubscribeEvent
    public void onSettlementEnter(SettlementEnterEvent event) {
        sendRegionTitle(
                event.getPlayer(),
                Component.translatable(REGION_WELCOME_TITLE_KEY),
                Component.literal(event.getSettlementMetadata().name())
        );
    }

    private static void sendRegionTitle(@Nonnull ServerPlayer player,
                                        @Nonnull Component title,
                                        @Nonnull Component subtitle) {
        // We always send animation + full text payload together so late-arriving enter events
        // never inherit stale title state from an earlier region transition.
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                TITLE_FADE_IN_TICKS,
                TITLE_STAY_TICKS,
                TITLE_FADE_OUT_TICKS
        ));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

}
