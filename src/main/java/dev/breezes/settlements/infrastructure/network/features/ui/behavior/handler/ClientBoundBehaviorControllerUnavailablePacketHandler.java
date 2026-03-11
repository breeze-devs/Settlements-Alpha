package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleClientPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerUnavailablePacket;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerScreen;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@CustomLog
@HandleClientPacket(ClientBoundBehaviorControllerUnavailablePacket.class)
public class ClientBoundBehaviorControllerUnavailablePacketHandler implements ClientSidePacketHandler<ClientBoundBehaviorControllerUnavailablePacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundBehaviorControllerUnavailablePacket packet) {
        boolean applied = BehaviorControllerClientState.markUnavailable(packet.sessionId(), packet.reasonKey());
        if (!applied) {
            Minecraft minecraft = Minecraft.getInstance();
            if (BehaviorControllerClientState.activeSessionId() <= 0 && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(packet.reasonKey()), true);
                log.debug("Displayed unavailable fallback message for {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
                return;
            }

            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        if (activeScreen instanceof BehaviorControllerScreen behaviorScreen
                && behaviorScreen.getSessionId() == packet.sessionId()) {
            behaviorScreen.markUnavailable(Component.translatable(packet.reasonKey()));
        }
    }

}
