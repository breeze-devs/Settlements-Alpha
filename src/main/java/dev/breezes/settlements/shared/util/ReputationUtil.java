package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.credibility.CredibilityStore;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import lombok.CustomLog;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-scope credibility registry — the real implementation of {@link ReputationQuery}.
 * <p>
 * Holds one {@link CredibilityStore} per observer villager so each villager forms
 * its own independent trust relationships. Callers who only need the read-only
 * multiplier should depend on {@link ReputationQuery} rather than this class directly
 * (DIP: callers import the interface, DI wires the concrete class).
 * <p>
 * Decay is applied on the {@link #tickDecay} cadence rather than per-access to avoid
 * hidden side-effects during query calls.
 */
@ServerScope
@CustomLog
public final class ReputationUtil implements ReputationQuery {

    /**
     * Multiplier applied when the source is completely unknown (no tally at all).
     * Mirrors {@link CredibilityStore#NEUTRAL_SCORE} → 1.0 effective multiplier.
     */
    private static final float UNKNOWN_SOURCE_MULTIPLIER = 1.0f;

    /**
     * Per-observer credibility stores, keyed by the observer's UUID.
     * ConcurrentHashMap guards against concurrent server-tick callbacks arriving on
     * different threads (in practice the server is single-threaded, but this is cheap).
     */
    private final Map<UUID, CredibilityStore> storesByObserver = new ConcurrentHashMap<>();
    private final float credibilityDecayPerTick;

    @Inject
    public ReputationUtil(EventLaneConfig config) {
        this.credibilityDecayPerTick = config.credibilityDecayPerTick();
    }

    @Override
    public float getCredibilityMultiplier(UUID observerVillagerId, UUID sourceId) {
        CredibilityStore store = this.storesByObserver.get(observerVillagerId);
        if (store == null) {
            return UNKNOWN_SOURCE_MULTIPLIER;
        }
        return store.getMultiplier(sourceId);
    }

    /**
     * Records a CONFIRMED resolution for a hearsay tip from {@code sourceId}, as witnessed
     * by {@code observerVillagerId}.
     *
     * @param observerVillagerId UUID of the villager who investigated the tip
     * @param sourceId           UUID of the villager who shared the tip
     * @param verifiedAtTick     game tick at which investigation completed
     * @param tipOriginTick      game tick at which the original fact was observed (staleness)
     */
    public void recordConfirmation(UUID observerVillagerId, UUID sourceId,
                                   long verifiedAtTick, long tipOriginTick) {
        this.storeOf(observerVillagerId).recordConfirmation(sourceId, verifiedAtTick, tipOriginTick);
    }

    /**
     * Records a REFUTED resolution for a hearsay tip from {@code sourceId}.
     */
    public void recordRefutation(UUID observerVillagerId, UUID sourceId,
                                 long verifiedAtTick, long tipOriginTick) {
        this.storeOf(observerVillagerId).recordRefutation(sourceId, verifiedAtTick, tipOriginTick);
    }

    /**
     * Convenience dispatcher that routes to confirm or refute based on the resolution enum.
     * Called by {@code InvestigateBehavior} so the behavior only needs to pass the resolution
     * value without branching itself.
     */
    public void recordResolution(UUID observerVillagerId, UUID sourceId,
                                 KnowledgeResolution resolution,
                                 long verifiedAtTick, long tipOriginTick) {
        switch (resolution) {
            case CONFIRMED -> recordConfirmation(observerVillagerId, sourceId, verifiedAtTick, tipOriginTick);
            case REFUTED -> recordRefutation(observerVillagerId, sourceId, verifiedAtTick, tipOriginTick);
            default -> log.debug("ReputationUtil: no credibility update for resolution={}", resolution);
        }
    }

    /**
     * Applies time-based score decay to all observer stores.
     * Should be called once per server tick from the per-server-tick event handler.
     *
     * @param elapsedTicks ticks since the last decay call (usually 1)
     */
    public void tickDecay(long elapsedTicks) {
        for (CredibilityStore store : this.storesByObserver.values()) {
            store.tickDecay(elapsedTicks);
        }
    }

    /**
     * Removes the credibility store for a villager who has left the world
     * (death, unload with no return) to prevent unbounded memory growth.
     */
    public void removeObserver(UUID observerVillagerId) {
        this.storesByObserver.remove(observerVillagerId);
        log.debug("ReputationUtil: removed credibility store for observer={}", observerVillagerId);
    }

    /**
     * Returns the credibility store for the given observer, creating it if absent.
     * Exposed for the NBT persistence layer so it can snapshot or restore the store without
     * going through the record/refute methods.
     *
     * @param observerVillagerId UUID of the observer whose store to fetch/create
     * @return the live {@link CredibilityStore} for that observer
     */
    public CredibilityStore getOrCreateStore(UUID observerVillagerId) {
        return this.storesByObserver.computeIfAbsent(observerVillagerId,
                id -> new CredibilityStore(this.credibilityDecayPerTick));
    }

    private CredibilityStore storeOf(UUID observerVillagerId) {
        return this.getOrCreateStore(observerVillagerId);
    }

}
