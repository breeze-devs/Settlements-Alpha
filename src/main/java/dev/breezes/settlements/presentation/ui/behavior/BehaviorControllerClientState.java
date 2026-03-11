package dev.breezes.settlements.presentation.ui.behavior;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.packet.ServerBoundHeartbeatBehaviorControllerPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Global singleton for client-side behavior controller state tracking
 * <p>
 * This should be fine since there's a single client per mod instance
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ClientSide
public final class BehaviorControllerClientState {

    private static final int HEARTBEAT_INTERVAL_TICKS = 40;
    private static final int SNAPSHOT_STALE_THRESHOLD_TICKS = 20;
    private static final int HEARTBEAT_ACK_STALE_THRESHOLD_TICKS = 120;

    private static long activeSessionId = -1L;
    private static int activeVillagerEntityId = -1;

    @Nullable
    private static BehaviorControllerSnapshot latestSnapshot;
    @Nullable
    private static String unavailableReasonKey;
    private static boolean sessionTerminalUnavailable = false;
    private static long nextHeartbeatGameTime = 0L;
    private static long lastSnapshotReceivedGameTime = 0L;
    private static long lastHeartbeatAckReceivedGameTime = 0L;
    private static int snapshotReceiveCount = 0;

    public static void openSession(long sessionId, int villagerEntityId) {
        activeSessionId = sessionId;
        activeVillagerEntityId = villagerEntityId;
        latestSnapshot = null;
        unavailableReasonKey = null;
        sessionTerminalUnavailable = false;
        nextHeartbeatGameTime = 0L;
        lastSnapshotReceivedGameTime = 0L;
        lastHeartbeatAckReceivedGameTime = 0L;
        snapshotReceiveCount = 0;
    }

    public static boolean applySnapshot(long sessionId, @Nonnull BehaviorControllerSnapshot snapshot) {
        if (sessionId != activeSessionId) {
            return false;
        }

        latestSnapshot = snapshot;
        unavailableReasonKey = null;
        Minecraft minecraft = Minecraft.getInstance();
        long observedGameTime = minecraft.level != null ? minecraft.level.getGameTime() : snapshot.gameTime();
        lastSnapshotReceivedGameTime = observedGameTime;
        // Snapshot receipt implies the server-to-client path is healthy.
        lastHeartbeatAckReceivedGameTime = observedGameTime;
        snapshotReceiveCount++;
        return true;
    }

    public static boolean recordHeartbeatAck(long sessionId) {
        if (sessionId != activeSessionId) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            lastHeartbeatAckReceivedGameTime = minecraft.level.getGameTime();
            return true;
        }

        if (latestSnapshot != null) {
            lastHeartbeatAckReceivedGameTime = latestSnapshot.gameTime();
            return true;
        }

        return false;
    }

    public static boolean markUnavailable(long sessionId, @Nonnull String reasonKey) {
        if (sessionId != activeSessionId) {
            return false;
        }

        unavailableReasonKey = reasonKey;
        sessionTerminalUnavailable = true;
        return true;
    }

    public static void clearSession(long sessionId) {
        if (sessionId != activeSessionId) {
            return;
        }

        activeSessionId = -1L;
        activeVillagerEntityId = -1;
        latestSnapshot = null;
        unavailableReasonKey = null;
        sessionTerminalUnavailable = false;
        nextHeartbeatGameTime = 0L;
        lastSnapshotReceivedGameTime = 0L;
        lastHeartbeatAckReceivedGameTime = 0L;
        snapshotReceiveCount = 0;
    }

    public static long activeSessionId() {
        return activeSessionId;
    }

    public static int activeVillagerEntityId() {
        return activeVillagerEntityId;
    }

    public static Optional<BehaviorControllerSnapshot> latestSnapshot() {
        return Optional.ofNullable(latestSnapshot);
    }

    public static Optional<String> unavailableReasonKey() {
        return Optional.ofNullable(unavailableReasonKey);
    }

    public static boolean isSnapshotUpdateStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != activeSessionId) {
            return false;
        }

        // Avoid false alarms before we've established that periodic updates are expected.
        if (snapshotReceiveCount < 2) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        return (gameTime - lastSnapshotReceivedGameTime) > SNAPSHOT_STALE_THRESHOLD_TICKS;
    }

    public static boolean isHeartbeatAckStale(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != activeSessionId) {
            return false;
        }

        if (lastHeartbeatAckReceivedGameTime <= 0L) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }

        long gameTime = minecraft.level.getGameTime();
        return (gameTime - lastHeartbeatAckReceivedGameTime) > HEARTBEAT_ACK_STALE_THRESHOLD_TICKS;
    }

    public static void tickHeartbeatIfNeeded(long screenSessionId) {
        if (screenSessionId <= 0 || screenSessionId != activeSessionId) {
            return;
        }

        if (sessionTerminalUnavailable) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime < nextHeartbeatGameTime) {
            return;
        }

        PacketDistributor.sendToServer(new ServerBoundHeartbeatBehaviorControllerPacket(screenSessionId));
        nextHeartbeatGameTime = gameTime + HEARTBEAT_INTERVAL_TICKS;
    }

}
