package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nonnull;

/**
 * Bootstrap wiring between NeoForge's client tick and flare playback.
 */
@ClientSide
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlareClientEvents {

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.flarePlayback().onClientTick();
        }
    }

}
