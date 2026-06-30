package dev.breezes.settlements.application.ai.inference;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Capability-agnostic configuration for the external inference backend.
 */
@BehaviorConfig(name = "inference", type = ConfigurationType.GENERAL)
public record InferenceConfig(

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "endpoint_base_url",
                description = "Base URL of the inference service, e.g. http://localhost:12345",
                defaultValue = "")
        String endpointBaseUrl,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "api_key",
                description = "Pre-shared bearer token for the inference service. Empty disables authentication",
                defaultValue = "")
        String apiKey,

        @StringConfig(
                type = ConfigurationType.GENERAL,
                identifier = "locale",
                description = "Server locale used for generated inference text",
                defaultValue = "en_us")
        String locale,

        @IntegerConfig(
                type = ConfigurationType.GENERAL,
                identifier = "max_episodic_entries",
                description = "Maximum episodic memory entries sent to the inference service per villager per request. "
                        + "Higher values give the model more to draw on at the cost of a larger prompt",
                defaultValue = 50,
                min = 1,
                max = 256)
        int maxEpisodicEntries

) {

    public boolean hasEndpoint() {
        return this.endpointBaseUrl != null && !this.endpointBaseUrl.isBlank();
    }

    public boolean hasApiKey() {
        return this.apiKey != null && !this.apiKey.isBlank();
    }

    public String normalizedBaseUrl() {
        if (this.endpointBaseUrl == null) {
            return "";
        }

        return this.endpointBaseUrl.stripTrailing().replaceAll("/+$", "");
    }

}
