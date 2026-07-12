package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Getter;

/**
 * Typed, centrally registered event types for the {@link WorldEventBus}.
 * <p>
 * Each constant carries its own classification metadata ({@link ObservationType},
 * base importance, and per-behavior flags) to prevent perception, inference, and memory
 * compaction from drifting as new constants are added. Co-locating this data here is the
 * single source of truth; downstream classes read fields instead of maintaining parallel
 * switch statements.
 * <p>
 * The two boolean flags are intentionally decoupled so sighting events (ZOMBIE_SIGHTED etc.)
 * can be forceRemember=true without requiring seedWorthy=true — and conversely, future event
 * types can be seedWorthy without bypassing the importance gate.
 * <p>
 * {@code selfWitnessed} marks events that have no single doer: every villager that perceives one
 * is an equal first-hand witness (the sightings). It defaults to false through the delegating
 * constructor, so only the sighting constants opt in.
 */
@Getter
public enum WorldEventType {

    /**
     * A villager started executing a plan behavior. Carries the behavior key as metadata.
     */
    BEHAVIOR_STARTED(WorldEventNamespace.WORLD, ObservationType.TASK_COMPLETION, 0.8F, false, false),

    /**
     * A villager finished a plan behavior normally. Carries the behavior key as metadata.
     */
    BEHAVIOR_COMPLETED(WorldEventNamespace.WORLD, ObservationType.TASK_COMPLETION, 0.8F, false, false),

    /**
     * A villager's behavior terminated without accomplishing its intended deed. Carries the
     * behavior key as metadata and an optional reason.
     */
    BEHAVIOR_FAILED(WorldEventNamespace.WORLD, ObservationType.TASK_FAILURE, 0.8F, false, false),

    SHEEP_SHEARED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    SHEEP_DYED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    RESOURCE_HARVESTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    FARMLAND_CULTIVATED(WorldEventNamespace.WORLD, ObservationType.ENVIRONMENT, 1.8F, true, true),

    /**
     * A trade negotiation was completed (deal or walk-away).
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code TradeSessionRegistry}, not the bus.
     */
    TRADE_COMPLETED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A courtship event was completed.
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code CourtshipSessionRegistry}, not the bus.
     */
    COURTSHIP_COMPLETED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A courtship advance was turned down. The actor is the receiver who declined;
     * the target is the spurned presenter. Only the receiver knows why it was rejected, so this is
     * emitted from the accept-side behavior. Carries the session registry id.
     */
    COURTSHIP_REJECTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A villager gifted emeralds to a destitute neighbor via the need-based donation floor.
     * The target is the recipient.
     */
    EMERALDS_DONATED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * This villager sent a trade invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    TRADE_INVITE_SENT(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, false, false),

    /**
     * This villager sent a courtship invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    COURTSHIP_INVITE_SENT(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, false, false),

    /**
     * The villager's current day plan was invalidated and will be regenerated.
     */
    DAY_PLAN_INVALIDATED(WorldEventNamespace.SYSTEM, ObservationType.ENVIRONMENT, 0.1F, false, false),

    /**
     * The villager's day plan was fully exhausted.
     */
    PLAN_EXHAUSTED(WorldEventNamespace.SYSTEM, ObservationType.ENVIRONMENT, 0.1F, false, false),

    COW_MILKED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    FISH_CAUGHT(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    STONE_CUT(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    RESOURCE_EXCAVATED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    MEAT_SMOKED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    ORE_SMELTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    GOODS_CRAFTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    TOOL_FORGED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    FURNACE_MISFIRED(WorldEventNamespace.WORLD, ObservationType.INCIDENT, 2.0F, true, true),
    LIVESTOCK_BUTCHERED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    ITEM_ENCHANTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    LEATHER_DYED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true, true),
    LEATHER_WASHED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.5F, true, true),
    ANIMAL_BRED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.6F, true, true),
    ANIMAL_TAMED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true, true),
    WOLF_WASHED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.2F, true, true),
    WOLF_FED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.2F, true, true),
    DOG_WALKED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.2F, true, true),
    ANIMAL_PETTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.0F, false, true),
    GOLEM_REPAIRED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true, true),
    POTION_THROWN(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true, true),
    BELL_RUNG(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.5F, true, true),
    TARGET_EGGED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, true, true),
    CHICKENS_CHASED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.5F, true, true),
    CHICKENS_REVENGED(WorldEventNamespace.WORLD, ObservationType.INCIDENT, 1.8F, true, true),
    LANDSCAPE_SURVEYED(WorldEventNamespace.WORLD, ObservationType.ENVIRONMENT, 1.2F, true, true),
    CHEST_MANAGED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.6F, true, true),
    ITEM_COLLECTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.0F, false, false),
    ZOMBIE_SIGHTED(WorldEventNamespace.WORLD, ObservationType.THREAT, 1.6F, true, true, true),
    PLAYER_SIGHTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 0.9F, false, true, true),
    WANDERING_TRADER_SIGHTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 0.9F, false, true, true),
    GOLEM_SIGHTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 0.8F, false, true, true),
    ;

    private final WorldEventNamespace namespace;
    private final ObservationType observationType;
    private final float baseImportance;

    /**
     * When true, the witnessing villager bypasses the importance gate and always stores this
     * event as a first-hand episodic fact. Appropriate for threats and landmark personal deeds
     * where the salience is absolute rather than gene- or novelty-dependent.
     */
    private final boolean forceRemember;

    /**
     * When true, this event type may appear as a seed in the villager's evening monologue.
     */
    private final boolean seedWorthy;

    /**
     * When true, this event has no single doer: every villager that perceives it (subject to the
     * perception gate) is an equal first-hand witness, so {@code actorId} is left null and each
     * perceiver records it as its own first-person observation. The sightings set this; deeds do
     * not, since a deed's force-remember applies only to its doer.
     */
    private final boolean selfWitnessed;

    /**
     * Delegating constructor for doer-model event types (the overwhelming majority), which are never
     * self-witnessed. Sighting constants use the six-arg form to opt into {@code selfWitnessed}.
     */
    WorldEventType(WorldEventNamespace namespace, ObservationType observationType, float baseImportance,
                   boolean forceRemember, boolean seedWorthy) {
        this(namespace, observationType, baseImportance, forceRemember, seedWorthy, false);
    }

    WorldEventType(WorldEventNamespace namespace, ObservationType observationType, float baseImportance,
                   boolean forceRemember, boolean seedWorthy, boolean selfWitnessed) {
        this.namespace = namespace;
        this.observationType = observationType;
        this.baseImportance = baseImportance;
        this.forceRemember = forceRemember;
        this.seedWorthy = seedWorthy;
        this.selfWitnessed = selfWitnessed;
    }

    /**
     * Salient events a villager always self-remembers, bypassing the importance gate.
     * Sighting types can choose force-remember independently of monologue seed
     * inclusion via {@link #seedWorthy}.
     */
    public boolean isSelfRememberableTerminalEvent() {
        return this.forceRemember;
    }

}
