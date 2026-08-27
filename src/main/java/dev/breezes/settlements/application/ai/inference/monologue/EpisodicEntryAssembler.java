package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.PackedPos;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Converts a villager's knowledge store into a structured episodic list for MONOLOGUE requests.
 * <p>
 * Phrasing is owned by SIS; this assembler ships only raw semantic slots (event type,
 * perspective, resolved names, outcome) so SIS can render first-person or third-person prose
 * as appropriate per entry. No English strings are constructed here.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class EpisodicEntryAssembler {

    private static final String PERSPECTIVE_PARTICIPANT = "FIRST_HAND_PARTICIPANT";
    private static final String PERSPECTIVE_BYSTANDER = "FIRST_HAND_BYSTANDER";

    private static final Comparator<KnowledgeEntry> WEIGHT_DESC =
            Comparator.comparingDouble(KnowledgeEntry::getWeight).reversed();
    private static final Comparator<KnowledgeEntry> ADMITTED_AT_TICK_DESC =
            Comparator.comparingLong(KnowledgeEntry::getAdmittedAtTick).reversed();

    private final VillagerNameDirectory nameDirectory;
    private final InferenceConfig inferenceConfig;

    /**
     * Assembles up to {@link InferenceConfig#maxEpisodicEntries()} structured episodic entries from the store,
     * heaviest first and most recently admitted first among equals.
     * <p>
     * Entries not backed by a seed-worthy {@link WorldEventType} are silently dropped.
     *
     * @param observerId  the UUID of the observing villager; used to derive PARTICIPANT vs BYSTANDER
     * @param store       the villager's knowledge store — plain Java, no Minecraft dependency
     * @param currentTick the current level game time; ageTicks = currentTick − admittedAtTick
     */
    public List<EpisodicEntryDTO> assemble(UUID observerId, VillagerKnowledgeStore store, long currentTick) {
        List<ResolvedEntry> seedWorthy = new ArrayList<>();
        for (KnowledgeEntry entry : store.entriesView()) {
            WorldEventType eventType = resolveEventType(entry);
            if (eventType == null || !eventType.isSeedWorthy()) {
                continue;
            }

            seedWorthy.add(new ResolvedEntry(entry, eventType));
        }

        return seedWorthy.stream()
                .sorted(Comparator.comparing(ResolvedEntry::entry, WEIGHT_DESC.thenComparing(ADMITTED_AT_TICK_DESC)))
                .limit(this.inferenceConfig.maxEpisodicEntries())
                .map(resolved -> toDto(observerId, resolved, currentTick))
                .toList();
    }

    private EpisodicEntryDTO toDto(UUID observerId, ResolvedEntry resolved, long currentTick) {
        KnowledgeEntry entry = resolved.entry();
        WorldEventType eventType = resolved.eventType();
        String perspective = derivePerspective(entry, observerId);
        // Clamp to zero: clock drift or a future-dated admittedAtTick should not produce negative age.
        long ageTicks = Math.max(0L, currentTick - entry.getAdmittedAtTick());

        return EpisodicEntryDTO.builder()
                .eventType(eventType.name())
                .perspective(perspective)
                .actor(resolveActor(entry, perspective))
                .target(resolveTarget(entry, eventType))
                .outcome(entry.getMetadata().get(ObservationMetadataKeys.OUTCOME))
                .reason(entry.getMetadata().get(ObservationMetadataKeys.REASON))
                .detail(collectDetailMap(entry.getMetadata()))
                .pos(resolvePos(entry.getPackedPos()))
                .ageTicks(ageTicks)
                .build();
    }

    /**
     * Collects all {@code "detail.*"} metadata entries into a plain map for the wire DTO.
     * <p>
     * The prefix is stripped so the DTO carries only the slot names (e.g. {@code "item"},
     * {@code "count"}). Returns null when no detail entries are present so Gson omits the
     * field entirely, keeping the wire payload minimal.
     */
    @Nullable
    private static Map<String, String> collectDetailMap(@Nonnull Map<String, String> metadata) {
        Map<String, String> result = null;
        for (Entry<String, String> entry : metadata.entrySet()) {
            if (entry.getKey().startsWith(ObservationMetadataKeys.DETAIL_PREFIX)) {
                if (result == null) {
                    result = new HashMap<>();
                }

                result.put(entry.getKey().substring(ObservationMetadataKeys.DETAIL_PREFIX.length()), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Unpacks the entry's block position, if any.
     * <p>
     * Returns null when the entry carries no spatial grounding (e.g. a private courtship
     * self-failure) so the wire never carries a misleading {@code (0,0,0)} coordinate — SIS
     * treats absent pos as "no spatial grounding". Flooring already happened at the pack site
     * ({@code PerceptionPipeline}), so no rounding occurs here.
     */
    @Nullable
    private static int[] resolvePos(@Nullable Long packedPos) {
        if (packedPos == null) {
            return null;
        }

        return new int[]{PackedPos.x(packedPos), PackedPos.y(packedPos), PackedPos.z(packedPos)};
    }

    /**
     * Derives the perspective of the observing villager relative to this entry.
     * <p>
     * An absent actor_id defaults to FIRST_HAND_PARTICIPANT rather than BYSTANDER: most terminal
     * deeds are the villager's own actions, and actor_id is not always written for first-person
     * events. Misclassifying them as BYSTANDER would cause SIS to render "I saw someone harvest"
     * instead of "I harvested".
     */
    private static String derivePerspective(KnowledgeEntry entry, UUID observerId) {
        UUID actorId = parseUuid(entry.getMetadata().get(ObservationMetadataKeys.ACTOR_ID));
        if (actorId == null || observerId.equals(actorId)) {
            return PERSPECTIVE_PARTICIPANT;
        }

        return PERSPECTIVE_BYSTANDER;
    }

    /**
     * Resolves the actor name to send on the wire.
     * <p>
     * For FIRST_HAND_PARTICIPANT, SIS already knows the villager's name from the persona
     * bundle and renders "I" — sending an actor name here would conflict with first-person prose.
     */
    @Nullable
    private String resolveActor(KnowledgeEntry entry, String perspective) {
        if (PERSPECTIVE_PARTICIPANT.equals(perspective)) {
            return null;
        }

        UUID actorId = parseUuid(entry.getMetadata().get(ObservationMetadataKeys.ACTOR_ID));
        return actorId != null ? this.nameDirectory.resolve(actorId) : null;
    }

    /**
     * Resolves the target name only for social events where relatedEntity is a nameable villager partner.
     * <p>
     * For all other event types the target field is null: relatedEntity may be a block entity,
     * item entity, or other non-person UUID, and passing it to the name resolver would yield a
     * fake human name. Long-term generalization: add a typed target-kind field to WorldEventType
     * so this gate can be data-driven rather than a hardcoded allowlist (deferred).
     */
    @Nullable
    private String resolveTarget(KnowledgeEntry entry, WorldEventType eventType) {
        boolean isNamableTarget = eventType == WorldEventType.TRADE_COMPLETED
                || eventType == WorldEventType.COURTSHIP_CHILD_BIRTH
                || eventType == WorldEventType.COURTSHIP_DATE_COMPLETED
                || eventType == WorldEventType.COURTSHIP_REJECTED
                || eventType == WorldEventType.EMERALDS_DONATED;
        if (!isNamableTarget) {
            return null;
        }

        UUID targetId = entry.getRelatedEntity();
        return targetId != null ? this.nameDirectory.resolve(targetId) : null;
    }

    /**
     * Parses a WorldEventType from entry metadata without throwing on missing or unknown tokens.
     * Returns null so callers can skip gracefully rather than crashing on future event type additions.
     */
    @Nullable
    private static WorldEventType resolveEventType(KnowledgeEntry entry) {
        String rawType = entry.getMetadata().get(ObservationMetadataKeys.EVENT_TYPE);
        if (rawType == null || rawType.isBlank()) {
            return null;
        }

        try {
            return WorldEventType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            log.info("Skipping episodic entry with unrecognized world event type: {}", rawType);
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
            log.error("Failed to parse UUID: {}", raw, e);
            return null;
        }
    }

    /**
     * Pairs a surviving knowledge entry with its already-resolved event type, so the type is parsed
     * once per entry rather than again for each entry that survives the cap.
     */
    private record ResolvedEntry(KnowledgeEntry entry, WorldEventType eventType) {
    }

}
