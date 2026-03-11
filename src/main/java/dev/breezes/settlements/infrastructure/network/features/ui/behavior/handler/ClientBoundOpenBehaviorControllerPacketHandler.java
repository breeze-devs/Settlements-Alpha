package dev.breezes.settlements.infrastructure.network.features.ui.behavior.handler;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleClientPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ClientBoundOpenBehaviorControllerPacket;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerClientState;
import dev.breezes.settlements.presentation.ui.behavior.BehaviorControllerScreen;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import java.util.List;

@CustomLog
@HandleClientPacket(ClientBoundOpenBehaviorControllerPacket.class)
public class ClientBoundOpenBehaviorControllerPacketHandler implements ClientSidePacketHandler<ClientBoundOpenBehaviorControllerPacket> {

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundOpenBehaviorControllerPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        log.debug("Received {} sessionId={} villagerEntityId={}",
                packet.getClass().getSimpleName(),
                packet.sessionId(),
                packet.villagerEntityId());

        BehaviorControllerClientState.openSession(packet.sessionId(), packet.villagerEntityId());

        // TODO: we should like make this a loading screen? maybe?
        BehaviorControllerSnapshot bootstrapSnapshot = BehaviorControllerSnapshot.builder()
                .gameTime(minecraft.level.getGameTime())
                .villagerEntityId(packet.villagerEntityId())
                .villagerName("Unknown")
                .scheduleBucket(SchedulePhase.UNKNOWN)
                .rawActivityKey("Unknown")
                .rows(List.of())
                .build();
        BehaviorControllerScreen screen = new BehaviorControllerScreen(packet.sessionId(), bootstrapSnapshot);
        minecraft.setScreen(screen);
    }

}
