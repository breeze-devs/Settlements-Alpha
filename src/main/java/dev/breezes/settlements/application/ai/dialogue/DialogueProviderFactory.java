package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.llm.DialogServiceHttpClient;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.annotation.Nonnull;

/**
 * Creates the appropriate {@link DialogueProvider} based on {@link DialogueConfig#resolvedMode()}.
 * <p>
 * This factory is the single place where the three modes are mapped to concrete providers,
 * keeping the Dagger module clean and making mode-specific construction explicit.
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
    public static DialogueProvider create(@Nonnull DialogueConfig config) {
        return switch (config.resolvedMode()) {
            case OFF -> {
                log.info("DialogueProvider: OFF — no dialog service will be contacted");
                yield new OffDialogueProvider();
            }
            case PACKS -> {
                if (isEndpointInvalid(config)) {
                    yield new OffDialogueProvider();
                }
                log.info("DialogueProvider: PACKS — evening batch sweep to {}", config.endpointBaseUrl());
                yield new PacksDialogueProvider(new DialogServiceHttpClient(config), config);
            }
            case LIVE -> {
                if (isEndpointInvalid(config)) {
                    yield new OffDialogueProvider();
                }
                log.info("DialogueProvider: LIVE — per-utterance calls to {}", config.endpointBaseUrl());
                yield new LiveDialogueProvider(new DialogServiceHttpClient(config), config);
            }
        };
    }

    /**
     * Returns whether a usable inference endpoint is configured. When it is not, the caller
     * falls back to {@link OffDialogueProvider} so a misconfigured PACKS/LIVE mode degrades to
     * silence instead of building a client that would throw on its first call.
     */
    private static boolean isEndpointInvalid(DialogueConfig config) {
        if (config.endpointBaseUrl() == null || config.endpointBaseUrl().isBlank()) {
            log.error("DialogueConfig: mode is {} but endpoint_base_url is not set — falling back to OFF. "
                    + "Set endpoint_base_url to enable generative dialog.", config.resolvedMode());
            return true;
        }
        return false;
    }

}
