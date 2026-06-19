package dev.breezes.settlements.application.ai.dialogue;

/**
 * Controls which inference path {@link DialogueProvider} uses
 * <p>
 * SCRIPTED is the backend-free floor. REHEARSED layers pre-generated model lines over that floor.
 */
public enum DialogueMode {

    /**
     * Hand-authored translation keys resolved locally and localized on each client.
     */
    SCRIPTED,

    /**
     * An evening batch generates per-villager utterance packs offline (zero daytime calls).
     */
    REHEARSED

}
