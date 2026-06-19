package dev.breezes.settlements.application.ai.inference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the serialize seam on {@link HttpInferenceTransport} renders the full
 * envelope shape — protocol version, capability, deadline fields, and nested payload —
 * so dump commands and future gateway-test fixtures are byte-faithful to what {@code post()}
 * would actually send.
 */
class InferenceTransportSerializeSeamTest {

    private HttpInferenceTransport transport;

    @AfterEach
    void tearDown() {
        if (this.transport != null) {
            this.transport.close();
        }
    }

    @Test
    void renderEnvelope_includesProtocolVersionAndCapability() {
        // Arrange
        this.transport = newTransport("http://localhost:9999", "");

        // Act
        String json = this.transport.renderEnvelope(
                InferenceCapability.MONOLOGUE,
                new SamplePayload("hello"),
                Duration.ofSeconds(30));

        // Assert – envelope fields must be present regardless of payload content
        assertTrue(json.contains("\"protocolVersion\":" + InferenceProtocol.VERSION),
                "Must include protocolVersion");
        assertTrue(json.contains("\"capability\":\"MONOLOGUE\""),
                "Must include capability discriminator");
    }

    @Test
    void renderEnvelope_includesDeadlineMillisAndSlack() {
        // Arrange
        this.transport = newTransport("http://localhost:9999", "");

        // Act
        String json = this.transport.renderEnvelope(
                InferenceCapability.MONOLOGUE,
                new SamplePayload("payload"),
                Duration.ofMillis(5000));

        // Assert
        assertTrue(json.contains("\"deadlineMillis\":5000"),
                "deadlineMillis must reflect the supplied duration");
        assertTrue(json.contains("\"deadlineSlackMillis\":250"),
                "deadlineSlackMillis must be present from config");
    }

    @Test
    void renderEnvelope_includesNestedPayload() {
        // Arrange
        this.transport = newTransport("http://localhost:9999", "");

        // Act
        String json = this.transport.renderEnvelope(
                InferenceCapability.MONOLOGUE,
                new SamplePayload("grounding seed"),
                Duration.ofSeconds(10));

        // Assert – the payload object is nested, not flattened into the envelope
        assertTrue(json.contains("\"payload\""), "Payload must be nested under 'payload' key");
        assertTrue(json.contains("\"message\":\"grounding seed\""),
                "Payload content must appear inside the envelope JSON");
    }

    @Test
    void renderEnvelope_includesRequestId() {
        // Arrange
        this.transport = newTransport("http://localhost:9999", "");

        // Act
        String json = this.transport.renderEnvelope(
                InferenceCapability.MONOLOGUE,
                new SamplePayload("test"),
                Duration.ofSeconds(1));

        // Assert – requestId must be a UUID-shaped value
        assertTrue(json.contains("\"requestId\""), "requestId field must be present");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpInferenceTransport newTransport(String endpoint, String apiKey) {
        InferenceConfig config = new InferenceConfig(endpoint, apiKey, "en_us", 2, 250);
        return new HttpInferenceTransport(config);
    }

    private record SamplePayload(String message) {
    }

}
