package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Typed, centrally registered event types for the {@link WorldEventBus}.
 * <p>
 * Each constant carries its own classification metadata ({@link ObservationType},
 * base importance, and terminal-deed flag) to prevent perception, inference, and memory
 * compaction from drifting as new constants are added. Co-locating this data here is the
 * single source of truth; downstream classes read fields instead of maintaining parallel
 * switch statements.
 */
@Getter
@RequiredArgsConstructor
public enum WorldEventType {

    /**
     * A villager started executing a plan behavior. Carries the behavior key as metadata.
     */
    BEHAVIOR_STARTED(WorldEventNamespace.WORLD, ObservationType.TASK_COMPLETION, 0.8F, false),

    /**
     * A villager finished a plan behavior normally. Carries the behavior key as metadata.
     */
    BEHAVIOR_COMPLETED(WorldEventNamespace.WORLD, ObservationType.TASK_COMPLETION, 0.8F, false),

    /**
     * A villager's behavior terminated without accomplishing its intended deed. Carries the
     * behavior key as metadata and an optional reason.
     */
    BEHAVIOR_FAILED(WorldEventNamespace.WORLD, ObservationType.TASK_FAILURE, 0.8F, false),

    SHEEP_SHEARED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    SHEEP_DYED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    CROP_HARVESTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),

    /**
     * A trade negotiation was completed (deal or walk-away).
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code TradeSessionRegistry}, not the bus.
     */
    TRADE_COMPLETED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true),

    /**
     * A courtship event was completed.
     * Carries the session registry id in the {@link WorldEvent#getRegistryId()} field.
     * First-accept-wins resolution goes through {@code CourtshipSessionRegistry}, not the bus.
     */
    COURTSHIP_COMPLETED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true),

    /**
     * A courtship advance was turned down. The actor is the receiver who declined;
     * the target is the spurned presenter. Only the receiver knows why it was rejected, so this is
     * emitted from the accept-side behavior. Carries the session registry id.
     */
    COURTSHIP_REJECTED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.5F, true),

    /**
     * This villager sent a trade invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    TRADE_INVITE_SENT(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, false),

    /**
     * This villager sent a courtship invite to a target.
     * Carries the session registry id so the receiver can correlate with the registry.
     */
    COURTSHIP_INVITE_SENT(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, false),

    /**
     * An Investigate behavior confirmed the hearsay claim: the claimed condition was found
     * to be true at the tip location. The actor is the investigating villager.
     */
    TIP_CONFIRMED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 2.0F, true),

    /**
     * An Investigate behavior refuted the hearsay claim: the claimed condition was not found
     * at the tip location. The actor is the investigating villager.
     */
    TIP_REFUTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.5F, true),

    /**
     * The villager's current day plan was invalidated and will be regenerated.
     */
    DAY_PLAN_INVALIDATED(WorldEventNamespace.SYSTEM, ObservationType.ENVIRONMENT, 0.1F, false),

    /**
     * The villager's day plan was fully exhausted.
     */
    PLAN_EXHAUSTED(WorldEventNamespace.SYSTEM, ObservationType.ENVIRONMENT, 0.1F, false),

    // -------------------------------------------------------------------------
    // New behavior deed types (23)
    // Importance and terminal-deed values from the behavior-signal-emission spec.
    // terminalDeed=false for logistics shuffles (take/store/collect) — low-signal
    // chest movement should not flood self-memory or gossip.
    // -------------------------------------------------------------------------

    COW_MILKED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    FISH_CAUGHT(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    STONE_CUT(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    MEAT_SMOKED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    ORE_SMELTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    LIVESTOCK_BUTCHERED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    ITEM_ENCHANTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    LEATHER_DYED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.8F, true),
    LEATHER_WASHED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.5F, true),
    ANIMAL_BRED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.6F, true),
    ANIMAL_TAMED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true),
    WOLF_WASHED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.2F, true),
    WOLF_FED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.2F, true),
    DOG_WALKED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.2F, true),
    GOLEM_REPAIRED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true),
    POTION_THROWN(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.8F, true),
    BELL_RUNG(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.5F, true),
    TARGET_EGGED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 2.0F, true),
    CHICKENS_CHASED(WorldEventNamespace.WORLD, ObservationType.SOCIAL, 1.5F, true),
    LANDSCAPE_SURVEYED(WorldEventNamespace.WORLD, ObservationType.ENVIRONMENT, 1.2F, true),
    ITEMS_TAKEN(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.0F, false),
    ITEMS_STORED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.0F, false),
    ITEM_COLLECTED(WorldEventNamespace.WORLD, ObservationType.RESOURCE, 1.0F, false),
    ;

    private final WorldEventNamespace namespace;
    private final ObservationType observationType;
    private final float baseImportance;
    /**
     * True for salient terminal deeds a villager always self-remembers and that are
     * worthy of being projected as monologue seeds. False for lifecycle noise and
     * low-signal logistics that should pass through the importance gate rather than
     * unconditionally promoting.
     * <p>
     * Unifying the former {@code isSelfRememberableTerminalEvent} switch and the
     * projector's {@code SEED_WORTHY_TYPES} set into one flag prevents the two from
     * drifting out of sync as new constants are added.
     */
    private final boolean terminalDeed;

    /**
     * Salient terminal deeds a villager always remembers about its own actions.
     * Generic lifecycle completion/failure is excluded because it would flood the
     * bounded knowledge store with low-signal "finished a task" entries.
     */
    public boolean isSelfRememberableTerminalEvent() {
        return this.terminalDeed;
    }

    /**
     * Whether this event type is worthy of inclusion as a monologue seed.
     * Equivalent to {@link #isSelfRememberableTerminalEvent()} — both are driven by
     * the same terminal-deed flag so the two allowlists cannot drift.
     */
    public boolean isSeedWorthy() {
        return this.terminalDeed;
    }

}
