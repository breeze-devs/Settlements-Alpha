package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.UUID;

/**
 * Thin application-layer facade for emitting events onto the {@link WorldEventBus}.
 * <p>
 * Behaviors and presenters should call this rather than touching the bus directly,
 * so emission details (chunk coords, game-tick capture) are centralized.
 * <p>
 * All methods are no-ops if the bus has not been injected.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class WorldEventEmitter {

    private final WorldEventBus bus;

    /**
     * Returns the overworld game time so emitted events share the same clock as
     * {@link WorldEventBusReaperServerEvents}, which evicts by overworld time.
     * Events emitted in the Nether/End would otherwise receive a mismatched TTL.
     * Falls back to the villager's local level clock only when the server reference
     * is unavailable (should never happen on the server tick thread, but avoids NPE
     * if the emitter is called from an unexpected context).
     */
    private static long overworldTime(BaseVillager villager) {
        if (villager.getServer() != null) {
            return villager.getServer().overworld().getGameTime();
        }
        return villager.level().getGameTime();
    }

    public void emitBehaviorStarted(BaseVillager villager, BehaviorKey key) {
        long gameTick = overworldTime(villager);
        this.bus.emit(
                WorldEvent.fromPos(villager.getX(), villager.getY(), villager.getZ())
                        .type(WorldEventType.BEHAVIOR_STARTED)
                        .actorId(villager.getUUID())
                        .metadata(key.id()),
                gameTick);
    }

    public void emitBehaviorCompleted(BaseVillager villager, BehaviorKey key) {
        long gameTick = overworldTime(villager);
        this.bus.emit(
                WorldEvent.fromPos(villager.getX(), villager.getY(), villager.getZ())
                        .type(WorldEventType.BEHAVIOR_COMPLETED)
                        .actorId(villager.getUUID())
                        .metadata(key.id()),
                gameTick);
    }

    public void emitBehaviorFailed(BaseVillager villager, BehaviorKey key, @Nullable String reason) {
        long gameTick = overworldTime(villager);
        this.bus.emit(
                WorldEvent.fromPos(villager.getX(), villager.getY(), villager.getZ())
                        .type(WorldEventType.BEHAVIOR_FAILED)
                        .actorId(villager.getUUID())
                        .metadata(key.id())
                        .outcome(EventOutcome.FAILURE)
                        .reason(reason),
                gameTick);
    }

    public void emitTerminalBehaviorEvent(@Nonnull BaseVillager actor,
                                          @Nonnull BehaviorKey key,
                                          @Nonnull WorldEventType type,
                                          @Nullable UUID targetId,
                                          @Nullable UUID registryId,
                                          @Nullable EventOutcome outcome,
                                          @Nullable String detail,
                                          @Nullable String reason) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(type)
                        .actorId(actor.getUUID())
                        .targetId(targetId)
                        .registryId(registryId)
                        .metadata(key.id())
                        .outcome(outcome)
                        .detail(detail)
                        .reason(reason),
                gameTick);
    }

    /**
     * Emits a trade-invite-sent event.
     *
     * @param actor     the initiating villager
     * @param targetId  the receiving villager's UUID
     * @param sessionId the registry id of the backing TradeSession
     */
    public void emitTradeInviteSent(BaseVillager actor, UUID targetId, UUID sessionId) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.TRADE_INVITE_SENT)
                        .actorId(actor.getUUID())
                        .targetId(targetId)
                        .registryId(sessionId),
                gameTick);
    }

    /**
     * Emits a courtship-invite-sent event.
     *
     * @param actor     the initiating villager
     * @param targetId  the receiving villager's UUID
     * @param sessionId the registry id of the backing CourtshipSession
     */
    public void emitCourtshipInviteSent(BaseVillager actor, UUID targetId, UUID sessionId) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.COURTSHIP_INVITE_SENT)
                        .actorId(actor.getUUID())
                        .targetId(targetId)
                        .registryId(sessionId),
                gameTick);
    }

    public void emitDayPlanInvalidated(BaseVillager villager) {
        long gameTick = overworldTime(villager);
        this.bus.emit(
                WorldEvent.fromPos(villager.getX(), villager.getY(), villager.getZ())
                        .type(WorldEventType.DAY_PLAN_INVALIDATED)
                        .actorId(villager.getUUID()),
                gameTick);
    }

    public void emitPlanExhausted(BaseVillager villager) {
        long gameTick = overworldTime(villager);
        this.bus.emit(
                WorldEvent.fromPos(villager.getX(), villager.getY(), villager.getZ())
                        .type(WorldEventType.PLAN_EXHAUSTED)
                        .actorId(villager.getUUID()),
                gameTick);
    }

}
