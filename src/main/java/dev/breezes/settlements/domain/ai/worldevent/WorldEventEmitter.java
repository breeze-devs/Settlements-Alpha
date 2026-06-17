package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

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

    public void emitSheepSheared(BaseVillager actor) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.SHEEP_SHEARED)
                        .actorId(actor.getUUID()),
                gameTick);
    }

    public void emitCropHarvested(BaseVillager actor) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.CROP_HARVESTED)
                        .actorId(actor.getUUID()),
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

    public void emitTradeCompleted(BaseVillager actor, @Nullable UUID partnerId, UUID sessionId) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.TRADE_COMPLETED)
                        .actorId(actor.getUUID())
                        .targetId(partnerId)
                        .registryId(sessionId),
                gameTick);
    }

    public void emitCourtshipCompleted(BaseVillager actor, @Nullable UUID partnerId, UUID sessionId) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.COURTSHIP_COMPLETED)
                        .actorId(actor.getUUID())
                        .targetId(partnerId)
                        .registryId(sessionId),
                gameTick);
    }

    /**
     * Emits a TIP_CONFIRMED event after Investigate finds the hearsay claim to be true.
     * Perception-gate-admitted observers learn that the tip was verified, allowing
     * downstream sensors to update their world model without re-investigating.
     *
     * @param actor the villager that completed the investigation
     */
    public void emitTipConfirmed(BaseVillager actor) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.TIP_CONFIRMED)
                        .actorId(actor.getUUID()),
                gameTick);
    }

    /**
     * Emits a TIP_REFUTED event after Investigate finds the hearsay claim to be false.
     * Useful for social topics ("no melons out west") and diagnostic logging.
     *
     * @param actor the villager that completed the investigation
     */
    public void emitTipRefuted(BaseVillager actor) {
        long gameTick = overworldTime(actor);
        this.bus.emit(
                WorldEvent.fromPos(actor.getX(), actor.getY(), actor.getZ())
                        .type(WorldEventType.TIP_REFUTED)
                        .actorId(actor.getUUID()),
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
