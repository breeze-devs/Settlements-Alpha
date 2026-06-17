package dev.breezes.settlements.application.ai.gossip;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-scope registry for in-flight gossip sessions.
 * <p>
 * Models a strict 1:1 initiate/accept handshake: each session has exactly one initiator and
 * one receiver. The registry is the state of record; events are announcements. A pending invite
 * is stored here until the receiver's arbiter admits the matching cue (via {@code onAdmit}),
 * at which point the invite is consumed and the session advances to ACCEPTED.
 * <p>
 * A gossip session is lighter than a courtship session: it has no multi-phase choreography,
 * no bed reservation, and the knowledge transfer is a single atomic write on completion.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(onConstructor_ = @Inject)
public final class GossipSessionRegistry {

    /**
     * How long a gossip invite stays open before it is discarded
     * <p>
     * Short because gossip is opportunistic — if the villager isn't nearby right now,
     * the initiator will try again the next time they meet.
     */
    public static final int INVITE_TIMEOUT_SECONDS = 10;

    /**
     * Absolute upper bound on any non-terminal session's lifetime.
     * <p>
     * A session in ACCEPTED phase whose cue never completes (e.g. the receiver chunk-unloads
     * or dies mid-cue) would otherwise leak forever, permanently locking both participants out
     * of future gossip via {@link #isParticipating}.
     */
    public static final int SESSION_MAX_LIFETIME_SECONDS = 20;

    /**
     * Active sessions keyed by both participants so either can look up the session.
     */
    private final Map<UUID, GossipSession> activeSessionsByParticipant = new ConcurrentHashMap<>();

    /**
     * Pending invites keyed by receiver UUID. The receiver checks this map each tick.
     */
    private final Map<UUID, GossipInvite> invitesByReceiver = new ConcurrentHashMap<>();

    /**
     * Session objects keyed by session id for O(1) close and reaper access.
     */
    private final Map<UUID, GossipSession> sessionsById = new ConcurrentHashMap<>();


    /**
     * Opens a new gossip session from the initiator to the receiver and registers
     * a pending invite for the receiver to pick up.
     *
     * @param initiatorId  UUID of the villager sharing knowledge
     * @param receiverId   UUID of the intended recipient
     * @param entryToShare the knowledge entry the initiator wants to share
     * @param currentTick  current game tick (for invite timeout accounting)
     * @return the newly created session
     */
    public GossipSession sendInvite(@Nonnull UUID initiatorId,
                                    @Nonnull UUID receiverId,
                                    @Nonnull KnowledgeEntry entryToShare,
                                    long currentTick) {
        if (initiatorId.equals(receiverId)) {
            throw new IllegalArgumentException("Gossip initiator and receiver must be different villagers");
        }

        UUID sessionId = UUID.randomUUID();
        GossipSession session = GossipSession.builder()
                .sessionId(sessionId)
                .initiatorId(initiatorId)
                .receiverId(receiverId)
                .entryToShare(entryToShare)
                .openedAtTick(currentTick)
                .phase(GossipPhase.INVITE_SENT)
                .build();

        long expireAtTick = currentTick + ClockTicks.seconds(INVITE_TIMEOUT_SECONDS).getTicks();
        GossipInvite invite = new GossipInvite(sessionId, initiatorId, receiverId, currentTick, expireAtTick);

        this.sessionsById.put(sessionId, session);
        this.activeSessionsByParticipant.put(initiatorId, session);
        this.activeSessionsByParticipant.put(receiverId, session);
        this.invitesByReceiver.put(receiverId, invite);

        log.info("GossipSession opened: sessionId={}, initiator={}, receiver={}, origin={}",
                sessionId, initiatorId, receiverId, entryToShare.getOriginObservationId());
        return session;
    }

    /**
     * Returns {@code true} if there is a pending gossip invite waiting for the given receiver
     */
    public boolean hasInviteFor(@Nonnull UUID receiverId) {
        return this.invitesByReceiver.containsKey(receiverId);
    }

    /**
     * Returns the pending invite for the receiver, if any
     */
    public Optional<GossipInvite> getInvite(@Nonnull UUID receiverId) {
        return Optional.ofNullable(this.invitesByReceiver.get(receiverId));
    }

    /**
     * Returns the active session for the given participant, if any
     */
    public Optional<GossipSession> getActiveSession(@Nonnull UUID participantId) {
        return Optional.ofNullable(this.activeSessionsByParticipant.get(participantId));
    }

