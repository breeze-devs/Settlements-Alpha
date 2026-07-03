package dev.breezes.settlements.application.ai.speech;

/**
 * A consumer of {@link VillagerUtterance}s, e.g. a bubble renderer or a chat mirror.
 */
public interface VillagerSpeechSink {

    void accept(VillagerUtterance utterance);

}
