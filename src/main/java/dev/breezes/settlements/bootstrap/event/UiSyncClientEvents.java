package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundCloseUiPacket;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class UiSyncClientEvents {

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        UiClientState uiClientState = SettlementsDagger.client().uiClientState();
        if (uiClientState.activeSessionId() <= 0) {
            return;
        }

        Screen currentScreen = Minecraft.getInstance().screen;
        UiChannel channel = uiClientState.activeChannel();
        if (currentScreen != null || channel == null) {
            return;
        }

        long sessionId = uiClientState.activeSessionId();
        PacketDistributor.sendToServer(new ServerBoundCloseUiPacket(channel, sessionId));
        uiClientState.clearSession(sessionId);
    }

}
