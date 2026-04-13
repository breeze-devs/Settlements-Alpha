package dev.breezes.settlements.presentation.ui.behavior;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundHeartbeatBehaviorControllerPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;

@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class BehaviorControllerClientState {

    private static final int HEARTBEAT_INTERVAL_TICKS = 40;
    private static final int SNAPSHOT_STALE_THRESHOLD_TICKS = 20;
    private static final int HEARTBEAT_ACK_STALE_THRESHOLD_TICKS = 120;

    private long activeSessionId = -1L;
    private int activeVillagerEntityId = -1;

    @Nullable
    private BehaviorControllerSnapshot latestSnapshot;
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
        this.latestSnapshot = null;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatGameTime = 0L;
        this.lastSnapshotReceivedGameTime = 0L;
        this.lastHeartbeatAckReceivedGameTime = 0L;
        this.snapshotReceiveCount = 0;
    }

    public boolean applySnapshot(long sessionId, @Nonnull BehaviorControllerSnapshot snapshot) {
        if (sessionId != this.activeSessionId) {
            return false;
        }

        this.latestSnapshot = snapshot;
        this.unavailableReasonKey = null;
        Minecraft minecraft = Minecraft.getInstance();
        long observedGameTime = minecraft.level != null ? minecraft.level.getGameTime() : snapshot.gameTime();
        this.lastSnapshotReceivedGameTime = observedGameTime;
        // Snapshot receipt implies the server-to-client path is healthy.
        this.lastHeartbeatAckReceivedGameTime = observedGameTime;
        this.snapshotReceiveCount++;
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

        if (this.latestSnapshot != null) {
            this.lastHeartbeatAckReceivedGameTime = this.latestSnapshot.gameTime();
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

        this.activeSessionId = -1L;
        this.activeVillagerEntityId = -1;
        this.latestSnapshot = null;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatGameTime = 0L;
        this.lastSnapshotReceivedGameTime = 0L;
        this.lastHeartbeatAckReceivedGameTime = 0L;
        this.snapshotReceiveCount = 0;
    }

    public long activeSessionId() {
        return this.activeSessionId;
    }

    public int activeVillagerEntityId() {
        return this.activeVillagerEntityId;
    }

    public Optional<BehaviorControllerSnapshot> latestSnapshot() {
        return Optional.ofNullable(this.latestSnapshot);
    }

    public Optional<String> unavailableReasonKey() {
        return Optional.ofNullable(this.unavailableReasonKey);
    }

    public boolean isSnapshotUpdateStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return false;
        }

        // Avoid false alarms before we've established that periodic updates are expected.
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

        PacketDistributor.sendToServer(new ServerBoundHeartbeatBehaviorControllerPacket(screenSessionId));
        this.nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
    }

}