    /**
     * Returns {@code true} if the given villager is already participating in a gossip session
     */
    public boolean isParticipating(@Nonnull UUID villagerId) {
        return this.activeSessionsByParticipant.containsKey(villagerId);
    }

    /**
     * Records that the receiver has accepted the invite and advances the session to ACCEPTED
     * Removes the invite from the pending map so no second receiver can steal it
     *
     * @param receiverId UUID of the accepting villager
     * @return the accepted session
     * @throws IllegalStateException if there is no pending invite for this receiver
     */
    public GossipSession acceptInvite(@Nonnull UUID receiverId) {
        GossipInvite invite = this.invitesByReceiver.remove(receiverId);
        if (invite == null) {
            throw new IllegalStateException("No pending gossip invite for receiverId=" + receiverId);
        }

        GossipSession session = this.sessionsById.get(invite.sessionId());
        if (session == null) {
            throw new IllegalStateException("Gossip invite had no matching session: sessionId=" + invite.sessionId());
        }

        session.transitionTo(GossipPhase.ACCEPTED);
        log.info("GossipSession accepted: sessionId={}, receiver={}", session.getSessionId(), receiverId);
        return session;
    }

    /**
     * Closes the session as COMPLETED after the knowledge copy has been written
     */
    public void completeSession(UUID sessionId) {
        closeSession(sessionId, GossipPhase.COMPLETED);
    }

    /**
     * Closes the session as ABORTED (timeout, range, etc.)
     */
    public void abortSession(UUID sessionId) {
        closeSession(sessionId, GossipPhase.ABORTED);
    }

    private void closeSession(UUID sessionId, GossipPhase terminalPhase) {
        GossipSession session = this.sessionsById.remove(sessionId);
        if (session == null) {
            return;
        }

        session.transitionTo(terminalPhase);
        this.activeSessionsByParticipant.remove(session.getInitiatorId());
        this.activeSessionsByParticipant.remove(session.getReceiverId());
        this.invitesByReceiver.remove(session.getReceiverId());

        log.info("GossipSession closed: sessionId={}, phase={}", sessionId, terminalPhase);
    }

    /**
     * Evicts expired invites and aborts sessions that have outlived their max lifetime
     * <p>
     * Two distinct cases:
     * <ol>
     *   <li><b>INVITE_SENT</b>: invite TTL check. The invite entry drives cleanup — once
     *       it expires, the session is aborted before the entry is removed.</li>
     *   <li><b>ACCEPTED (and any non-terminal phase)</b>: session max-lifetime check.
     *       If the receiver's cue never completes (chunk-unload, death mid-cue) the
     *       {@code onComplete} callback never fires, leaving the session permanently open
     *       and blocking both participants from future gossip. The max-lifetime ceiling
     *       reclaims these orphaned sessions.</li>
     * </ol>
     */
    public void tickTimeouts(long currentTick) {
        // Abort sessions whose invite TTL has expired and are still in INVITE_SENT.
        this.invitesByReceiver.entrySet().removeIf(entry -> {
            if (entry.getValue().expireAtTick() < currentTick) {
                UUID sessionId = entry.getValue().sessionId();
                log.debug("GossipSession invite timed out: sessionId={}, receiver={}", sessionId, entry.getKey());

                // Close the session if it hasn't been accepted yet.
                GossipSession session = this.sessionsById.get(sessionId);
                if (session != null && session.getPhase() == GossipPhase.INVITE_SENT) {
                    closeSession(sessionId, GossipPhase.ABORTED);
                }
                return true;
            }
            return false;
        });

        // Abort any non-terminal session that has exceeded the absolute max lifetime
        long maxLifetimeTicks = ClockTicks.seconds(SESSION_MAX_LIFETIME_SECONDS).getTicks();
        List<UUID> expiredSessionIds = new ArrayList<>();
        for (GossipSession session : this.sessionsById.values()) {
            if (session.getPhase() == GossipPhase.COMPLETED || session.getPhase() == GossipPhase.ABORTED) {
                continue;
            }
            if (currentTick - session.getOpenedAtTick() > maxLifetimeTicks) {
                expiredSessionIds.add(session.getSessionId());
            }
        }

        for (UUID sessionId : expiredSessionIds) {
            log.debug("GossipSession exceeded max lifetime: sessionId={}", sessionId);
            closeSession(sessionId, GossipPhase.ABORTED);
        }
    }

}
