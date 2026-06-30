package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
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
     * dedupeKey → game tick after which a co-witness may re-announce the same sighting
     */
    private final Map<UUID, Long> recentSightingEmissions = new HashMap<>();

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
                                          @Nullable String reason,
                                          @Nullable Map<String, String> detailFields) {
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
                        .reason(reason)
                        .detailFields(detailFields),
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

    /**
     * Emits a sighting event recording that a villager directly observed {@code subject}.
     * <p>
     * A sighting has no single doer, so no actor is recorded: every villager that perceives the
     * event (subject to the perception gate) treats it as its own first-hand observation, rather
     * than inheriting whichever witness happened to emit first. Position is anchored to the subject
     * so spatial grounding and coarse-cell deduplication both use the subject's actual location.
     * <p>
     * Co-witnesses that compute the same {@code dedupeKey} collapse to a single bus emission for the
     * retention window, so fan-out stays bounded by distinct sightings rather than by crowd size.
     *
     * @param witness      the villager whose sensor fired (used only as the emission clock source)
     * @param subject      the entity that was observed
     * @param entityTypeId namespaced entity type string (e.g. "minecraft:zombie"), computed once by the caller
     * @param type         the sighting event type (ZOMBIE_SIGHTED, PLAYER_SIGHTED, etc.)
     * @param dedupeKey    content-addressed UUID from {@link dev.breezes.settlements.domain.ai.perception.SightingDedupeKeyFactory}
     *                     so independent co-witnesses converge on the same episodic fact
     */
    public void emitSighting(@Nonnull BaseVillager witness,
                             @Nonnull Entity subject,
                             @Nonnull String entityTypeId,
                             @Nonnull WorldEventType type,
                             @Nonnull UUID dedupeKey) {
        long gameTick = overworldTime(witness);

        // Cross-witness fan-out suppression: while this sighting is still live on the bus, every
        // nearby villager visits it once through its cursor, so a co-witness re-announcing the same
        // content-addressed fact is pure waste
        purgeExpiredSightingEmissions(gameTick);
        if (isSightingSuppressed(dedupeKey, gameTick)) {
            return;
        }

        // actorId is deliberately left null -- a sighting is witnessed, not done
        WorldEvent.WorldEventBuilder builder = WorldEvent.fromPos(subject.getX(), subject.getY(), subject.getZ())
                .type(type)
                .actorId(null)
                .targetId(subject.getUUID())
                .metadata(entityTypeId)
                .dedupeKey(dedupeKey);

        // Snapshot the player's display name at emit time
        if (subject instanceof Player player) {
            builder.detailFields(Map.of("player", player.getName().getString()));
        }

        this.bus.emit(builder, gameTick);
        this.recentSightingEmissions.put(dedupeKey, gameTick + this.bus.ttlTicks());
    }

    private boolean isSightingSuppressed(UUID dedupeKey, long currentTick) {
        Long expiry = this.recentSightingEmissions.get(dedupeKey);
        return expiry != null && currentTick < expiry;
    }

    private void purgeExpiredSightingEmissions(long currentTick) {
        this.recentSightingEmissions.values().removeIf(expiry -> expiry <= currentTick);
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
