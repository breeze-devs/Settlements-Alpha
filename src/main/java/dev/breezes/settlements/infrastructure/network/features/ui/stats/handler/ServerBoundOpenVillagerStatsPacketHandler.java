package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSession;
import dev.breezes.settlements.application.ui.stats.session.VillagerStatsSessionService;
import dev.breezes.settlements.application.ui.stats.snapshot.VillagerStatsSnapshotBuilder;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundOpenVillagerStatsPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerDemandSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerInventorySnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerStatsUnavailablePacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ClientBoundVillagerTradeCatalogSnapshotPacket;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundOpenVillagerStatsPacket;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Objects;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundOpenVillagerStatsPacketHandler implements ServerSidePacketHandler<ServerBoundOpenVillagerStatsPacket> {

    private static final String UNAVAILABLE_REASON_KEY = "ui.settlements.stats.unavailable";

    private final VillagerStatsSessionService sessionService;
    private final VillagerStatsSnapshotBuilder snapshotBuilder;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DemandSignalService demandSignalService;

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundOpenVillagerStatsPacket packet) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = player.serverLevel().getEntity(packet.villagerEntityId());
        if (!(target instanceof BaseVillager villager) || !villager.isAlive() || villager.isRemoved()) {
            PacketDistributor.sendToPlayer(player, new ClientBoundVillagerStatsUnavailablePacket(-1L, UNAVAILABLE_REASON_KEY));
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        VillagerStatsSession session = sessionService.startOrReplaceSession(player, packet.villagerEntityId(), gameTime);

        PacketDistributor.sendToPlayer(player,
                new ClientBoundOpenVillagerStatsPacket(session.getSessionId(), packet.villagerEntityId()));

        VillagerStatsSnapshot statsSnapshot = snapshotBuilder.buildStats(villager, gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerStatsSnapshotPacket(session.getSessionId(), statsSnapshot));
        session.markStatsSnapshotSent(gameTime);

        VillagerInventorySnapshot inventorySnapshot = snapshotBuilder.buildInventory(villager);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerInventorySnapshotPacket(session.getSessionId(), inventorySnapshot));
        session.markInventorySnapshotSent(gameTime, villager.getSettlementsInventory().getInventoryVersion());

        VillagerTradeCatalogSnapshot tradeCatalogSnapshot = snapshotBuilder.buildTradeCatalog(villager);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerTradeCatalogSnapshotPacket(session.getSessionId(), tradeCatalogSnapshot));
        session.markTradeCatalogSnapshotSent(gameTime, this.tradeCatalogRegistry.catalogVersion(), resolveProfessionKey(villager));

        VillagerDemandDisplaySnapshot demandSnapshot = snapshotBuilder.buildDemandSnapshot(villager, gameTime);
        PacketDistributor.sendToPlayer(player, new ClientBoundVillagerDemandSnapshotPacket(session.getSessionId(), demandSnapshot));
        session.markDemandSnapshotSent(gameTime, demandDisplayVersion(villager, this.demandSignalService));

        log.debug("Opened villager stats sessionId={} for player={} villagerEntityId={}",
                session.getSessionId(), player.getUUID(), packet.villagerEntityId());
    }

    @Nonnull
    private static VillagerProfessionKey resolveProfessionKey(@Nonnull BaseVillager villager) {
        return VillagerProfessionKey.fromResourceLocation(
                // TODO: make base villager return convenience domain profession key method
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
        );
    }

    private static int demandDisplayVersion(@Nonnull BaseVillager villager,
                                            @Nonnull DemandSignalService demandSignalService) {
        return Objects.hash(villager.getSettlementsInventory().getInventoryVersion(), demandSignalService.getVersion(villager));
    }

}
