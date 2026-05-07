package dev.breezes.settlements.application.ui.sync.session;

import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class UiSession {

    private final long sessionId;
    private final UiChannel channel;
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
