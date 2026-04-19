package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.application.ui.stats.model.VillagerDemandDisplaySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerInventorySnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.application.ui.stats.model.VillagerTradeCatalogSnapshot;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.infrastructure.network.features.ui.stats.packet.ServerBoundHeartbeatVillagerStatsPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;

/**
 * Tracks the client-side state of an active villager stats session.
 * <p>
 * All access must occur on the Minecraft client thread (render/tick).
 * Not thread-safe by design — Minecraft's client is single-threaded.
 * <p>
 * The client only needs one process-lifetime instance, but keeping that lifetime
 * in the Dagger graph avoids leaking static state between tests and sessions.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class VillagerStatsClientState {

    private static final int HEARTBEAT_INTERVAL_TICKS = 40;
    private static final int SNAPSHOT_STALE_THRESHOLD_TICKS = 20;
    private static final int HEARTBEAT_ACK_STALE_THRESHOLD_TICKS = 120;

    private long activeSessionId = -1L;
    private int activeVillagerEntityId = -1;

    @Nullable
    private VillagerStatsSnapshot latestStatsSnapshot;
    @Nullable
    private VillagerInventorySnapshot latestInventorySnapshot;
    @Nullable
    private VillagerTradeCatalogSnapshot latestTradeCatalogSnapshot;
    @Nullable
    private VillagerDemandDisplaySnapshot latestDemandSnapshot;
    @Nullable
    private String unavailableReasonKey;
    private boolean sessionTerminalUnavailable = false;
    private long nextHeartbeatGameTime = 0L;
    private long lastSnapshotReceivedGameTime = 0L;
    private long lastHeartbeatAckReceivedGameTime = 0L;
    private int snapshotReceiveCount = 0;

    public void openSession(long sessionId, int villagerEntityId) {
        this.activeSessionId = sessionId;
        this.activeVillagerEntityId = villagerEntityId;
        this.latestStatsSnapshot = null;
        this.latestInventorySnapshot = null;
        this.latestTradeCatalogSnapshot = null;
        this.latestDemandSnapshot = null;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatGameTime = 0L;
        this.lastSnapshotReceivedGameTime = 0L;
        this.lastHeartbeatAckReceivedGameTime = 0L;
        this.snapshotReceiveCount = 0;
    }

    public boolean applyStatsSnapshot(long sessionId, @Nonnull VillagerStatsSnapshot snapshot) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.latestStatsSnapshot = snapshot;
        this.unavailableReasonKey = null;
        Minecraft minecraft = Minecraft.getInstance();
        long observedGameTime = minecraft.level != null ? minecraft.level.getGameTime() : snapshot.gameTime();
        this.lastSnapshotReceivedGameTime = observedGameTime;
        this.lastHeartbeatAckReceivedGameTime = observedGameTime;
        this.snapshotReceiveCount++;
        return true;
    }

    public boolean applyInventorySnapshot(long sessionId, @Nonnull VillagerInventorySnapshot inventory) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.latestInventorySnapshot = inventory;
        return true;
    }

    public boolean applyTradeCatalogSnapshot(long sessionId, @Nonnull VillagerTradeCatalogSnapshot snapshot) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.latestTradeCatalogSnapshot = snapshot;
        return true;
    }

    public boolean applyDemandSnapshot(long sessionId, @Nonnull VillagerDemandDisplaySnapshot snapshot) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.latestDemandSnapshot = snapshot;
        return true;
    }

    public boolean recordHeartbeatAck(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            this.lastHeartbeatAckReceivedGameTime = minecraft.level.getGameTime();
            return true;
        }

        if (this.latestStatsSnapshot != null) {
            this.lastHeartbeatAckReceivedGameTime = this.latestStatsSnapshot.gameTime();
            return true;
        }

        return false;
    }

    public boolean markUnavailable(long sessionId, @Nonnull String reasonKey) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.unavailableReasonKey = reasonKey;
        this.sessionTerminalUnavailable = true;
        return true;
    }

    public void clearSession(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return;
        }
        this.forceClearSession();
    }

    public void forceClearSession() {
        this.activeSessionId = -1L;
        this.activeVillagerEntityId = -1;
        this.latestStatsSnapshot = null;
        this.latestInventorySnapshot = null;
        this.latestTradeCatalogSnapshot = null;
        this.latestDemandSnapshot = null;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatGameTime = 0L;
        this.lastSnapshotReceivedGameTime = 0L;
        this.lastHeartbeatAckReceivedGameTime = 0L;
        this.snapshotReceiveCount = 0;
    }

    public boolean hasActiveSession() {
        return this.activeSessionId > 0;
    }

    public long activeSessionId() {
        return this.activeSessionId;
    }

    public int activeVillagerEntityId() {
        return this.activeVillagerEntityId;
    }

    public Optional<VillagerStatsSnapshot> latestStatsSnapshot() {
        return Optional.ofNullable(this.latestStatsSnapshot);
    }

    public Optional<VillagerInventorySnapshot> latestInventorySnapshot() {
        return Optional.ofNullable(this.latestInventorySnapshot);
    }

    public Optional<VillagerTradeCatalogSnapshot> latestTradeCatalogSnapshot() {
        return Optional.ofNullable(this.latestTradeCatalogSnapshot);
    }

    public Optional<VillagerDemandDisplaySnapshot> latestDemandSnapshot() {
        return Optional.ofNullable(this.latestDemandSnapshot);
    }

    public boolean isSnapshotUpdateStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return false;
        }

        if (this.snapshotReceiveCount < 2) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        return (gameTime - this.lastSnapshotReceivedGameTime) > SNAPSHOT_STALE_THRESHOLD_TICKS;
    }

    public boolean isHeartbeatAckStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return false;
        }

        if (this.lastHeartbeatAckReceivedGameTime <= 0L) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        return (gameTime - this.lastHeartbeatAckReceivedGameTime) > HEARTBEAT_ACK_STALE_THRESHOLD_TICKS;
    }

    public void tickHeartbeatIfNeeded(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return;
        }

        if (this.sessionTerminalUnavailable) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime < this.nextHeartbeatGameTime) {
            return;
        }

        PacketDistributor.sendToServer(new ServerBoundHeartbeatVillagerStatsPacket(screenSessionId));
        this.nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
    }

}
