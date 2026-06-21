package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nullable;

/**
 * Translates a {@link WorldEventType} (plus resolved actor/target names) into a short
 * English clause suitable for use as a monologue seed.
 * <p>
 * This class owns ALL per-event English phrasing, keeping it out of the projector.
 * Every clause is written in the past tense and follows the fixed slot pattern:
 * <pre>  {actor} &lt;verb clause&gt; {target}</pre>
 * <p>
 * <b>Detail seam.</b>  Behaviors can inject a richer object description by placing a
 * string under the metadata key {@value #METADATA_KEY_DETAIL} on the knowledge entry.
 * Event types that support detail (e.g. {@code CROP_HARVESTED}, {@code TRADE_COMPLETED})
 * render {@code "<actor> <verb> <detail>"} when the key is present, and fall back to the
 * generic object when it is absent.
 * <p>
 * <b>Outcome/reason seam.</b>  Behaviors can signal a failed attempt by placing
 * {@link EventOutcome#FAILURE} under the metadata key {@value #METADATA_KEY_OUTCOME}.
 * A failure turns the completed-act clause into an attempt clause:
 * {@code "<actor> tried to <verb> <target> but <reason>"}. When {@value #METADATA_KEY_REASON}
 * is absent the fallback is a generic "but it fell through".
 * <p>
 * <b>Constant ownership.</b>  The three phrasing-relevant metadata keys are defined on
 * {@link ObservationMetadataKeys} and re-exported here as public aliases so callers in
 * this layer have a single stable import point.
 * <p>
 * <b>Adding a new event type.</b>  Add a branch in the switch below; the fallback chain
 * ({@link ObservationType} → coarse phrase) handles the gap between shipment and
 * the next deploy.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SeedPhrasebook {

    /**
     * Metadata key under which a behavior may place a rich detail string.
     * Example value: "3 melons", "4 bread for 1 emerald".
     * When absent the clause falls back to its generic object (e.g. "crops", "someone").
     */
    public static final String METADATA_KEY_DETAIL = ObservationMetadataKeys.DETAIL;

    /**
     * Metadata key under which a behavior may place a structured {@link EventOutcome} name.
     * When absent, outcome is treated as {@link EventOutcome#SUCCESS}.
     */
    public static final String METADATA_KEY_OUTCOME = ObservationMetadataKeys.OUTCOME;

    /**
     * Metadata key under which a behavior may place a free-text reason fragment.
     * Rendered after "but" in failure clauses: e.g. "but no bed was found".
     * When absent and outcome is FAILURE, falls back to "it fell through".
     */
    public static final String METADATA_KEY_REASON = ObservationMetadataKeys.REASON;

    /**
     * Produces a third-person clause for the given event type, with outcome/reason awareness.
     * <p>
     * When {@code outcome} is {@link EventOutcome#FAILURE}, the clause shifts to an attempt form:
     * {@code "<actor> tried to <verb> <target> but <reason>"}. Absent reason falls back to
     * "it fell through". Absent or {@code SUCCESS} outcome renders the normal completed-act form.
     *
     * @param eventType  the event kind; may be null (returns fallback clause)
     * @param actorName  resolved name of the actor; never null
     * @param targetName resolved name of the target entity, or null if not applicable
     * @param detail     optional behavior-supplied detail string (see {@link #METADATA_KEY_DETAIL});
     *                   null means "use generic object"
     * @param outcome    optional structured outcome; null or SUCCESS renders normally
     * @param reason     optional reason fragment for failure clauses; null falls back to generic
     * @return a non-null, non-empty clause in past tense (or attempt form for FAILURE)
     */
    public static String phraseClause(@Nullable WorldEventType eventType,
                                      String actorName,
                                      @Nullable String targetName,
                                      @Nullable String detail,
                                      @Nullable EventOutcome outcome,
                                      @Nullable String reason) {
        return phraseClause(eventType, actorName, targetName, detail, outcome, reason, null);
    }

    /**
     * Produces a third-person clause with optional behavior-key metadata for lifecycle events.
     * Behavior keys are used only as a fallback phrasing source, so deed events with rich detail
     * such as "0 pumpkins" keep their natural deed wording instead of being over-classified.
     */
    public static String phraseClause(@Nullable WorldEventType eventType,
                                      String actorName,
                                      @Nullable String targetName,
                                      @Nullable String detail,
                                      @Nullable EventOutcome outcome,
                                      @Nullable String reason,
                                      @Nullable String eventMetadata) {
        if (eventType == null) {
            return actorName + " did something";
        }

        if (eventType == WorldEventType.BEHAVIOR_FAILED) {
            return actorName + " tried to " + readableTask(eventMetadata) + " but " + failureReason(reason);
        }

        // Route failed attempts to the attempt-phrasing path before the normal switch.
        // Resource events (harvest/shear) have no meaningful failure variant per design,
        // so they fall through to success phrasing even if FAILURE is somehow set.
        if (outcome == EventOutcome.FAILURE) {
            return phraseFailureClause(eventType, actorName, targetName, reason);
        }

        return switch (eventType) {
            case TRADE_COMPLETED -> {
                // "traded [<items>] with <partner>": detail carries the items (e.g.
                // "4 bread for 1 emerald"), targetName the partner. The "with <partner>" clause is
                // dropped when the partner is unknown but items are present, so a detailed trade
                // still reads cleanly without a "with someone" filler.
                String base = actorName + " traded";
                if (detail != null) {
                    base = base + " " + detail;
                }
                if (targetName != null) {
                    yield base + " with " + targetName;
                }
                if (detail != null) {
                    yield base;
                }
                yield base + " with someone";
            }
            case COURTSHIP_COMPLETED -> {
                String object = targetName != null ? targetName : "someone";
                yield actorName + " courted " + object;
            }
            case SHEEP_SHEARED -> {
                // detail might be e.g. "3 wool"
                String object = detail != null ? detail : "a sheep";
                yield actorName + " sheared " + object;
            }
            case SHEEP_DYED -> {
                // detail might be e.g. "3 sheep"
                String object = detail != null ? detail : "a sheep";
                yield actorName + " dyed " + object;
            }
            case CROP_HARVESTED -> {
                // detail might be e.g. "3 melons"
                String object = detail != null ? detail : "crops";
                yield actorName + " harvested " + object;
            }
            case TIP_CONFIRMED -> actorName + " confirmed a rumour";
            case TIP_REFUTED -> actorName + " disproved a rumour";
            case TRADE_INVITE_SENT -> actorName + " sent a trade invite";
            case COURTSHIP_INVITE_SENT -> actorName + " sent a courtship invite";
            case BEHAVIOR_STARTED -> actorName + " started a task";
            case BEHAVIOR_COMPLETED -> actorName + " finished " + readableTask(eventMetadata);
            case COW_MILKED -> {
                String object = detail != null ? detail : "a cow";
                yield actorName + " milked " + object;
            }
            case FISH_CAUGHT -> {
                String object = detail != null ? detail : "a fish";
                yield actorName + " caught " + object;
            }
            case STONE_CUT -> {
                String object = detail != null ? detail : "stone";
                yield actorName + " cut " + object;
            }
            case MEAT_SMOKED -> {
                String object = detail != null ? detail : "meat";
                yield actorName + " smoked " + object;
            }
            case ORE_SMELTED -> {
                String object = detail != null ? detail : "ore";
                yield actorName + " smelted " + object;
            }
            case LIVESTOCK_BUTCHERED -> {
                String object = detail != null ? detail : "an animal";
                yield actorName + " butchered " + object;
            }
            case ITEM_ENCHANTED -> {
                String object = detail != null ? detail : "an object";
                yield actorName + " enchanted " + object;
            }
            case LEATHER_DYED -> {
                // detail carries color e.g. "red"
                String object = detail != null ? "leather " + detail : "leather";
                yield actorName + " dyed " + object;
            }
            case LEATHER_WASHED -> actorName + " washed leather";
            case ANIMAL_BRED -> {
                String object = detail != null ? detail : "animals";
                yield actorName + " bred " + object;
            }
            case ANIMAL_TAMED -> {
                String object = detail != null ? detail : "an animal";
                yield actorName + " tamed " + object;
            }
            case WOLF_WASHED -> actorName + " washed their dog";
            case WOLF_FED -> actorName + " fed their dog";
            case DOG_WALKED -> actorName + " walked their dog";
            case GOLEM_REPAIRED -> actorName + " repaired the iron golem";
            case POTION_THROWN -> {
                String target = targetName != null ? targetName : "someone";
                yield actorName + " threw a potion at " + target;
            }
            case BELL_RUNG -> actorName + " rang the village bell";
            case TARGET_EGGED -> {
                String target = targetName != null ? targetName : "someone";
                yield actorName + " egged " + target;
            }
            case CHICKENS_CHASED -> actorName + " chased a chicken around the village";
            case LANDSCAPE_SURVEYED -> actorName + " surveyed the surrounding terrain";
            case ITEMS_TAKEN -> {
                String object = detail != null ? detail : "some items";
                yield actorName + " took " + object + " from a chest";
            }
            case ITEMS_STORED -> {
                String object = detail != null ? detail : "some items";
                yield actorName + " stored " + object;
            }
            case ITEM_COLLECTED -> {
                String object = detail != null ? detail : "an item";
                yield actorName + " collected " + object;
            }
            // System-namespace events should never reach seeds; defensive fallback.
            default -> actorName + " did something";
        };
    }

    /**
     * Convenience overload for callers that have no outcome/reason context.
     * Delegates to the full overload with null outcome and null reason.
     * Exists to keep existing call sites compiling unchanged.
     *
     * @param eventType  the event kind; may be null (returns fallback clause)
     * @param actorName  resolved name of the actor; never null
     * @param targetName resolved name of the target entity, or null if not applicable
     * @param detail     optional behavior-supplied detail string; null means "use generic object"
     * @return a non-null, non-empty clause in past tense
     */
    public static String phraseClause(@Nullable WorldEventType eventType,
                                      String actorName,
                                      @Nullable String targetName,
                                      @Nullable String detail) {
        return phraseClause(eventType, actorName, targetName, detail, null, null);
    }

    /**
     * Fallback clause when no {@link WorldEventType} is available, using the coarser
     * {@link ObservationType} phrasing. Never returns null.
     */
    public static String phraseFallback(String actorName, ObservationType type) {
        return switch (type) {
            case SOCIAL -> actorName + " interacted with someone";
            case RESOURCE -> actorName + " gathered resources";
            case TASK_COMPLETION -> actorName + " finished a task";
            case TASK_FAILURE -> actorName + " failed at something";
            case THREAT -> actorName + " faced a threat";
            case ENVIRONMENT -> actorName + " noticed a change nearby";
            case GOSSIP_RECEIVED -> actorName + " shared some news";
        };
    }

    /**
     * Renders the "tried to ... but ..." form for events with {@link EventOutcome#FAILURE}.
     * Resource events fall through to success phrasing because failure has no meaningful
     * variant for them (you either shear or you don't reach the sheep).
     * The reason fragment is appended verbatim after "but"; absent reason uses "it fell through".
     */
    private static String phraseFailureClause(@Nullable WorldEventType eventType,
                                              String actorName,
                                              @Nullable String targetName,
                                              @Nullable String reason) {
        String but = failureReason(reason);

        return switch (eventType) {
            case TRADE_COMPLETED -> {
                String partner = targetName != null ? " with " + targetName : "";
                yield actorName + " tried to trade" + partner + " but " + but;
            }
            case COURTSHIP_COMPLETED -> {
                String object = targetName != null ? " " + targetName : " someone";
                yield actorName + " tried to court" + object + " but " + but;
            }
            // Rejection is the receiver's deliberate act, not a failed attempt, so it reads
            // "<receiver> turned down <presenter>'s courtship because <reason>".
            case COURTSHIP_REJECTED -> {
                String object = targetName != null ? targetName + "'s courtship" : "a courtship";
                yield actorName + " turned down " + object + " because " + but;
            }
            // Resource events have no meaningful failure phrasing — render as success.
            case SHEEP_SHEARED -> actorName + " sheared a sheep";
            case SHEEP_DYED -> actorName + " dyed a sheep";
            case CROP_HARVESTED -> actorName + " harvested crops";
            // For any other event type, produce a generic attempt clause.
            default -> actorName + " tried to do something but " + but;
        };
    }

    private static String failureReason(@Nullable String reason) {
        return reason != null ? reason : "it fell through";
    }

    private static String readableTask(@Nullable String eventMetadata) {
        if (eventMetadata == null || eventMetadata.isBlank()) {
            return "a task";
        }

        return eventMetadata.replace('_', ' ').strip();
    }

}
