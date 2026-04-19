package dev.breezes.settlements.application.ai.trading;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.world.item.Item;

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
public final class TradeSessionRegistry {

    private static final int DEADLINE_SLACK_TICKS = Ticks.seconds(2).getTicksAsInt();

    private final TradingConfig tradingConfig;

    private final Map<UUID, TradeSession> activeSessionsByParticipant = new ConcurrentHashMap<>();
    private final Map<UUID, SessionParticipants> sessionParticipantsBySessionId = new ConcurrentHashMap<>();
    private final Map<UUID, TradeInvite> invitesByTarget = new ConcurrentHashMap<>();

    @Inject
    public TradeSessionRegistry(@Nonnull TradingConfig tradingConfig) {
        this.tradingConfig = tradingConfig;
    }

    public TradeSession sendInvite(@Nonnull BaseVillager initiator,
                                   @Nonnull BaseVillager target,
                                   @Nonnull UUID buyerId,
                                   @Nonnull UUID sellerId,
                                   @Nonnull Item matchedItem,
                                   int bundleSize,
                                   int priceJitter,
                                   int buyerOffer,
                                   int sellerAsk,
                                   int negotiationRoundsRemaining,
                                   long currentGameTime) {
        UUID sessionId = UUID.randomUUID();
        TradeSession session = TradeSession.builder()
                .sessionId(sessionId)
                .initiatorId(initiator.getUUID())
                .responderId(target.getUUID())
                .buyerId(buyerId)
                .sellerId(sellerId)
                .matchedItem(matchedItem)
                .bundleSize(bundleSize)
                .priceJitter(priceJitter)
                .buyerOffer(buyerOffer)
                .sellerAsk(sellerAsk)
                .negotiationRoundsRemaining(negotiationRoundsRemaining)
                .phase(TradeSessionPhase.SYN_SENT)
                .phaseEnteredGameTime(currentGameTime)
                .build();

        long inviteExpireGameTime = currentGameTime + Ticks.seconds(this.tradingConfig.inviteTimeoutSeconds()).getTicks();
        TradeInvite invite = new TradeInvite(sessionId, initiator.getUUID(), target.getUUID(), currentGameTime, inviteExpireGameTime);

        putSession(session);
        this.invitesByTarget.put(target.getUUID(), invite);
        log.info("Created trade invite: sessionId={}, initiator={}, responder={}, buyer={}, seller={}, item={}, bundleSize={}, priceJitter={}, buyerOffer={}, sellerAsk={}",
                sessionId, initiator.getUUID(), target.getUUID(), buyerId, sellerId, matchedItem, bundleSize, priceJitter, buyerOffer, sellerAsk);
        return session;
    }

    public boolean hasInviteFor(@Nonnull UUID targetId) {
        return this.invitesByTarget.containsKey(targetId);
    }

    public Optional<TradeInvite> getInvite(@Nonnull UUID targetId) {
        return Optional.ofNullable(this.invitesByTarget.get(targetId));
    }

    public TradeSession acceptInvite(@Nonnull UUID targetId, long currentGameTime) {
        TradeInvite invite = Optional.ofNullable(this.invitesByTarget.remove(targetId))
                .orElseThrow(() -> new IllegalArgumentException("No pending trade invite exists for targetId=" + targetId));

        TradeSession session = Optional.ofNullable(this.activeSessionsByParticipant.get(targetId))
                .orElseThrow(() -> new IllegalStateException("Pending trade invite had no matching active session"));

        session.transitionTo(TradeSessionPhase.ESTABLISHED, currentGameTime);
        log.info("Accepted trade invite: sessionId={}, targetId={}, phase={}", session.getSessionId(), targetId, session.getPhase());
        return session;
    }

    public Optional<TradeSession> getActiveSession(@Nonnull UUID villagerId) {
        return Optional.ofNullable(this.activeSessionsByParticipant.get(villagerId));
    }

