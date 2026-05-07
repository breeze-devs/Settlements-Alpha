package dev.breezes.settlements.presentation.ui.sync;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundHeartbeatUiPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Optional;

@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class UiClientState {

    private static final int HEARTBEAT_INTERVAL_TICKS = Ticks.seconds(2).getTicksAsInt();
    private static final int SNAPSHOT_STALE_THRESHOLD_TICKS = Ticks.seconds(4).getTicksAsInt();
    private static final int HEARTBEAT_ACK_STALE_THRESHOLD_TICKS = Ticks.seconds(6).getTicksAsInt();

    private long activeSessionId = -1L;
    @Nullable
    private UiChannel activeChannel;
    private int activeVillagerEntityId = -1;
    @Nullable
    private String unavailableReasonKey;
    private boolean sessionTerminalUnavailable = false;
    private long nextHeartbeatGameTime = 0L;
    private long lastSnapshotReceivedGameTime = 0L;
    private long lastHeartbeatAckReceivedGameTime = 0L;
    private int snapshotReceiveCount = 0;

    public void openSession(UiChannel channel, long sessionId, int villagerEntityId) {
        this.activeChannel = channel;
        this.activeSessionId = sessionId;
        this.activeVillagerEntityId = villagerEntityId;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        long gameTime = currentClientGameTime();
        this.nextHeartbeatGameTime = gameTime;
        this.lastSnapshotReceivedGameTime = gameTime;
        this.lastHeartbeatAckReceivedGameTime = gameTime;
        this.snapshotReceiveCount = 0;
    }

    public void clearSession(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return;
        }
        this.activeSessionId = -1L;
        this.activeChannel = null;
        this.activeVillagerEntityId = -1;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatGameTime = 0L;
        this.lastSnapshotReceivedGameTime = 0L;
        this.lastHeartbeatAckReceivedGameTime = 0L;
        this.snapshotReceiveCount = 0;
    }

    public boolean recordSnapshotReceived(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return false;
        }
        this.lastSnapshotReceivedGameTime = currentClientGameTime();
        this.snapshotReceiveCount++;
        return true;
    }

    public boolean recordHeartbeatAck(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return false;
        }
        this.lastHeartbeatAckReceivedGameTime = currentClientGameTime();
        return true;
    }

    public boolean markUnavailable(long sessionId, String reasonKey) {
        if (sessionId > 0 && sessionId != this.activeSessionId) {
            return false;
        }
        this.unavailableReasonKey = reasonKey;
        this.sessionTerminalUnavailable = true;
        return true;
    }

    public void tickHeartbeatIfNeeded(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId || this.activeChannel == null) {
            return;
        }
        long gameTime = currentClientGameTime();
        if (gameTime < this.nextHeartbeatGameTime) {
            return;
        }
        PacketDistributor.sendToServer(new ServerBoundHeartbeatUiPacket(this.activeChannel, screenSessionId));
        this.nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
    }

    public boolean isSnapshotUpdateStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId || this.snapshotReceiveCount == 0) {
            return false;
        }
        return currentClientGameTime() - this.lastSnapshotReceivedGameTime > SNAPSHOT_STALE_THRESHOLD_TICKS;
    }

    public boolean isHeartbeatAckStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return false;
        }
        return currentClientGameTime() - this.lastHeartbeatAckReceivedGameTime > HEARTBEAT_ACK_STALE_THRESHOLD_TICKS;
    }

    public long activeSessionId() {
        return this.activeSessionId;
    }

    @Nullable
    public UiChannel activeChannel() {
        return this.activeChannel;
    }

    public int activeVillagerEntityId() {
        return this.activeVillagerEntityId;
    }

    public Optional<String> unavailableReasonKey() {
        return Optional.ofNullable(this.unavailableReasonKey);
    }

    public boolean isSessionTerminalUnavailable() {
        return this.sessionTerminalUnavailable;
    }

    private static long currentClientGameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

}
