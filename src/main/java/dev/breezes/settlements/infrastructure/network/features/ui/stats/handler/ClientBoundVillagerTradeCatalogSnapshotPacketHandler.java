package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsScreen;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundVillagerTradeCatalogSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundVillagerTradeCatalogSnapshotPacket> {

    private final VillagerStatsClientState villagerStatsClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundVillagerTradeCatalogSnapshotPacket packet) {
        boolean applied = this.villagerStatsClientState.applyTradeCatalogSnapshot(packet.sessionId(), packet.snapshot());
        if (!applied) {
            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        if (!(activeScreen instanceof VillagerStatsScreen statsScreen)) {
            log.debug("Cannot apply trade catalog snapshot because client does not have a villager stats screen");
            return;
        }

        if (statsScreen.getSessionId() != packet.sessionId()) {
            log.debug("Cannot apply trade catalog snapshot because session ID mismatch: expected {}, got {}", packet.sessionId(), statsScreen.getSessionId());
            return;
        }

        statsScreen.applyTradeCatalogSnapshot(packet.snapshot());
    }

}
