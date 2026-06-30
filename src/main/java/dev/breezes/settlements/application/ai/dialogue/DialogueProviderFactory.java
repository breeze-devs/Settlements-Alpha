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
     * Creates and returns the provider for the configured dialogue mode.
     * <p>
     * In REHEARSED mode the scripted floor is wrapped as the fallback rung inside
     * {@link RehearsedDialogueProvider}, so villagers always degrade gracefully when no
     * pre-generated pack is available.
     *
     * @param config                  the dialogue configuration snapshot
     * @param lineIndex               scripted line catalog used by the scripted floor
     * @param monologueRequestService handles SIS round-trips for the evening sweep
     * @return the concrete provider for the configured mode
     */
    public static DialogueProvider create(@Nonnull DialogueConfig config,
                                          @Nonnull DialogueLineIndex lineIndex,
                                          @Nonnull MonologueRequestService monologueRequestService) {
        DialogueProvider scriptedProvider = new ScriptedDialogueProvider(lineIndex, config);
        return switch (config.resolvedMode()) {
            case SCRIPTED -> {
                log.info("DialogueProvider: SCRIPTED — localized built-in dialogue floor");
                yield scriptedProvider;
            }
            case REHEARSED -> {
                log.info("DialogueProvider: REHEARSED — dynamically generate monologue with SCRIPTED as fallback");
                yield new RehearsedDialogueProvider(scriptedProvider, monologueRequestService);
            }
        };
    }

}
