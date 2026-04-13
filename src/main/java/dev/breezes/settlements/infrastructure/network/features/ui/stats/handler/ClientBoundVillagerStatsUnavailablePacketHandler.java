package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsUnavailablePacket;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsScreen;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerStatsUnavailablePacketHandler implements ClientSidePacketHandler<ClientBoundVillagerStatsUnavailablePacket> {

    private final VillagerStatsClientState villagerStatsClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerStatsUnavailablePacket packet) {
        boolean applied = this.villagerStatsClientState.markUnavailable(packet.sessionId(), packet.reasonKey());
        if (!applied) {
            Minecraft minecraft = Minecraft.getInstance();
            if (this.villagerStatsClientState.activeSessionId() <= 0 && minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(packet.reasonKey()), true);
                log.debug("Displayed unavailable fallback message for {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
                return;
            }

            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        if (activeScreen instanceof VillagerStatsScreen statsScreen
                && statsScreen.getSessionId() == packet.sessionId()) {
            statsScreen.markUnavailable(Component.translatable(packet.reasonKey()));
        }
    }

}
