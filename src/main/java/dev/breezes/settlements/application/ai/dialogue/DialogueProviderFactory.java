package dev.breezes.settlements.application.ai.dialogue;

import dagger.Lazy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Creates the {@link DialogueProvider} for the effective dialogue mode.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DialogueProviderFactory {

    /**
     * Creates and returns the provider for the effective dialogue mode, as {@link #effectiveMode} resolves it.
     * <p>
     * In REHEARSED mode the scripted floor is wrapped as the fallback rung inside
     * {@link RehearsedDialogueProvider}, so villagers always degrade gracefully when no
     * pre-generated pack is available. monologueRequestService is resolved only in REHEARSED mode, so SCRIPTED never
     * constructs the monologue gateway → HTTP transport chain.
     *
     * @param config                  the always-available dialogue configuration snapshot
     * @param rehearsedDialogueConfig the SIS-backed dialogue configuration snapshot (mode, pack sizing)
     * @param lineIndex               scripted line catalog used by the scripted floor
     * @param monologueRequestService handles SIS round-trips for the evening sweep, lazily resolved
     * @param inferenceEnabled        the resolved InferenceGate state
     * @return the concrete provider for the effective mode
     */
    public static DialogueProvider create(@Nonnull DialogueConfig config,
                                          @Nonnull RehearsedDialogueConfig rehearsedDialogueConfig,
                                          @Nonnull DialogueLineIndex lineIndex,
                                          @Nonnull Lazy<MonologueRequestService> monologueRequestService,
                                          boolean inferenceEnabled) {
        DialogueProvider scriptedProvider = new ScriptedDialogueProvider(lineIndex, config);

        return switch (effectiveMode(rehearsedDialogueConfig, inferenceEnabled)) {
            case SCRIPTED -> scriptedProvider;
            case REHEARSED -> new RehearsedDialogueProvider(scriptedProvider, monologueRequestService.get());
        };
    }

    /**
     * Returns the dialogue mode a provider created from the same arguments runs in: the configured mode while the
     * Settlements Inference Service is enabled, and SCRIPTED regardless of the configured value while it is off.
     *
     * @param rehearsedDialogueConfig the SIS-backed dialogue configuration snapshot
     * @param inferenceEnabled        the resolved InferenceGate state
     */
    public static DialogueMode effectiveMode(@Nonnull RehearsedDialogueConfig rehearsedDialogueConfig,
                                             boolean inferenceEnabled) {
        return inferenceEnabled ? rehearsedDialogueConfig.resolvedMode() : DialogueMode.SCRIPTED;
    }

}
