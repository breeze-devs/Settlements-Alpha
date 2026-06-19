package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueLineIndex;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.DialogueProviderFactory;
import dev.breezes.settlements.application.ai.inference.HttpInferenceTransport;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.monologue.HttpMonologueGateway;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueGateway;
import dev.breezes.settlements.di.ServerScope;

/**
 * Exposes the {@link DialogueProvider} singleton to the server Dagger graph
 */
@Module
public final class DialogueServiceModule {

    @Provides
    @ServerScope
    static DialogueLineIndex dialogueLineIndex() {
        return new DialogueLineIndex();
    }

    @Provides
    @ServerScope
    static DialogueProvider dialogueProvider(DialogueConfig config, DialogueLineIndex lineIndex) {
        return DialogueProviderFactory.create(config, lineIndex);
    }

    @Provides
    @ServerScope
    static InferenceTransport inferenceTransport(HttpInferenceTransport transport) {
        return transport;
    }

    @Provides
    @ServerScope
    static MonologueGateway monologueGateway(HttpMonologueGateway gateway) {
        return gateway;
    }

}
