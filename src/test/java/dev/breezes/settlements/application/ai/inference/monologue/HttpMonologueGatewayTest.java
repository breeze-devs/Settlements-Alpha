package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.InferenceTransportResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpMonologueGatewayTest {

    @Test
    void generate_postsMonologueCapabilityAndParsesResponse() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder()
                .locale("en_us")
                .build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String body = """
                {
                  "protocolVersion": 1,
                  "requestId": "00000000-0000-0000-0000-000000000001",
                  "payload": {
                    "villagers": [
                      {
                        "villagerId": "%s",
                        "buckets": {
                          "WORK": [ { "text": "These furrows won't hoe themselves.", "status": "OK" } ]
                        }
                      }
                    ]
                  }
                }
                """.formatted(villagerId);
        when(transport.post(eq(InferenceCapability.MONOLOGUE), eq(request), eq(deadline)))
                .thenReturn(CompletableFuture.completedFuture(InferenceTransportResponse.success(body, 200)));

        // Act
        MonologueBatchResponse response = gateway.generate(request, deadline).join();

        // Assert
        assertEquals(1, response.getVillagers().size());
        assertEquals(villagerId, response.getVillagers().getFirst().getVillagerId());
        assertEquals("These furrows won't hoe themselves.",
                response.getVillagers().getFirst().getBuckets().get(Occasion.WORK).getFirst().getText());
        verify(transport).post(InferenceCapability.MONOLOGUE, request, deadline);
    }

    @Test
    void generate_defaultsMissingJsonContractFieldsAfterGsonParsing() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder()
                .locale("en_us")
                .build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String body = """
                {
                  "protocolVersion": 1,
                  "requestId": "00000000-0000-0000-0000-000000000002",
                  "payload": {
                    "villagers": [
                      {
                        "villagerId": "%s",
                        "buckets": {
                          "WORK": [ { "text": "The mill hums well today." } ]
                        }
                      }
                    ]
                  }
                }
                """.formatted(villagerId);
        when(transport.post(eq(InferenceCapability.MONOLOGUE), eq(request), eq(deadline)))
                .thenReturn(CompletableFuture.completedFuture(InferenceTransportResponse.success(body, 200)));

        // Act
        MonologueBatchResponse response = gateway.generate(request, deadline).join();

        // Assert
        GeneratedLine line = response.getVillagers().getFirst().getBuckets().get(Occasion.WORK).getFirst();
        assertEquals("The mill hums well today.", line.getText());
        assertEquals(LineStatus.OK, line.getStatus());
    }

    @Test
    void generate_returnsEmptyResponseWhenTransportMisses() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder()
                .locale("en_us")
                .build();
        Duration deadline = Duration.ofSeconds(30);
        when(transport.post(eq(InferenceCapability.MONOLOGUE), eq(request), eq(deadline)))
                .thenReturn(CompletableFuture.completedFuture(InferenceTransportResponse.miss()));

        // Act
        MonologueBatchResponse response = gateway.generate(request, deadline).join();

        // Assert
        assertTrue(response.getVillagers().isEmpty());
    }

    @Test
    void generate_returnsEmptyResponseWhenProtocolVersionMismatches() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder()
                .locale("en_us")
                .build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String body = """
                {
                  "protocolVersion": 2,
                  "requestId": "00000000-0000-0000-0000-000000000003",
                  "payload": {
                    "villagers": [
                      {
                        "villagerId": "%s",
                        "buckets": {
                          "WORK": [ { "text": "These furrows won't hoe themselves.", "status": "OK" } ]
                        }
                      }
                    ]
                  }
                }
                """.formatted(villagerId);
        when(transport.post(eq(InferenceCapability.MONOLOGUE), eq(request), eq(deadline)))
                .thenReturn(CompletableFuture.completedFuture(InferenceTransportResponse.success(body, 200)));

        // Act
        MonologueBatchResponse response = gateway.generate(request, deadline).join();

        // Assert
        assertTrue(response.getVillagers().isEmpty());
    }

    @Test
    void dtoBuilder_createsMonologueBatchPayload() {
        // Arrange
        UUID villagerId = UUID.randomUUID();

        // Act
        MonologueBatchRequest request = MonologueBatchRequest.builder()
                .locale("en_us")
                .villager(VillagerMonologueRequest.builder()
                        .villagerId(villagerId)
                        .persona(PersonaBundle.builder()
                                .professionKey("farmer")
                                .traits(List.of("diligent", "sociable"))
                                .build())
                        .seed("the wheat by the river came in thick this season")
                        .bucket(OccasionBucketSpec.builder()
                                .occasion(Occasion.WORK)
                                .lineCount(6)
                                .build())
                        .build())
                .build();

        // Assert
        assertEquals("en_us", request.getLocale());
        assertEquals(villagerId, request.getVillagers().getFirst().getVillagerId());
        assertEquals("farmer", request.getVillagers().getFirst().getPersona().getProfessionKey());
        assertEquals(Occasion.WORK, request.getVillagers().getFirst().getBuckets().getFirst().getOccasion());
    }

}
