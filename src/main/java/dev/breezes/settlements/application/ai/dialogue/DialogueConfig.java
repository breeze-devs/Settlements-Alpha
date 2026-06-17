package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Configuration record for the dialog service
 * <p>
 * {@link DialogueMode#OFF} is the default — no dialog service required, no network fields
 * are read or validated, and the mod is fully playable with no dialog backend service reachable
 * <p>
 * {@code apiKey} defaults to an empty string (treated as "no key configured")
 * It is intentionally not logged above DEBUG level and is never sent to the client
 */
@BehaviorConfig(name = "dialogue", type = ConfigurationType.GENERAL)
public record DialogueConfig(

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "mode",
                description = "Dialog mode: OFF (default, no dialog service), PACKS (evening batch), LIVE (per-utterance).",
                defaultValue = "OFF")
        String mode,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "endpoint_base_url",
                description = "Base URL of the dialog service, e.g. http://localhost:11434. Only required when mode is PACKS or LIVE.",
                defaultValue = "")
        String endpointBaseUrl,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "model",
                description = "Model name to request from the backend, e.g. gemma4:26b",
                defaultValue = "gemma4:26b")
        String model,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "api_key",
                description = "API key for the dialog service (can be empty)",
                defaultValue = "")
        String apiKey,

        // TODO: stuff below we should tune
        @DoubleConfig(
                type = ConfigurationType.GENERAL,
                identifier = "temperature",
                description = "Sampling temperature (0.0–2.0). Higher = more varied, lower = more deterministic.",
                defaultValue = 0.8,
                min = 0.0,
                max = 2.0)
        double temperature,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "max_output_tokens",
                description = "Hard output token cap sent to the backend. A bubble fits ~48 tokens with slack.",
                defaultValue = 48,
                min = 8,
                max = 256)
        int maxOutputTokens,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "bubble_char_cap",
                description = "Maximum characters of LLM output shown in a bubble after sanitization.",
                defaultValue = 120,
                min = 20,
                max = 300)
        int bubbleCharCap,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "max_concurrent_requests",
                description = "Maximum in-flight LLM requests at any moment. Requests over the cap " +
                        "are queued or dropped by priority.",
                defaultValue = 2,
                min = 1,
                max = 8)
        int maxConcurrentRequests,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "live_deadline_millis_ambient",
                description = "Deadline in ms for ambient / villager-to-villager LIVE requests. " +
                        "Missing the deadline is cosmetically invisible; the bubble just never appears.",
                defaultValue = 1500,
                min = 500,
                max = 10000)
        int liveDeadlineMillisAmbient,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "live_deadline_millis_player",
                description = "Deadline in ms for player-to-villager LIVE requests. " +
                        "Timeout triggers a canned fallback to keep degradation diegetic.",
                defaultValue = 3500,
                min = 500,
                max = 15000)
        int liveDeadlineMillisPlayer,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "pack_lines_per_villager",
                description = "How many candidate lines to generate per villager in the evening PACKS sweep.",
                defaultValue = 12,
                min = 1,
                max = 50)
        int packLinesPerVillager,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "pack_sweep_deadline_seconds",
                description = "Total time budget in seconds for the evening PACKS sweep across all villagers.",
                defaultValue = 30,
                min = 5,
                max = 120)
        int packSweepDeadlineSeconds

) {

    /**
     * Resolves the {@code mode} string to a {@link DialogueMode} enum value
     * Unrecognized strings default to {@link DialogueMode#OFF}
     */
    public DialogueMode resolvedMode() {
        try {
            return DialogueMode.valueOf(this.mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DialogueMode.OFF;
        }
    }

    /**
     * Returns {@code true} when the api key field contains a non-blank value that should be
     * included as a token. Empty / whitespace-only strings mean "no key configured."
     */
    public boolean hasApiKey() {
        return this.apiKey != null && !this.apiKey.isBlank();
    }

}
