package dev.breezes.settlements.application.ai.inference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InferenceConfigTest {

    @Test
    void normalizedBaseUrl_stripsTrailingWhitespaceAndSlashes() {
        // Arrange
        InferenceConfig config = new InferenceConfig(
                true,
                "http://localhost:12345//   ",
                "",
                "en_us",
                50);

        // Act
        String normalizedBaseUrl = config.normalizedBaseUrl();

        // Assert
        assertEquals("http://localhost:12345", normalizedBaseUrl);
    }

    @Test
    void normalizedBaseUrl_returnsEmptyWhenEndpointIsNull() {
        // Arrange
        InferenceConfig config = new InferenceConfig(true, null, "", "en_us", 50);

        // Act
        String normalizedBaseUrl = config.normalizedBaseUrl();

        // Assert
        assertEquals("", normalizedBaseUrl);
    }

}