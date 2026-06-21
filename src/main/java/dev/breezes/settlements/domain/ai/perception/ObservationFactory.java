package dev.breezes.settlements.domain.ai.perception;

import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEvent;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts a gate-admitted {@link WorldEvent} into an {@link Observation}
 * ready for the {@link dev.breezes.settlements.domain.ai.observation.ObservationBuffer}
 * <p>
 * Importance baselines are tuned so:
 * <ul>
 *   <li>Threats and social acts (trade, courtship) score above the
 *       {@link dev.breezes.settlements.application.ai.memory.MemoryImportanceGate#PROMOTION_THRESHOLD}
 *       even before gene/novelty modifiers.</li>
 *   <li>Routine behavior lifecycle events start below the threshold and only
 *       promote for high-intelligence villagers observing novel activity.</li>
 * </ul>
 * <p>
 * Metadata is materialized only when an observation promotes to knowledge. Most admitted
 * observations are short-lived scoring candidates, so delaying map allocation keeps the
 * per-villager perception hot path lean.
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
        ObservationType type = mapToObservationType(event.getType());
        float baseImportance = baseImportanceFor(event.getType());
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
                .posX(event.getPosX())
                .posY(event.getPosY())
                .posZ(event.getPosZ())
                .build();
    }

    /**
     * Builds the persistence metadata only after the importance gate chooses to promote.
     */
    public static Map<String, String> metadataFor(@Nonnull Observation observation) {
        Map<String, String> metadata = new HashMap<>(8);
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
        if (observation.outcome() != null) {
            metadata.put(ObservationMetadataKeys.OUTCOME, observation.outcome().name());
        }
        if (observation.reason() != null) {
            metadata.put(ObservationMetadataKeys.REASON, observation.reason());
        }
        if (observation.detail() != null) {
            metadata.put(ObservationMetadataKeys.DETAIL, observation.detail());
        }

        metadata.put(ObservationMetadataKeys.POS_X, String.valueOf(observation.posX()));
        metadata.put(ObservationMetadataKeys.POS_Y, String.valueOf(observation.posY()));
        metadata.put(ObservationMetadataKeys.POS_Z, String.valueOf(observation.posZ()));

        return Map.copyOf(metadata);
    }

    private static UUID observationIdFor(@Nonnull WorldEvent event) {
        UUID actorId = event.getActorId();
        long actorMost = actorId != null ? actorId.getMostSignificantBits() : 0L;
        long actorLeast = actorId != null ? actorId.getLeastSignificantBits() : 0L;
        long sequence = event.getSequence();
        long gameTick = event.getGameTick();

        return new UUID(actorMost ^ Long.rotateLeft(gameTick, 32), actorLeast ^ gameTick ^ sequence);
    }

    private static ObservationType mapToObservationType(@Nonnull WorldEventType eventType) {
        return switch (eventType) {
            case BEHAVIOR_STARTED, BEHAVIOR_COMPLETED -> ObservationType.TASK_COMPLETION;
            case BEHAVIOR_FAILED -> ObservationType.TASK_FAILURE;
            case SHEEP_SHEARED, SHEEP_DYED, CROP_HARVESTED -> ObservationType.RESOURCE;
            case TRADE_COMPLETED, COURTSHIP_COMPLETED, COURTSHIP_REJECTED,
                 TRADE_INVITE_SENT, COURTSHIP_INVITE_SENT -> ObservationType.SOCIAL;
            // Confirmed/refuted tips are resource-relevant observations — the investigator
            // reports on a world-state condition, not a social act.
            case TIP_CONFIRMED, TIP_REFUTED -> ObservationType.RESOURCE;
            // Defensive mapping for system events
            case DAY_PLAN_INVALIDATED, PLAN_EXHAUSTED -> ObservationType.ENVIRONMENT;
        };
    }

    /**
     * Base importance scores
     * <p>
     * Social and resource events start above or at the promotion threshold so
     * most observing villagers will retain them. Routine lifecycle signals start
     * below threshold so only genuinely curious villagers notice them.
     */
    private static float baseImportanceFor(WorldEventType eventType) {
        return switch (eventType) {
            // Social acts are meaningful regardless of profession
            case TRADE_COMPLETED, COURTSHIP_COMPLETED, COURTSHIP_REJECTED -> 2.5F;
            case TRADE_INVITE_SENT, COURTSHIP_INVITE_SENT -> 2.0F;
            // Resource events are relevant mostly to profession-matched villagers
            case SHEEP_SHEARED, SHEEP_DYED, CROP_HARVESTED -> 1.8F;
            // Investigation results are useful to nearby villagers with similar resource goals
            case TIP_CONFIRMED -> 2.0F;
            case TIP_REFUTED -> 1.5F;
            // Lifecycle events are low-signal background noise
            case BEHAVIOR_STARTED, BEHAVIOR_COMPLETED, BEHAVIOR_FAILED -> 0.8F;
            // Defensively low for any system signal that slips through
            case DAY_PLAN_INVALIDATED, PLAN_EXHAUSTED -> 0.1F;
        };
    }

    private static String buildContent(WorldEvent event) {
        String actorStr = event.getActorId() != null ? event.getActorId().toString() : "unknown";
        String meta = event.getMetadata() != null ? " (" + event.getMetadata() + ")" : "";
        return event.getType().name().toLowerCase().replace("_", " ") + " by " + actorStr + meta;
    }

}
