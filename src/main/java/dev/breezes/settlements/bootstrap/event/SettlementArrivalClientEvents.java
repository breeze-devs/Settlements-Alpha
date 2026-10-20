package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import javax.annotation.Nonnull;

/**
 * Bootstrap wiring between NeoForge's client event bus and the settlement arrival HUD.
 */
@ClientSide
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementArrivalClientEvents {

    // Last, so only bars no listener canceled are measured, at the spacing listeners settled on
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBossEventProgress(@Nonnull CustomizeGuiOverlayEvent.BossEventProgress event) {
        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        if (clientComponent != null) {
            clientComponent.settlementArrivalHudRenderer().onBossEventProgress(event);
        }
    }

}
