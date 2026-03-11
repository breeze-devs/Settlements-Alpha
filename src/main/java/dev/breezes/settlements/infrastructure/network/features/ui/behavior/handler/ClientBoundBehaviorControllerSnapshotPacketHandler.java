package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleClientPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundBehaviorControllerSnapshotPacket;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerScreen;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

@CustomLog
@HandleClientPacket(ClientBoundBehaviorControllerSnapshotPacket.class)
public class ClientBoundBehaviorControllerSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundBehaviorControllerSnapshotPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundBehaviorControllerSnapshotPacket packet) {
        boolean applied = BehaviorControllerClientState.applySnapshot(packet.sessionId(), packet.snapshot());
        if (!applied) {
            log.debug("Ignoring stale {} sessionId={}", packet.getClass().getSimpleName(), packet.sessionId());
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Screen activeScreen = minecraft.screen;
        if (!(activeScreen instanceof BehaviorControllerScreen behaviorScreen)) {
            log.debug("Cannot apply snapshot because client does not have a behavior screen");
            return;
        }

        if (behaviorScreen.getSessionId() != packet.sessionId()) {
            log.debug("Cannot apply snapshot because session ID mismatch: expected {}, got {}", packet.sessionId(), behaviorScreen.getSessionId());
            return;
        }

        behaviorScreen.applySnapshot(packet.snapshot());
    }

}
