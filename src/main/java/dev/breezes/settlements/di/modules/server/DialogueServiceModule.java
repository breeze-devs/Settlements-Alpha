package dev.breezes.settlements.di.modules.server;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueLineIndex;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.DialogueProviderFactory;
import dev.breezes.settlements.application.ai.dialogue.MonologueRequestService;
import dev.breezes.settlements.application.ai.dialogue.OccasionSetResolver;
import dev.breezes.settlements.application.ai.dialogue.RehearsedDialogueConfig;
import dev.breezes.settlements.application.ai.inference.HttpInferenceTransport;
import dev.breezes.settlements.application.ai.inference.InferenceGate;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.monologue.HttpMonologueGateway;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueGateway;
import dev.breezes.settlements.application.ai.inference.persona.HttpPersonaGateway;
import dev.breezes.settlements.application.ai.inference.persona.PersonaGateway;
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
    static OccasionSetResolver occasionSetResolver() {
        return new OccasionSetResolver();
    }

    @Provides
    @ServerScope
    static DialogueProvider dialogueProvider(DialogueConfig config,
                                             RehearsedDialogueConfig rehearsedDialogueConfig,
                                             DialogueLineIndex lineIndex,
                                             Lazy<MonologueRequestService> monologueRequestService,
                                             InferenceGate inferenceGate) {
        return DialogueProviderFactory.create(config, rehearsedDialogueConfig, lineIndex, monologueRequestService, inferenceGate.isEnabled());
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

    @Provides
    @ServerScope
    static PersonaGateway personaGateway(HttpPersonaGateway gateway) {
        return gateway;
    }

}
