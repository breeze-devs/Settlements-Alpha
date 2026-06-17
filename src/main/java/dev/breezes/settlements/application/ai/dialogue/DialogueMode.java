package dev.breezes.settlements.application.ai.dialogue;

/**
 * Controls which inference path {@link DialogueProvider} uses
 * <p>
 * OFF is the default and requires no backend dialog service reachable — the mod is fully playable
 * without a dedicated dialog service endpoint. PACKS and LIVE both talk to the dialog service,
 * they differ only in when the call happens.
 */
public enum DialogueMode {

    /**
     * No dialog generation. No network configuration is required.
     */
    OFF,

    /**
     * An evening batch generates per-villager utterance packs offline (zero daytime calls).
     */
    PACKS,

    /**
     * A per-utterance call at speaking time with a tight deadline.
     */
    LIVE

}
