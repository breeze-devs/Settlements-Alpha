package dev.breezes.settlements.application.ai.gossip;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Tracks the state of a paired gossip exchange between two villagers.
 * <p>
 * A session is born when the initiator admits the gossip cue and immediately
 * deposits the entry to share. The session lives until:
 * <ul>
 *   <li>The receiver's matching cue completes and the knowledge copy lands (COMPLETED).</li>
 *   <li>The invite times out before the receiver admits (ABORTED).</li>
 * </ul>
 * <p>
 * The {@link #entryToShare} is resolved by the initiator before the session is
 * created — it is the single most-important shareable entry from the initiator's store,
 * selected by the gossip cue trigger. The receiver writes a copy into their own store
 * on session completion.
 */
@Builder
@Getter
public final class GossipSession {

    private final UUID sessionId;

    /**
     * UUID of the villager who initiated the gossip (the one sharing knowledge).
     */
    private final UUID initiatorId;

    /**
     * UUID of the villager who will receive the knowledge.
     */
    private final UUID receiverId;

    /**
     * The knowledge entry the initiator intends to share.
     * Set at session creation; never changed — the knowledge transfer is
     * determined at initiation time, not at completion time, so the receiver
     * gets a consistent copy regardless of how long the cue takes.
     */
    private final KnowledgeEntry entryToShare;

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
