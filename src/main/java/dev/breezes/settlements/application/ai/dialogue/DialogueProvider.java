package dev.breezes.settlements.application.ai.dialogue;

import java.util.Optional;
import java.util.UUID;

/**
 * The single seam between the villager AI and whatever inference path is configured.
 * <p>
 * All dialog output flows through this interface. Game-state decisions (knowledge transfer,
 * relationship deltas, credibility moves) are made deterministically before calling
 * this provider. The provider only renders words — it never mutates game state.
 * <p>
 * Implementations must be thread-safe: the provider may schedule async HTTP calls internally,
 * but it is constructed on the server thread and its methods may be called from it.
 */
public interface DialogueProvider {

    /**
     * Samples a single ambient utterance line for the given villager to display in a FLAVOR
     * bubble. The call returns immediately: SCRIPTED returns a translatable key, while
     * REHEARSED returns a literal model line when available and otherwise falls back to SCRIPTED.
     * <p>
     * The result, if present, has already been sanitized by {@link DialogueResponseSanitizer}.
     *
     * @param villagerUuid the unique id of the speaking villager
     * @param context      structured prompt context assembled by the caller
     * @return a sanitized, displayable line, or empty when the configured floor is silent
     */
    Optional<DialogueLine> sampleAmbientLine(UUID villagerUuid, DialogueContext context);

    /**
     * Kicks off the evening batch sweep. Called once per in-game evening from the server-tick event.
     * <p>
     * This is a no-op in SCRIPTED mode; REHEARSED mode performs async generation within
     * the configured sweep deadline.
     *
     */
    void runEveningPackSweep();

    /**
     * Returns {@code true} if this provider is effectively enabled — i.e. will ever produce
     * non-empty results. Callers can skip building the context object when the provider is
     * disabled, avoiding any wasted work.
     */
    boolean isEnabled();

    /**
     * Returns true for providers that can use evening batch context collection.
     */
    default boolean supportsRehearsedDialogSweep() {
        return false;
    }

}
