package dev.breezes.settlements.presentation.ui.sync;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.packet.ServerBoundHeartbeatUiPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Client-side view of the open UI sync session: which channel and villager it addresses, and whether the
 * server is still answering.
 * <p>
 * Liveness is measured against real elapsed time rather than world time, because world time is advanced by the
 * very server these checks exist to catch failing. A stalled server stops advancing the game clock, so a
 * game-clock threshold can never elapse and the staleness it is watching for becomes undetectable.
 */
@ClientSide
@ClientScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class UiClientState implements ClientSessionResettable {

    private static final long HEARTBEAT_INTERVAL_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(2));
    private static final long SNAPSHOT_STALE_THRESHOLD_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(4));
    private static final long HEARTBEAT_ACK_STALE_THRESHOLD_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(6));

    private long activeSessionId = -1L;
    @Nullable
    private UiChannel activeChannel;
    private int activeVillagerEntityId = -1;
    @Nullable
    private String unavailableReasonKey;
    @Getter
    private boolean sessionTerminalUnavailable = false;
    private long nextHeartbeatAtMillis = 0L;
    private long lastSnapshotReceivedAtMillis = 0L;
    private long lastHeartbeatAckReceivedAtMillis = 0L;
    private int snapshotReceiveCount = 0;

    public void openSession(UiChannel channel, long sessionId, int villagerEntityId) {
        this.activeChannel = channel;
        this.activeSessionId = sessionId;
        this.activeVillagerEntityId = villagerEntityId;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        long now = ClientMonotonicClock.nowMillis();
        this.nextHeartbeatAtMillis = now;
        this.lastSnapshotReceivedAtMillis = now;
        this.lastHeartbeatAckReceivedAtMillis = now;
        this.snapshotReceiveCount = 0;
    }

    public void clearSession(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return;
        }
        this.resetSessionState();
    }

    public boolean recordSnapshotReceived(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return false;
        }
        this.lastSnapshotReceivedAtMillis = ClientMonotonicClock.nowMillis();
        this.snapshotReceiveCount++;
        return true;
    }

    public boolean recordHeartbeatAck(long sessionId) {
        if (sessionId != this.activeSessionId) {
            return false;
        }
        this.lastHeartbeatAckReceivedAtMillis = ClientMonotonicClock.nowMillis();
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
        long now = ClientMonotonicClock.nowMillis();
        if (now < this.nextHeartbeatAtMillis) {
            return;
        }
        PacketDistributor.sendToServer(new ServerBoundHeartbeatUiPacket(this.activeChannel, screenSessionId));
        this.nextHeartbeatAtMillis = now + HEARTBEAT_INTERVAL_MILLIS;
    }

    public boolean isSnapshotUpdateStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId || this.snapshotReceiveCount == 0) {
            return false;
        }
        return ClientMonotonicClock.nowMillis() - this.lastSnapshotReceivedAtMillis > SNAPSHOT_STALE_THRESHOLD_MILLIS;
    }

    public boolean isHeartbeatAckStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != this.activeSessionId) {
            return false;
        }
        return ClientMonotonicClock.nowMillis() - this.lastHeartbeatAckReceivedAtMillis > HEARTBEAT_ACK_STALE_THRESHOLD_MILLIS;
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

    @Override
    public void onClientSessionEnded() {
        // Unconditional, unlike clearSession: there is no surviving session whose id could still match, and a
        // session left open here would have the next connection's first screen adopt its liveness timers.
        this.resetSessionState();
    }

    private void resetSessionState() {
        this.activeSessionId = -1L;
        this.activeChannel = null;
        this.activeVillagerEntityId = -1;
        this.unavailableReasonKey = null;
        this.sessionTerminalUnavailable = false;
        this.nextHeartbeatAtMillis = 0L;
        this.lastSnapshotReceivedAtMillis = 0L;
        this.lastHeartbeatAckReceivedAtMillis = 0L;
        this.snapshotReceiveCount = 0;
    }

}
