package dev.breezes.settlements.application.ai.inference;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Capability discriminator for requests sent through the shared inference backend transport.
 */
@AllArgsConstructor
@Getter
public enum InferenceCapability {

    MONOLOGUE("/v1/monologue");

    private final String path;

}
