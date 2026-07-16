package dev.breezes.settlements.application.ai.inference;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

/**
 * Capability-agnostic configuration for the external inference backend.
 * <p>
 * {@code enabled} is the central kill-switch for every SIS/cognition path in the mod: with the
 * default ({@code false}), the mod never dials the endpoint regardless of what any per-capability
 * mode (dialogue, plan, ...) is set to. See {@link dev.breezes.settlements.application.ai.inference.InferenceGate}
 * for the single predicate every registration seam defers to.
 */
@BehaviorConfig(name = "inference", type = ConfigurationType.INFERENCE)
public record InferenceConfig(

        @BooleanConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "enabled",
                description = "Enable Settlements Inference Service",
                defaultValue = false)
        boolean enabled,

        @StringConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "endpoint_base_url",
                description = "Base URL of the inference service, e.g. http://127.0.0.1:12345",
                defaultValue = "")
        String endpointBaseUrl,

        @StringConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "api_key",
                description = "Pre-shared bearer token for the inference service. Empty disables authentication",
                defaultValue = "")
        String apiKey,

        @StringConfig(
                type = ConfigurationType.INFERENCE,
                identifier = "locale",
                description = "Server locale used for generated inference text",
                defaultValue = "en_us")
        String locale,

        @IntegerConfig(
                type = ConfigurationType.INFERENCE,
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
