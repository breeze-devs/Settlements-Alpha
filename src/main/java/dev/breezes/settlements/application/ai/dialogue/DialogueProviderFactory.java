package dev.breezes.settlements.application.ai.dialogue;

import dagger.Lazy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;

/**
 * Creates the appropriate {@link DialogueProvider} based on {@link RehearsedDialogueConfig#resolvedMode()}.
 */
@CustomLog
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogueProviderFactory {

    /**
     * Creates and returns the provider for the configured dialogue mode.
     * <p>
     * In REHEARSED mode the scripted floor is wrapped as the fallback rung inside
     * {@link RehearsedDialogueProvider}, so villagers always degrade gracefully when no
     * pre-generated pack is available. {@code monologueRequestService} is only ever {@code .get()}
     * inside the REHEARSED branch — SCRIPTED (and the inference-disabled short-circuit below) never
     * construct the monologue gateway → HTTP transport chain.
     *
     * @param config                  the always-available dialogue configuration snapshot
     * @param rehearsedDialogueConfig the SIS-backed dialogue configuration snapshot (mode, pack sizing)
     * @param lineIndex               scripted line catalog used by the scripted floor
     * @param monologueRequestService handles SIS round-trips for the evening sweep, lazily resolved
     * @param inferenceEnabled        the resolved {@code InferenceGate} state — the central
     *                                kill-switch subsumes {@code rehearsedDialogueConfig.mode()}:
     *                                when off, dialogue is forced to SCRIPTED regardless of the
     *                                configured value
     * @return the concrete provider for the configured mode
     */
    public static DialogueProvider create(@Nonnull DialogueConfig config,
                                          @Nonnull RehearsedDialogueConfig rehearsedDialogueConfig,
                                          @Nonnull DialogueLineIndex lineIndex,
                                          @Nonnull Lazy<MonologueRequestService> monologueRequestService,
                                          boolean inferenceEnabled) {
        DialogueProvider scriptedProvider = new ScriptedDialogueProvider(lineIndex, config);

        if (!inferenceEnabled) {
            log.info("DialogueProvider: SCRIPTED — Settlements Inference Service is off, dialogue mode forced regardless of configured value");
            return scriptedProvider;
        }

        return switch (rehearsedDialogueConfig.resolvedMode()) {
            case SCRIPTED -> {
                log.info("DialogueProvider: SCRIPTED — localized built-in dialogue floor");
                yield scriptedProvider;
            }
            case REHEARSED -> {
                log.info("DialogueProvider: REHEARSED — dynamically generate monologue with SCRIPTED as fallback");
                yield new RehearsedDialogueProvider(scriptedProvider, monologueRequestService.get());
            }
        };
    }

}
