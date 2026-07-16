package dev.breezes.settlements.application.ai.inference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic coverage for the central kill-switch predicate. No Minecraft state is touched — the gate
 * only combines two booleans off an {@link InferenceConfig} snapshot.
 */
class InferenceGateTest {

    private static final String ENDPOINT = "http://127.0.0.1:12345";

    @Test
    void isEnabled_whenEnabledAndEndpointPresent_isOn() {
        // Arrange
        InferenceGate gate = gateFor(true, ENDPOINT);

        // Act & Assert
        assertTrue(gate.isEnabled());
        assertFalse(gate.isMisconfigured());
    }

    @Test
    void isEnabled_whenEnabledButEndpointBlank_isOffAndMisconfigured() {
        // Arrange
        InferenceGate gate = gateFor(true, "");

        // Act & Assert
        assertFalse(gate.isEnabled());
        assertTrue(gate.isMisconfigured());
    }

    @Test
    void isEnabled_whenDisabledWithEndpoint_isOffAndNotMisconfigured() {
        // Arrange — the explicit switch wins over a configured endpoint.
        InferenceGate gate = gateFor(false, ENDPOINT);

        // Act & Assert
        assertFalse(gate.isEnabled());
        assertFalse(gate.isMisconfigured());
    }

    @Test
    void isEnabled_whenDisabledAndBlank_isOff() {
        // Arrange — the default-config case.
        InferenceGate gate = gateFor(false, "");

        // Act & Assert
        assertFalse(gate.isEnabled());
        assertFalse(gate.isMisconfigured());
    }

    private static InferenceGate gateFor(boolean enabled, String endpoint) {
        InferenceConfig config = new InferenceConfig(enabled, endpoint, "", "en_us", 50);
        return new InferenceGate(config);
    }

}
