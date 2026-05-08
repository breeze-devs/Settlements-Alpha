package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsClientState;
import dev.breezes.settlements.presentation.ui.stats.VillagerStatsScreen;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ClientBoundOpenVillagerStatsPacketHandler implements ClientSidePacketHandler<ClientBoundOpenVillagerStatsPacket> {

    private final VillagerStatsClientState villagerStatsClientState;

    @Override
    public void runOnClient(@Nonnull IPayloadContext context, @Nonnull ClientBoundOpenVillagerStatsPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        log.debug("Received {} sessionId={} villagerEntityId={}",
                packet.getClass().getSimpleName(),
                packet.sessionId(),
                packet.villagerEntityId());

        this.villagerStatsClientState.openSession(packet.sessionId(), packet.villagerEntityId());

        VillagerStatsSnapshot bootstrapSnapshot = VillagerStatsSnapshot.builder()
                .gameTime(minecraft.level.getGameTime())
                .villagerEntityId(packet.villagerEntityId())
                .villagerName(null)
                .professionKey("minecraft:none")
                .expertiseLevel(1)
                .currentHealth(20)
                .maxHealth(20)
                .geneValues(new double[GeneType.VALUES.length])
                .homePos(null)
                .workstationPos(null)
                .activeBehaviorNameKey(null)
                .activeBehaviorIconId(null)
                .schedulePhase(SchedulePhase.UNKNOWN)
                .reputation(0)
                .build();
        VillagerStatsScreen screen = new VillagerStatsScreen(packet.sessionId(), bootstrapSnapshot, this.villagerStatsClientState);
        minecraft.setScreen(screen);
    }

}
