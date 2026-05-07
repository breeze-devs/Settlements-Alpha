package dev.breezes.settlements.infrastructure.network.features.ui.dayplan.handler;

import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.dayplan.packet.ClientBoundDayPlanSnapshotPacket;
import dev.breezes.settlements.presentation.ui.dayplan.DayPlanScreen;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
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
public class ClientBoundDayPlanSnapshotPacketHandler implements ClientSidePacketHandler<ClientBoundDayPlanSnapshotPacket> {

    private final UiClientState uiClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundDayPlanSnapshotPacket packet) {
        if (!this.uiClientState.recordSnapshotReceived(packet.sessionId())) {
            log.debug("Ignoring stale day plan snapshot sessionId={}", packet.sessionId());
            return;
        }

        Screen activeScreen = Minecraft.getInstance().screen;
        if (!(activeScreen instanceof DayPlanScreen dayPlanScreen) || dayPlanScreen.getSessionId() != packet.sessionId()) {
            log.debug("Cannot apply day plan snapshot because active screen does not match sessionId={}", packet.sessionId());
            return;
        }

        dayPlanScreen.applySnapshot(packet.snapshot());
    }

}
