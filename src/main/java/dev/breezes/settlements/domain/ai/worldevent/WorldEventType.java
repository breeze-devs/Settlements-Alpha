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
 * can be forceRemember=true without requiring seedWorthy=true — and conversely, an event type
 * can be seedWorthy without also demanding unconditional admission.
 * <p>
 * {@code selfWitnessed} marks events that have no single doer: every villager that perceives one
 * is an equal first-hand witness (the sightings). It defaults to false through the delegating
 * constructor, so only the sighting constants opt in.
 * <p>
 * TODO: the tuning fields here are provisional — identity, perception tuning, and wire vocabulary
 *  each want their own home, so this enum is expected to split once emission is redesigned.
 */
@Getter
public enum WorldEventType {

    SHEEP_SHEARED(ObservationType.RESOURCE, 1.8F, true, true),
    SHEEP_DYED(ObservationType.RESOURCE, 1.8F, true, true),
    RESOURCE_HARVESTED(ObservationType.RESOURCE, 1.8F, true, true),
    FARMLAND_CULTIVATED(ObservationType.ENVIRONMENT, 1.8F, true, true),

    /**
     * A trade negotiation was completed (deal or walk-away).
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code TradeSessionRegistry}, not the bus.
     */
    TRADE_COMPLETED(ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A courtship dance completed and the conception roll succeeded: a child was born.
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code CourtshipSessionRegistry}, not the bus.
     */
    COURTSHIP_CHILD_BIRTH(ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A courtship dance completed but the conception roll failed: the pair still had a
     * successful date and both go on breed cooldown, but no child was born. Distinct from
     * {@link #COURTSHIP_CHILD_BIRTH} because "X dated Y" is semantically different from
     * "X had a baby with Y". Carries the session registry id in the same manner.
     * <p>
     * Ranked below the 2.5F salient-social tier and left out of forceRemember on purpose: this is
     * the majority courtship outcome, so demanding unconditional admission would let routine dates
     * crowd the bounded knowledge store and displace the events worth remembering. It stays
     * seedWorthy — a date is better monologue material than a harvest — but it has to earn its
     * place like any other ordinary deed.
     */
    COURTSHIP_DATE_COMPLETED(ObservationType.SOCIAL, 2.0F, false, true),

    /**
     * A courtship advance was turned down. The actor is the receiver who declined;
     * the target is the spurned presenter. Only the receiver knows why it was rejected, so this is
     * declared by the accept-side behavior. Carries the session registry id.
     */
    COURTSHIP_REJECTED(ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * A villager gifted emeralds to a destitute neighbor via the need-based donation floor.
     * The target is the recipient.
     */
    EMERALDS_DONATED(ObservationType.SOCIAL, 2.5F, true, true),

    /**
     * This villager sent a trade invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    TRADE_INVITE_SENT(ObservationType.SOCIAL, 2.0F, false, false),

    /**
     * This villager sent a courtship invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    COURTSHIP_INVITE_SENT(ObservationType.SOCIAL, 2.0F, false, false),

    COW_MILKED(ObservationType.RESOURCE, 1.8F, true, true),
    FISH_CAUGHT(ObservationType.RESOURCE, 1.8F, true, true),
    STONE_CUT(ObservationType.RESOURCE, 1.8F, true, true),
    RESOURCE_EXCAVATED(ObservationType.RESOURCE, 1.8F, true, true),
    MEAT_SMOKED(ObservationType.RESOURCE, 1.8F, true, true),
    ORE_SMELTED(ObservationType.RESOURCE, 1.8F, true, true),
    GOODS_CRAFTED(ObservationType.RESOURCE, 1.8F, true, true),
    TOOL_FORGED(ObservationType.RESOURCE, 1.8F, true, true),
    FURNACE_MISFIRED(ObservationType.INCIDENT, 2.0F, true, true),
    LIVESTOCK_BUTCHERED(ObservationType.RESOURCE, 1.8F, true, true),
    ITEM_ENCHANTED(ObservationType.RESOURCE, 1.8F, true, true),
    LEATHER_DYED(ObservationType.RESOURCE, 1.8F, true, true),
    LEATHER_WASHED(ObservationType.RESOURCE, 1.5F, true, true),
    ANIMAL_BRED(ObservationType.RESOURCE, 1.6F, true, true),
    ANIMAL_TAMED(ObservationType.SOCIAL, 1.8F, true, true),
    WOLF_WASHED(ObservationType.RESOURCE, 1.2F, true, true),
    WOLF_FED(ObservationType.RESOURCE, 1.2F, true, true),
    DOG_WALKED(ObservationType.SOCIAL, 1.2F, true, true),
    ANIMAL_PETTED(ObservationType.SOCIAL, 1.0F, false, true),
    GOLEM_REPAIRED(ObservationType.SOCIAL, 1.8F, true, true),
    POTION_THROWN(ObservationType.SOCIAL, 1.8F, true, true),
    BELL_RUNG(ObservationType.SOCIAL, 1.5F, true, true),
    TARGET_EGGED(ObservationType.SOCIAL, 2.0F, true, true),
    CHICKENS_CHASED(ObservationType.SOCIAL, 1.5F, true, true),
    CHICKENS_REVENGED(ObservationType.INCIDENT, 1.8F, true, true),
    LANDSCAPE_SURVEYED(ObservationType.ENVIRONMENT, 1.2F, true, true),
    CHEST_MANAGED(ObservationType.RESOURCE, 1.6F, true, true),
    ITEM_COLLECTED(ObservationType.RESOURCE, 1.0F, false, false),
    ZOMBIE_SIGHTED(ObservationType.THREAT, 1.6F, true, true, true),
    PLAYER_SIGHTED(ObservationType.SOCIAL, 0.9F, false, true, true),
    WANDERING_TRADER_SIGHTED(ObservationType.SOCIAL, 0.9F, false, true, true),
    GOLEM_SIGHTED(ObservationType.SOCIAL, 0.8F, false, true, true),
    ;

    private final ObservationType observationType;
    private final float baseImportance;

    /**
     * When true, a witnessing villager must store this event as a first-hand episodic fact no
     * matter how selective memory admission is. Appropriate for threats and landmark personal
     * deeds where the salience is absolute rather than gene- or novelty-dependent.
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
     * self-witnessed. Sighting constants use the five-arg form to opt into {@code selfWitnessed}.
     */
    WorldEventType(ObservationType observationType, float baseImportance,
                   boolean forceRemember, boolean seedWorthy) {
        this(observationType, baseImportance, forceRemember, seedWorthy, false);
    }

    WorldEventType(ObservationType observationType, float baseImportance,
                   boolean forceRemember, boolean seedWorthy, boolean selfWitnessed) {
        this.observationType = observationType;
        this.baseImportance = baseImportance;
        this.forceRemember = forceRemember;
        this.seedWorthy = seedWorthy;
        this.selfWitnessed = selfWitnessed;
    }

    /**
     * Salient events a villager always self-remembers, however selective memory admission is.
     * Sighting types can choose force-remember independently of monologue seed
     * inclusion via {@link #seedWorthy}.
     */
    public boolean isSelfRememberableTerminalEvent() {
        return this.forceRemember;
    }

}
