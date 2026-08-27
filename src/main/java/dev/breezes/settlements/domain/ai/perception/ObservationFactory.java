package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationContentRenderer;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Converts a gate-admitted {@link WorldEvent} into an {@link Observation}
 * ready for the {@link dev.breezes.settlements.domain.ai.observation.ObservationBuffer}
 * <p>
 * Metadata materialization is a separate call ({@link #metadataFor}) rather than part of the
 * observation, so an observation discarded before promotion never allocates the map.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ObservationFactory {

    /**
     * Builds an {@link Observation} from an admitted world event.
     *
     * @param event       the event that passed the perception gate
     * @param currentTick the game tick at the moment of observation (usually the villager's step tick)
     */
    public static Observation fromEvent(@Nonnull WorldEvent event, long currentTick) {
        ObservationType type = event.getType().getObservationType();
        float baseImportance = event.getType().getBaseImportance();
        String content = buildContent(event);
        UUID observationId = observationIdFor(event);

        return Observation.builder()
                .id(observationId)
                .timestampTick(currentTick)
                .type(type)
                .eventType(event.getType())
                .content(content)
                .baseImportance(baseImportance)
                .relatedEntity(event.getTargetId())
                .actorId(event.getActorId())
                .registryId(event.getRegistryId())
                .eventMetadata(event.getMetadata())
                .outcome(event.getOutcome())
                .reason(event.getReason())
                .detail(event.getDetail())
                .detailFields(event.getDetailFields())
                .posX(event.getPosX())
                .posY(event.getPosY())
                .posZ(event.getPosZ())
                .build();
    }

    /**
     * Builds the persistence metadata for an observation that is being promoted to knowledge.
     */
    public static Map<String, String> metadataFor(@Nonnull Observation observation) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ObservationMetadataKeys.EVENT_TYPE, observation.eventType().name());

        if (observation.eventMetadata() != null) {
            metadata.put(ObservationMetadataKeys.EVENT_META, observation.eventMetadata());
        }
        if (observation.actorId() != null) {
            metadata.put(ObservationMetadataKeys.ACTOR_ID, observation.actorId().toString());
        }
        if (observation.registryId() != null) {
            metadata.put(ObservationMetadataKeys.REGISTRY_ID, observation.registryId().toString());
        }
        // Absent outcome is already treated as SUCCESS downstream, so only persist the
        // non-default case; the common case (SUCCESS) is not written at all.
        if (observation.outcome() != null && observation.outcome() != EventOutcome.SUCCESS) {
            metadata.put(ObservationMetadataKeys.OUTCOME, observation.outcome().name());
        }
        if (observation.reason() != null) {
            metadata.put(ObservationMetadataKeys.REASON, observation.reason());
        }
        if (observation.detail() != null) {
            metadata.put(ObservationMetadataKeys.DETAIL, observation.detail());
        }

        // Serialize structured detail as "detail.<slot>" entries
        Map<String, String> detailFields = observation.detailFields();
        if (detailFields != null) {
            for (Entry<String, String> entry : detailFields.entrySet()) {
                metadata.put(ObservationMetadataKeys.DETAIL_PREFIX + entry.getKey(), entry.getValue());
            }
        }

        return Map.copyOf(metadata);
    }

    private static UUID observationIdFor(@Nonnull WorldEvent event) {
        // A content-addressed dedupeKey is used verbatim so independent witnesses of one subject
        // derive a single shared id rather than one id each. Everything downstream is keyed on that
        // id, so the second arrival of the same occurrence is recognized as the same fact.
        if (event.getDedupeKey() != null) {
            return event.getDedupeKey();
        }

        UUID actorId = event.getActorId();
        long actorMost = actorId != null ? actorId.getMostSignificantBits() : 0L;
        long actorLeast = actorId != null ? actorId.getLeastSignificantBits() : 0L;
        long sequence = event.getSequence();
        long gameTick = event.getGameTick();

        return new UUID(actorMost ^ Long.rotateLeft(gameTick, 32), actorLeast ^ gameTick ^ sequence);
    }

    private static String buildContent(WorldEvent event) {
        String actorId = event.getActorId() != null ? event.getActorId().toString() : null;
        return ObservationContentRenderer.render(event.getType().name(), actorId, event.getMetadata());
    }

}
