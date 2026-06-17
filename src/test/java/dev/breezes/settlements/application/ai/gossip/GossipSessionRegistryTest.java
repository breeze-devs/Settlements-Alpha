package dev.breezes.settlements.application.ai.gossip;

import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.time.ClockTicks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GossipSessionRegistry} lifecycle and leak-prevention paths.
 * No Minecraft types are used — all dependencies are plain-Java domain objects.
 */
class GossipSessionRegistryTest {

    private GossipSessionRegistry registry;

    private UUID initiatorId;
    private UUID receiverId;

    @BeforeEach
    void setUp() {
        this.registry = new GossipSessionRegistry();
        this.initiatorId = UUID.randomUUID();
        this.receiverId = UUID.randomUUID();
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void happyPath_sendInviteAcceptComplete_freesBothParticipants() {
        // Arrange
        long tick = 100L;
        KnowledgeEntry entry = knowledgeEntry();

        // Act — initiate
        GossipSession session = this.registry.sendInvite(initiatorId, receiverId, entry, tick);
        UUID sessionId = session.getSessionId();

        // Assert — both participants are locked
        assertTrue(this.registry.isParticipating(initiatorId));
        assertTrue(this.registry.isParticipating(receiverId));
        assertTrue(this.registry.hasInviteFor(receiverId));
        assertEquals(GossipPhase.INVITE_SENT, session.getPhase());

        // Act — accept
        GossipSession accepted = this.registry.acceptInvite(receiverId);

        // Assert — invite consumed, session advanced
        assertFalse(this.registry.hasInviteFor(receiverId),
                "Invite must be consumed on accept so no second receiver can steal it");
        assertEquals(sessionId, accepted.getSessionId());
        assertEquals(GossipPhase.ACCEPTED, accepted.getPhase());
        assertTrue(this.registry.isParticipating(initiatorId),
                "Both participants remain locked during the ACCEPTED phase");
        assertTrue(this.registry.isParticipating(receiverId));

        // Act — complete
        this.registry.completeSession(sessionId);

        // Assert — both participants freed
        assertFalse(this.registry.isParticipating(initiatorId),
                "Initiator must be freed after session completes");
        assertFalse(this.registry.isParticipating(receiverId),
                "Receiver must be freed after session completes");
        assertFalse(this.registry.getActiveSession(initiatorId).isPresent());
        assertFalse(this.registry.getActiveSession(receiverId).isPresent());
    }

    @Test
    void sendInvite_sameInitiatorAndReceiver_throwsAndDoesNotRegisterSession() {
        // Arrange
        long tick = 100L;
        KnowledgeEntry entry = knowledgeEntry();

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> this.registry.sendInvite(initiatorId, initiatorId, entry, tick));

        // Assert
        assertFalse(this.registry.isParticipating(initiatorId));
        assertFalse(this.registry.hasInviteFor(initiatorId));
        assertFalse(this.registry.getActiveSession(initiatorId).isPresent());
    }

    // -------------------------------------------------------------------------
    // Invite TTL reaper
    // -------------------------------------------------------------------------

    @Test
    void inviteTimeout_unacceptedInviteIsAborted_freesBothParticipants() {
        // Arrange
        long startTick = 0L;
        KnowledgeEntry entry = knowledgeEntry();
        GossipSession session = this.registry.sendInvite(initiatorId, receiverId, entry, startTick);

        // Sanity: both locked before timeout
        assertTrue(this.registry.isParticipating(initiatorId));
        assertTrue(this.registry.isParticipating(receiverId));

        // Act — advance time past the invite TTL
        long expiredTick = startTick
                + ClockTicks.seconds(GossipSessionRegistry.INVITE_TIMEOUT_SECONDS).getTicks()
                + 1L;
        this.registry.tickTimeouts(expiredTick);

        // Assert — invite gone, session aborted, both participants freed
        assertFalse(this.registry.hasInviteFor(receiverId),
                "Invite must be evicted after TTL");
        assertFalse(this.registry.isParticipating(initiatorId),
                "Initiator must be freed when invite times out");
        assertFalse(this.registry.isParticipating(receiverId),
                "Receiver must be freed when invite times out");
        assertEquals(GossipPhase.ABORTED, session.getPhase());
    }

