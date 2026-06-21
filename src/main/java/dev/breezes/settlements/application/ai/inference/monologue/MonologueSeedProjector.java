package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.naming.VillagerNameResolver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Converts knowledge entries into flat third-person seed strings for MONOLOGUE requests
 * <p>
 * Every seed is a third-person clause with real names in fixed semantic slots:
 * <pre>  {actor} &lt;verb clause&gt; {target}</pre>
 * The model that receives these seeds already knows the villager's own name from the persona
 * bundle, so it can write first-person prose without the projector encoding perspective.
 * <p>
 * <b>Allowlist.</b>  Terminal behavior outcomes become seeds; mid-run attempts and start noise
 * are filtered before rendering. See {@link #SEED_WORTHY_TYPES}.
 * <p>
 * <b>Hearsay.</b>  A hearsay entry is prefixed with the source's resolved name, e.g.
 * {@code "Aldric says <actor> courted <target>"}.
 * <p>
 * <b>Pipeline.</b>  Seeds are deduped on the final rendered string (highest weight wins),
 * sorted by weight descending, and capped at {@link #MAX_SEEDS}.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MonologueSeedProjector {

    /**
     * Hard cap on seeds sent per villager.
     * Dedup runs before the cap so the cap yields up to 50 distinct seeds.
     */
    public static final int MAX_SEEDS = 50;

    /**
     * The only {@link WorldEventType} values that become seeds.
     * Invites and generic lifecycle start/complete/fail events are excluded as mid-run or
     * low-signal noise; only salient terminal outcomes (deeds, social acts, tip resolutions) are
     * worth muttering about.
     */
    static final Set<WorldEventType> SEED_WORTHY_TYPES = Set.of(
            WorldEventType.TRADE_COMPLETED,
            WorldEventType.COURTSHIP_COMPLETED,
            WorldEventType.COURTSHIP_REJECTED,
            WorldEventType.CROP_HARVESTED,
            WorldEventType.SHEEP_SHEARED,
            WorldEventType.SHEEP_DYED,
            WorldEventType.TIP_CONFIRMED,
            WorldEventType.TIP_REFUTED
    );

    // Keys are defined in ObservationMetadataKeys (domain layer) so both the writer
    // (ObservationFactory) and reader (this class) share the same constants without
    // a circular dependency.
    private static final String METADATA_KEY_EVENT_TYPE = ObservationMetadataKeys.EVENT_TYPE;
    private static final String METADATA_KEY_ACTOR_ID = ObservationMetadataKeys.ACTOR_ID;

    private final VillagerNameResolver nameResolver;

    /**
     * Projects the villager's knowledge store into a list of seed strings.
     * Entries whose event type is not in the completions allowlist are silently skipped.
     *
     * @param observerId UUID of the villager whose store is being projected (unused for
     *                   rendering now that seeds are flat third-person, but retained so
     *                   the signature is stable and callers need not change)
     * @param store      the villager's knowledge store
     */
    public List<String> project(UUID observerId, VillagerKnowledgeStore store) {
        // Dedup map: final seed string → highest weight seen so far.
        // Deduplication before sorting ensures the limit always operates on distinct strings.
        Map<String, Float> bestWeightBySeed = new HashMap<>();

        for (KnowledgeEntry entry : store.entriesView()) {
            // Skip entries that are lifecycle noise or explicit invites.
            WorldEventType eventType = resolveEventType(entry);
            if (eventType != null && !SEED_WORTHY_TYPES.contains(eventType)) {
                continue;
            }
            // When eventType is null we fall through to the ObservationType fallback in
            // renderSeed, so unknown future types still produce a coarse seed rather than
            // disappearing silently.

            String seed = renderSeed(entry);
            float existing = bestWeightBySeed.getOrDefault(seed, Float.NEGATIVE_INFINITY);
            if (entry.getWeight() > existing) {
                bestWeightBySeed.put(seed, entry.getWeight());
            }
        }

        // Sort by weight descending, then cap. The external service owns final prose quality;
        // sending the most salient seeds first is the correct signal.
        return bestWeightBySeed.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(MAX_SEEDS)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Renders a single entry into a flat third-person clause.
     * Hearsay entries are prefixed with "{sourceName} says ".
     * All name slots are resolved to human-readable names via {@link VillagerNameResolver}.
     */
    private String renderSeed(KnowledgeEntry entry) {
        UUID actorId = parseUuid(entry.getMetadata().get(METADATA_KEY_ACTOR_ID));
        UUID targetId = entry.getRelatedEntity();
        String detail = entry.getMetadata().get(SeedPhrasebook.METADATA_KEY_DETAIL);
        String eventMetadata = entry.getMetadata().get(ObservationMetadataKeys.EVENT_META);
        EventOutcome outcome = resolveOutcome(entry);
        String reason = entry.getMetadata().get(SeedPhrasebook.METADATA_KEY_REASON);

        String actorName = this.nameResolver.resolve(actorId);
        String targetName = targetId != null ? this.nameResolver.resolve(targetId) : null;

        WorldEventType eventType = resolveEventType(entry);
        String clause;
        if (eventType != null) {
            clause = SeedPhrasebook.phraseClause(eventType, actorName, targetName, detail, outcome, reason, eventMetadata);
        } else {
            // Unknown or absent event type: fall back to coarser ObservationType phrasing.
            clause = SeedPhrasebook.phraseFallback(actorName, entry.getType());
        }

        if (entry.isHearsay()) {
            String sourceName = this.nameResolver.resolve(entry.getSource());
            return sourceName + " says " + clause;
        }

        return clause;
    }

    /**
     * Resolves the {@link WorldEventType} from entry metadata; returns null when absent
     * or unrecognized. Null drives callers to the {@link dev.breezes.settlements.domain.ai.observation.ObservationType}
     * fallback rather than throwing.
     */
    @Nullable
    private static WorldEventType resolveEventType(KnowledgeEntry entry) {
        String rawType = entry.getMetadata().get(METADATA_KEY_EVENT_TYPE);
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return WorldEventType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            // Unknown future event type — let the caller fall back gracefully rather than crashing.
            return null;
        }
    }

    /**
     * Resolves the {@link EventOutcome} from entry metadata; returns null when absent
     * or unrecognized. Null is treated as SUCCESS by the phrasebook.
     */
    @Nullable
    private static EventOutcome resolveOutcome(KnowledgeEntry entry) {
        String rawOutcome = entry.getMetadata().get(SeedPhrasebook.METADATA_KEY_OUTCOME);
        if (rawOutcome == null || rawOutcome.isBlank()) {
            return null;
        }
        try {
            return EventOutcome.valueOf(rawOutcome);
        } catch (IllegalArgumentException e) {
            // Unknown outcome value — treat as absent (i.e. success) rather than crashing.
            return null;
        }
    }

    @Nullable
    private static UUID parseUuid(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