    public void closeSession(@Nonnull UUID sessionId, @Nonnull CloseReason reason) {
        SessionParticipants participants = this.sessionParticipantsBySessionId.remove(sessionId);
        if (participants == null) {
            return;
        }

        TradeSession session = this.activeSessionsByParticipant.remove(participants.firstParticipantId());
        this.activeSessionsByParticipant.remove(participants.secondParticipantId());
        this.invitesByTarget.remove(participants.secondParticipantId());
        this.invitesByTarget.remove(participants.firstParticipantId());

        if (session == null) {
            return;
        }

        TradeSessionPhase terminalPhase = switch (reason) {
            case DEAL -> TradeSessionPhase.DEAL;
            case WALK_AWAY -> TradeSessionPhase.WALK_AWAY;
            case EXTERNAL_CANCEL -> TradeSessionPhase.EXTERNAL_CANCEL;
            case TIMEOUT -> TradeSessionPhase.CLOSED;
        };
        session.transitionTo(terminalPhase, session.getPhaseEnteredGameTime());
        log.info("Closed trade session: sessionId={}, reason={}, terminalPhase={}", sessionId, reason, terminalPhase);
    }

    public void tickTimeouts(long currentGameTime) {
        this.invitesByTarget.entrySet().removeIf(entry -> entry.getValue().expireGameTime() < currentGameTime);

        // Sessions are double-keyed by participant so callers get O(1) lookups from either villager.
        // Timeout evaluation must deduplicate by sessionId first, or the same session would be closed twice.
        // TODO: this is allocated per tick, consider using cache
        Set<UUID> seenSessionIds = new HashSet<>();
        for (TradeSession session : this.activeSessionsByParticipant.values()) {
            if (!seenSessionIds.add(session.getSessionId())) {
                continue;
            }

            long maxDurationTicks = maxDurationTicks(session.getPhase());
            if (maxDurationTicks < 0) {
                continue;
            }
            if ((currentGameTime - session.getPhaseEnteredGameTime()) > maxDurationTicks) {
                log.info("Trade session timed out: sessionId={}, phase={}, phaseEnteredGameTime={}, currentGameTime={}",
                        session.getSessionId(), session.getPhase(), session.getPhaseEnteredGameTime(), currentGameTime);
                closeSession(session.getSessionId(), CloseReason.TIMEOUT);
            }
        }
    }

    private long maxDurationTicks(@Nonnull TradeSessionPhase phase) {
        return switch (phase) {
            case SYN_SENT -> Ticks.seconds(this.tradingConfig.inviteTimeoutSeconds()).getTicks();
            case ESTABLISHED -> Ticks.seconds(2).getTicks();
            case APPROACH -> Ticks.seconds(10).getTicks() + DEADLINE_SLACK_TICKS;
            case OPENING_OFFER -> (this.tradingConfig.openingOfferDuration().getTicks() * 2L) + DEADLINE_SLACK_TICKS;
            case NEGOTIATING ->
                    (this.tradingConfig.negotiationRoundDuration().getTicks() * 2L * this.tradingConfig.maxNegotiationRounds())
                            + DEADLINE_SLACK_TICKS;
            case DEAL -> this.tradingConfig.dealDuration().getTicks() + DEADLINE_SLACK_TICKS;
            case WALK_AWAY -> this.tradingConfig.walkawayDuration().getTicks() + DEADLINE_SLACK_TICKS;
            case EXTERNAL_CANCEL, CLOSED -> -1L;
        };
    }

    private void putSession(@Nonnull TradeSession session) {
        SessionParticipants participants = new SessionParticipants(session.getInitiatorId(), session.getResponderId());
        this.activeSessionsByParticipant.put(participants.firstParticipantId(), session);
        this.activeSessionsByParticipant.put(participants.secondParticipantId(), session);
        this.sessionParticipantsBySessionId.put(session.getSessionId(), participants);
    }

    private record SessionParticipants(@Nonnull UUID firstParticipantId, @Nonnull UUID secondParticipantId) {
    }

}