    @Test
    void inviteTimeout_notYetExpired_doesNotAbort() {
        // Arrange
        long startTick = 0L;
        this.registry.sendInvite(initiatorId, receiverId, knowledgeEntry(), startTick);

        // Act — tick just before expiry
        long justBeforeExpiry = startTick
                + ClockTicks.seconds(GossipSessionRegistry.INVITE_TIMEOUT_SECONDS).getTicks()
                - 1L;
        this.registry.tickTimeouts(justBeforeExpiry);

        // Assert — invite and participants still present
        assertTrue(this.registry.hasInviteFor(receiverId));
        assertTrue(this.registry.isParticipating(initiatorId));
        assertTrue(this.registry.isParticipating(receiverId));
    }

    // -------------------------------------------------------------------------
    // FIX #2 regression — ACCEPTED session whose cue never completes must be reaped
    // -------------------------------------------------------------------------

    @Test
    void sessionMaxLifetime_acceptedSessionThatNeverCompletes_isAborted_freesBothParticipants() {
        // Arrange — open and accept a session, then never call completeSession
        long startTick = 0L;
        GossipSession session = this.registry.sendInvite(initiatorId, receiverId, knowledgeEntry(), startTick);
        this.registry.acceptInvite(receiverId);

        // Verify the session is in ACCEPTED phase and participants are locked
        assertEquals(GossipPhase.ACCEPTED, session.getPhase());
        assertTrue(this.registry.isParticipating(initiatorId));
        assertTrue(this.registry.isParticipating(receiverId));

        // Act — advance time past the session max lifetime without completing the session
        long expiredTick = startTick
                + ClockTicks.seconds(GossipSessionRegistry.SESSION_MAX_LIFETIME_SECONDS).getTicks()
                + 1L;
        this.registry.tickTimeouts(expiredTick);

        // Assert — reaper must free both participants even though onComplete never fired
        assertFalse(this.registry.isParticipating(initiatorId),
                "Initiator must be freed by the max-lifetime reaper");
        assertFalse(this.registry.isParticipating(receiverId),
                "Receiver must be freed by the max-lifetime reaper");
        assertEquals(GossipPhase.ABORTED, session.getPhase(),
                "Session must be transitioned to ABORTED by the reaper");
    }

    @Test
    void sessionMaxLifetime_notYetExpired_doesNotAbort() {
        // Arrange — accept a session
        long startTick = 0L;
        this.registry.sendInvite(initiatorId, receiverId, knowledgeEntry(), startTick);
        this.registry.acceptInvite(receiverId);

        // Act — tick just before max lifetime
        long justBeforeExpiry = startTick
                + ClockTicks.seconds(GossipSessionRegistry.SESSION_MAX_LIFETIME_SECONDS).getTicks()
                - 1L;
        this.registry.tickTimeouts(justBeforeExpiry);

        // Assert — session still alive
        assertTrue(this.registry.isParticipating(initiatorId));
        assertTrue(this.registry.isParticipating(receiverId));
    }

    @Test
    void sessionMaxLifetime_completedSessionIsNotTouchedByReaper() {
        // Arrange — run a full happy-path session to completion
        long startTick = 0L;
        GossipSession session = this.registry.sendInvite(initiatorId, receiverId, knowledgeEntry(), startTick);
        this.registry.acceptInvite(receiverId);
        this.registry.completeSession(session.getSessionId());

        assertEquals(GossipPhase.COMPLETED, session.getPhase());

        // Act — tick well past the max lifetime
        long veryLate = startTick + ClockTicks.seconds(GossipSessionRegistry.SESSION_MAX_LIFETIME_SECONDS).getTicks() + 1000L;
        // Should not throw; the reaper skips terminal sessions
        this.registry.tickTimeouts(veryLate);

        // Assert — completed phase is preserved (reaper must not transition terminal sessions)
        assertEquals(GossipPhase.COMPLETED, session.getPhase());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static KnowledgeEntry knowledgeEntry() {
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "test fact",
                ObservationType.RESOURCE,
                0L,
                0L,
                null,
                Map.of(),
                1.5f);
    }

}
