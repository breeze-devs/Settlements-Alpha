package dev.breezes.settlements.application.ai.gossip;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Tracks the state of a paired gossip exchange between two villagers.
 * <p>
 * A session is born when the initiator admits the gossip cue, and lives until:
 * - The receiver's matching cue completes (COMPLETED).
 * - The invite times out before the receiver admits (ABORTED).
 * <p>
 * The exchange is presentation only — the session pairs the two villagers so each can
 * aim at the other, and carries no payload between them.
 */
@Builder
@Getter
public final class GossipSession {

    private final UUID sessionId;

    /**
     * UUID of the villager who initiated the gossip.
     */
    private final UUID initiatorId;

    /**
     * UUID of the villager who was invited to gossip.
     */
    private final UUID receiverId;

    /**
     * Game tick at which this session was opened.
     * Used by the reaper to time out stale invites.
     */
    private final long openedAtTick;

    private GossipPhase phase;


    public void transitionTo(GossipPhase newPhase) {
        this.phase = newPhase;
    }

}
