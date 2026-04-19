package dev.breezes.settlements.application.ui.stats.session;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class VillagerStatsSession {

    private final long sessionId;
    private final UUID playerUuid;
    private final int villagerEntityId;
    private final long openedAtGameTime;

    // Mutable fields below are only written from the server tick thread (single-threaded in Minecraft).
    // The one exception is markClientKeepAlive(), which is called via VillagerStatsSessionService.recordHeartbeat()
    // from the network thread -- but that write is serialized through ConcurrentHashMap.computeIfPresent(),
    // which provides the necessary visibility guarantee.
    private long lastStatsSnapshotSentGameTime;
    private long lastInventorySnapshotSentGameTime;
    private int lastSentInventoryVersion;
    private long lastTradeCatalogSnapshotSentGameTime;
    private int lastSentCatalogVersion;
    @Nullable
    private VillagerProfessionKey lastSentCatalogProfessionKey;
    private long lastDemandSnapshotSentGameTime;
    private int lastSentDemandVersion;
    private long lastClientAckOrKeepAliveGameTime;

    public void markStatsSnapshotSent(long gameTime) {
        this.lastStatsSnapshotSentGameTime = gameTime;
    }

    public void markInventorySnapshotSent(long gameTime, int inventoryVersion) {
        this.lastInventorySnapshotSentGameTime = gameTime;
        this.lastSentInventoryVersion = inventoryVersion;
    }

    public void markTradeCatalogSnapshotSent(long gameTime, int catalogVersion, @Nonnull VillagerProfessionKey professionKey) {
        this.lastTradeCatalogSnapshotSentGameTime = gameTime;
        this.lastSentCatalogVersion = catalogVersion;
        this.lastSentCatalogProfessionKey = professionKey;
    }

    public void markDemandSnapshotSent(long gameTime, int demandVersion) {
        this.lastDemandSnapshotSentGameTime = gameTime;
        this.lastSentDemandVersion = demandVersion;
    }

    public void markClientKeepAlive(long gameTime) {
        this.lastClientAckOrKeepAliveGameTime = gameTime;
    }

}
