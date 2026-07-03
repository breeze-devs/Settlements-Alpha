package dev.breezes.settlements.application.ai.speech;

/**
 * Discriminates the kind ("register") of a villager utterance.
 * <p>
 * {@link #MONOLOGUE}, {@link #GOSSIP}, and {@link #DIALOGUE} are speech: they mirror to the
 * chat log when the chat mirror is enabled. {@link #AMBIENT} is a non-verbal caption (e.g. a
 * gossip "psst..." lean-in or a "(listening)" choreography bubble) that renders a bubble but
 * is never mirrored to chat.
 */
public enum SpeechRegister {

    MONOLOGUE,
    GOSSIP,
    DIALOGUE,
    AMBIENT,
    ;

}
