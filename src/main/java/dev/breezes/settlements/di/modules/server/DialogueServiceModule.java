package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.DialogueProvider;
import dev.breezes.settlements.application.ai.dialogue.DialogueProviderFactory;
import dev.breezes.settlements.di.ServerScope;

/**
 * Exposes the {@link DialogueProvider} singleton to the server Dagger graph
 */
@Module
public final class DialogueServiceModule {

    @Provides
    @ServerScope
    static DialogueProvider dialogueProvider(DialogueConfig config) {
        return DialogueProviderFactory.create(config);
    }

}
