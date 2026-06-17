package dev.breezes.settlements.application.ai.dialogue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Structured prompt context for a single utterance request.
 * <p>
 * The provider assembles the dialog system block from these fields:
 * <ol>
 *   <li>{@link #personaCard} — name, profession, and trait words (assembled by the caller).</li>
 *   <li>{@link #groundingSeeds} — 3–5 topic seeds (e.g. confirmed/refuted tips, recent shared events).
 *          Curated, never dumped — prefill cost scales with token count.</li>
 *   <li>A fixed instruction appended by the provider: "Reply with one short, in-character sentence."</li>
 * </ol>
 * <p>
 * All fields are set by the server-side caller <em>before</em> the provider call. The model
 * never reads arbitrary game state — it only phrases what it is given.
 */
@Builder
@Getter
public final class DialogueContext {

    /**
     * Short persona card: name, profession, 2–3 trait words, a speech-style hint.
     * One or two lines; assembled from deterministic per-villager attributes.
     * Example: "Name: Elara, Profession: Farmer, Traits: cheerful, curious, blunt."
     */
    private final String personaCard;

    /**
     * Topic seeds drawn from knowledge, e.g. confirmed/refuted tips, recent shared events, relationship notes.
     */
    @Singular
    private final List<String> groundingSeeds;

    /**
     * The current stimulus driving the utterance — e.g. a player message or situational
     * description. May be {@code null} for ambient utterances with no specific trigger
     */
    private final String stimulus;

    /**
     * Priority tier, used by the request queue to order concurrent in-flight requests
     * Higher priority = earlier execution + last to be dropped under concurrency pressure
     */
    private final DialoguePriority priority;

}
