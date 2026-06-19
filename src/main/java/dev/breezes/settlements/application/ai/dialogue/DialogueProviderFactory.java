package dev.breezes.settlements.application.ai.dialogue;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;

/**
 * Creates the appropriate {@link DialogueProvider} based on {@link DialogueConfig#resolvedMode()}.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogueProviderFactory {

    /**
     * Creates and returns the provider for the configured dialogue mode
     *
     * @param config the dialogue configuration snapshot
     * @return the concrete provider for the configured mode
     */
    public static DialogueProvider create(@Nonnull DialogueConfig config, @Nonnull DialogueLineIndex lineIndex) {
        DialogueProvider scripted = new ScriptedDialogueProvider(lineIndex, config);
        return switch (config.resolvedMode()) {
            case SCRIPTED -> {
                log.info("DialogueProvider: SCRIPTED — localized built-in dialogue floor");
                yield scripted;
            }
            case REHEARSED -> {
                log.warn("DialogueProvider: REHEARSED selected, but MONOLOGUE runtime sweep is deferred to phase 3 — using SCRIPTED floor");
                yield scripted;
            }
        };
    }

}
