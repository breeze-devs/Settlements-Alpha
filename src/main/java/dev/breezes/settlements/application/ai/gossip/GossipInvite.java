package dev.breezes.settlements.application.ai.gossip;

import java.util.UUID;

/**
 * Lightweight invitation record held by {@link GossipSessionRegistry} until the receiver
 * accepts or the invite expires
 * <p>
 * The invite carries only the session id — the full {@link GossipSession} (including
 * the entry to share) is retrieved from the registry when the receiver accepts.
 *
 * @param sessionId     the owning session
 * @param initiatorId   UUID of the initiating villager
 * @param receiverId    UUID of the intended recipient
 * @param createdAtTick game tick at creation (for timeout accounting)
 * @param expireAtTick  game tick after which this invite should be discarded
 */
public record GossipInvite(
        UUID sessionId,
        UUID initiatorId,
        UUID receiverId,
        long createdAtTick,
        long expireAtTick) {

}
