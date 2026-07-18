package dev.breezes.settlements.application.ai.speech;

import dev.breezes.settlements.di.ServerScope;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Set;

/**
 * The single fan-out point for villager speech: producers depend only on this and call
 * {@link #utter}, which dispatches to every registered {@link VillagerSpeechSink}.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerSpeechService {

    private final Set<VillagerSpeechSink> sinks;

    public void utter(VillagerUtterance utterance) {
        for (VillagerSpeechSink sink : this.sinks) {
            sink.accept(utterance);
        }
    }

}
