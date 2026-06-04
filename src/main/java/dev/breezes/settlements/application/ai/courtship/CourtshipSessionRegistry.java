package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ServerScope
@CustomLog
@AllArgsConstructor(onConstructor_ = @Inject)
public final class CourtshipSessionRegistry {

    private static final int DEADLINE_SLACK_TICKS = ClockTicks.seconds(2).getTicksAsInt();
    public static final int INVITE_TIMEOUT_SECONDS = 15;

    private final Map<UUID, CourtshipSession> activeSessionsByParticipant = new ConcurrentHashMap<>();
    private final Map<UUID, SessionParticipants> sessionParticipantsBySessionId = new ConcurrentHashMap<>();
    private final Map<UUID, CourtshipInvite> invitesByReceiver = new ConcurrentHashMap<>();

    public CourtshipSession sendInvite(@Nonnull BaseVillager presenter,
                                       @Nonnull BaseVillager receiver,
                                       int courtshipDurationTicks,
                                       long currentGameTime) {
        UUID sessionId = UUID.randomUUID();
        CourtshipSession session = CourtshipSession.builder()
                .sessionId(sessionId)
                .presenterId(presenter.getUUID())
                .receiverId(receiver.getUUID())
                .courtshipDurationTicks(courtshipDurationTicks)
                .phase(CourtshipPhase.INVITE_SENT)
                .phaseEnteredGameTime(currentGameTime)
                .build();

        long expireGameTime = currentGameTime + ClockTicks.seconds(INVITE_TIMEOUT_SECONDS).getTicks();
        CourtshipInvite invite = new CourtshipInvite(sessionId, presenter.getUUID(), receiver.getUUID(), currentGameTime, expireGameTime);

        putSession(session);
        this.invitesByReceiver.put(receiver.getUUID(), invite);
        log.info("Created courtship invite: sessionId={}, presenter={}, receiver={}", sessionId, presenter.getUUID(), receiver.getUUID());
        return session;
    }

    public boolean hasInviteFor(@Nonnull UUID receiverId) {
        return this.invitesByReceiver.containsKey(receiverId);
    }

    public Optional<CourtshipInvite> getInvite(@Nonnull UUID receiverId) {
        return Optional.ofNullable(this.invitesByReceiver.get(receiverId));
    }

    public CourtshipSession acceptInvite(@Nonnull UUID receiverId, long currentGameTime) {
        Optional.ofNullable(this.invitesByReceiver.remove(receiverId))
                .orElseThrow(() -> new IllegalArgumentException("No pending courtship invite for receiverId=" + receiverId));

        CourtshipSession session = Optional.ofNullable(this.activeSessionsByParticipant.get(receiverId))
                .orElseThrow(() -> new IllegalStateException("Pending courtship invite had no matching active session"));

        session.transitionTo(CourtshipPhase.ACCEPTED, currentGameTime);
        log.info("Accepted courtship invite: sessionId={}, receiverId={}", session.getSessionId(), receiverId);
        return session;
    }

    public Optional<CourtshipSession> getActiveSession(@Nonnull UUID villagerId) {
        return Optional.ofNullable(this.activeSessionsByParticipant.get(villagerId));
    }

    public void closeSession(@Nonnull UUID sessionId, @Nonnull CourtshipCloseReason reason) {
        SessionParticipants participants = this.sessionParticipantsBySessionId.remove(sessionId);
        if (participants == null) {
            return;
        }

        CourtshipSession session = this.activeSessionsByParticipant.remove(participants.firstParticipantId());
        this.activeSessionsByParticipant.remove(participants.secondParticipantId());
        this.invitesByReceiver.remove(participants.firstParticipantId());
        this.invitesByReceiver.remove(participants.secondParticipantId());

        if (session == null) {
            return;
        }

        CourtshipPhase terminalPhase = reason == CourtshipCloseReason.COMPLETED
                ? CourtshipPhase.COMPLETED
                : CourtshipPhase.ABORTED;
        session.transitionTo(terminalPhase, session.getPhaseEnteredGameTime());
        log.info("Closed courtship session: sessionId={}, reason={}, terminalPhase={}", sessionId, reason, terminalPhase);
    }

    /**
     * Safety net for sessions whose behaviors were unloaded without calling onBehaviorStop.
     * Bed release is handled by TeardownScope, which discharges ReleaseHomePoiObligations on every exit path.
     */
    public void tickTimeouts(long currentGameTime) {
        this.invitesByReceiver.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < currentGameTime);

        // Sessions are double-keyed by participant; deduplicate by sessionId before evaluating timeouts.
        Set<UUID> seenSessionIds = new HashSet<>();
        for (CourtshipSession session : this.activeSessionsByParticipant.values()) {
            if (!seenSessionIds.add(session.getSessionId())) {
                continue;
            }

            long maxDurationTicks = maxDurationTicks(session);
            if (maxDurationTicks < 0) {
                continue;
            }
            if ((currentGameTime - session.getPhaseEnteredGameTime()) > maxDurationTicks) {
                log.info("Courtship session timed out: sessionId={}, phase={}", session.getSessionId(), session.getPhase());
                closeSession(session.getSessionId(), CourtshipCloseReason.TIMEOUT);
            }
        }
    }

    private long maxDurationTicks(@Nonnull CourtshipSession session) {
        return switch (session.getPhase()) {
            case INVITE_SENT -> ClockTicks.seconds(INVITE_TIMEOUT_SECONDS).getTicks();
            case ACCEPTED -> ClockTicks.seconds(2).getTicks();
            case APPROACH -> ClockTicks.seconds(15).getTicks() + DEADLINE_SLACK_TICKS;
            // Timeout must exceed duration so the responder's delayed mirror never trips it.
            case COURTSHIP -> (session.getCourtshipDurationTicks() * 2L) + DEADLINE_SLACK_TICKS;
            case BIRTH -> ClockTicks.seconds(5).getTicks() + DEADLINE_SLACK_TICKS;
            case COMPLETED, ABORTED -> -1L;
        };
    }

    private void putSession(@Nonnull CourtshipSession session) {
        SessionParticipants participants = new SessionParticipants(session.getPresenterId(), session.getReceiverId());
        this.activeSessionsByParticipant.put(participants.firstParticipantId(), session);
        this.activeSessionsByParticipant.put(participants.secondParticipantId(), session);
        this.sessionParticipantsBySessionId.put(session.getSessionId(), participants);
    }

    private record SessionParticipants(@Nonnull UUID firstParticipantId, @Nonnull UUID secondParticipantId) {
    }

}
