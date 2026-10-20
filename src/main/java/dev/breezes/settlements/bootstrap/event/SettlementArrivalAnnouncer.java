package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.founding.SettlementFoundingConfig;
import dev.breezes.settlements.application.settlement.health.SettlementHealthService;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.infrastructure.minecraft.attachments.PlayerSettlementDiscoveries;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementEnterEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementExitEvent;
import dev.breezes.settlements.infrastructure.minecraft.event.settlement.SettlementRenamedEvent;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.ClientBoundSettlementArrivalPacket;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.SettlementAnnouncementType;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Sends each player the settlement moments it lives through, and keeps the record of the settlements each player has
 * discovered, which decides whether an entry is a discovery.
 * <p>
 * Spectators are sent nothing and discover nothing: they pass through settlements without being there.
 */
@ServerSide
@ServerScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementArrivalAnnouncer {

    private final SettlementStore settlementStore;
    private final SettlementHealthService healthService;
    private final SettlementFoundingConfig foundingConfig;

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerSettlementDiscoveries discoveries = player.getExistingDataOrNull(AttachmentRegistry.PLAYER_SETTLEMENT_DISCOVERIES);
        if (discoveries == null) {
            return;
        }

        // A disbanded settlement's id is never stored again, so removing it from discovered set is safe
        // Hence this may run before or after the tracker places the player: no placement enters a settlement this deletes
        MinecraftServer server = player.server;
        PlayerSettlementDiscoveries kept = discoveries.pruned(dimension -> server.getLevel(dimension) != null,
                (dimension, settlementId) -> this.settlementStore.find(server.getLevel(dimension), settlementId)
                        .isPresent());

        if (!kept.equals(discoveries)) {
            player.setData(AttachmentRegistry.PLAYER_SETTLEMENT_DISCOVERIES, kept);
        }
    }

    @SubscribeEvent
    public void onSettlementEnter(SettlementEnterEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.isSpectator()) {
            return;
        }

        Settlement settlement = event.getSettlement();
        boolean discovered = discover(player, event.getDimension(), settlement.id());
        send(player, SettlementAnnouncementType.ofEntry(event.getCause(), discovered), event.getDimension(), settlement,
                this.status(settlement), null);
    }

    @SubscribeEvent
    public void onSettlementExit(SettlementExitEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.isSpectator()) {
            return;
        }

        SettlementAnnouncementType announcementType = SettlementAnnouncementType.ofExit(event.getCause());
        Settlement settlement = event.getSettlement();
        // A disbanded settlement has no status left: its removed record's fading counts toward nothing
        SettlementSummary.Fading status = announcementType == SettlementAnnouncementType.DISBANDED ? null : this.status(settlement);
        send(player, announcementType, event.getDimension(), settlement, status, null);
    }

    @SubscribeEvent
    public void onSettlementRenamed(SettlementRenamedEvent event) {
        ServerPlayer renamer = event.getRenamer();
        if (renamer == null || renamer.isSpectator()) {
            return;
        }

        ResourceKey<Level> dimension = event.getLevel().dimension();
        Settlement settlement = event.getSettlement();
        discover(renamer, dimension, settlement.id());
        send(renamer, SettlementAnnouncementType.RENAMED, dimension, settlement, this.status(settlement), event.getPreviousName());
    }

    /**
     * Records the settlement as discovered by the player.
     *
     * @return whether the player had discovered it before
     */
    private static boolean discover(ServerPlayer player, ResourceKey<Level> dimension, UUID settlementId) {
        // Read without creating it, so a player who never discovers anything saves nothing
        PlayerSettlementDiscoveries discoveries = player.getExistingData(AttachmentRegistry.PLAYER_SETTLEMENT_DISCOVERIES)
                .orElse(PlayerSettlementDiscoveries.NONE);
        if (discoveries.contains(dimension, settlementId)) {
            return true;
        }

        player.setData(AttachmentRegistry.PLAYER_SETTLEMENT_DISCOVERIES, discoveries.with(dimension, settlementId));
        return false;
    }

    @Nullable
    private SettlementSummary.Fading status(Settlement settlement) {
        return SettlementSummary.of(settlement, this.healthService.fadingDuration().orElse(null),
                this.foundingConfig.minimumResidents()).fading();
    }

    private static void send(ServerPlayer player, SettlementAnnouncementType announcementType,
                             ResourceKey<Level> dimension, Settlement settlement, @Nullable SettlementSummary.Fading status,
                             @Nullable String previousName) {
        PacketDistributor.sendToPlayer(player, ClientBoundSettlementArrivalPacket.builder()
                .announcementType(announcementType)
                .dimension(dimension)
                .settlementId(settlement.id())
                .name(settlement.name())
                .status(status)
                .previousName(previousName)
                .build());
    }

}
