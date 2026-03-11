package dev.breezes.settlements.application.ui.behavior.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Builder
@Getter
public class BehaviorControllerSession {

    private final long sessionId;
    private final UUID playerUuid;
    private final int villagerEntityId;
    private final long openedAtGameTime;
    private volatile long lastSentGameTime;
    private volatile long lastClientAckOrKeepAliveGameTime;

    public void markSnapshotSent(long gameTime) {
        this.lastSentGameTime = gameTime;
    }

    public void markClientKeepAlive(long gameTime) {
        this.lastClientAckOrKeepAliveGameTime = gameTime;
    }

}
