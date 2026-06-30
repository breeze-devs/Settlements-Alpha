package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The backend streams NDJSON: one {@link VillagerMonologueResult} JSON object per line. These
 * fixtures mirror the real wire format captured from SIS — no enclosing envelope, and each
 * occasion bucket is an array of bare strings (a filtered line is omitted server-side, so no
 * per-line status rides the wire).
 * <p>
 * Tests drive the streaming path by mocking {@link InferenceTransport#postStreaming} to invoke
 * the {@code onLine} consumer synchronously, simulating lines arriving from the backend.
 */
class HttpMonologueGatewayTest {

    @Test
    void generate_parsesAndDeliversSingleVillager() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder().locale("en_us").build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String line = villagerLine(villagerId, "WORK", "These furrows won't hoe themselves.");

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle, line);

        List<VillagerMonologueResult> received = new ArrayList<>();

        // Act
        InferenceStreamHandle handle = gateway.generate(request, deadline, received::add);

        // Assert
        assertSame(mockHandle, handle);
        assertEquals(1, received.size());
        assertEquals(villagerId, received.getFirst().getVillagerId());
        assertEquals("These furrows won't hoe themselves.",
                received.getFirst().getBuckets().get(Occasion.WORK).getFirst());
        verify(transport).postStreaming(eq(InferenceCapability.MONOLOGUE), eq(request), eq(deadline), any());
    }

    @Test
    void generate_parsesMultiLineNdjsonStream() {
        // Arrange — two villager lines must both parse (the real regression: a single fromJson on
        // the whole body would throw on line 2).
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder().locale("en_us").build();
        Duration deadline = Duration.ofSeconds(30);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle,
                villagerLine(first, "WORK", "I tend the wheat."),
                villagerLine(second, "IDLE", "A fine day to wander."));

        List<VillagerMonologueResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert
        assertEquals(2, received.size());
        assertTrue(received.stream().anyMatch(r -> r.getVillagerId().equals(first)));
        assertTrue(received.stream().anyMatch(r -> r.getVillagerId().equals(second)));
        VillagerMonologueResult firstResult = received.stream()
                .filter(r -> r.getVillagerId().equals(first)).findFirst().orElseThrow();
        assertEquals("I tend the wheat.", firstResult.getBuckets().get(Occasion.WORK).getFirst());
    }

    @Test
    void generate_skipsMalformedLineButKeepsValidOnes() {
        // Arrange — a garbage line in the middle must not discard valid villagers.
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder().locale("en_us").build();
        Duration deadline = Duration.ofSeconds(30);
        UUID valid = UUID.randomUUID();

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle,
                "{ this is not json",
                villagerLine(valid, "MORNING", "Rise and shine."));

        List<VillagerMonologueResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert — only the valid villager arrives
        assertEquals(1, received.size());
        assertEquals(valid, received.getFirst().getVillagerId());
    }

    @Test
    void generate_returnsHandleWithNoCallbacksWhenNoEndpoint() {
        // Arrange — transport returns noOp for missing endpoint (no lines delivered)
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpMonologueGateway gateway = new HttpMonologueGateway(transport);
        MonologueBatchRequest request = MonologueBatchRequest.builder().locale("en_us").build();

        doAnswer(invocation -> InferenceStreamHandle.noOp())
                .when(transport).postStreaming(any(), any(), any(), any());

        List<VillagerMonologueResult> received = new ArrayList<>();

        // Act
        InferenceStreamHandle handle = gateway.generate(request, Duration.ofSeconds(30), received::add);

        // Assert — noOp handle, completion already done, no villagers delivered
        assertTrue(handle.completion().isDone());
        assertTrue(received.isEmpty());
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
                                .profession("minecraft:farmer")
                                .traits(List.of("diligent", "sociable"))
                                .anchors(Anchors.builder()
                                        .body(new int[]{10, 64, 20})
                                        .build())
                                .build())
                        .snapshot(Snapshot.builder().build())
                        .bucket(OccasionBucketSpec.builder()
                                .occasion(Occasion.WORK)
                                .lineCount(6)
                                .build())
                        .build())
                .build();

        // Assert
        assertEquals("en_us", request.getLocale());
        assertEquals(villagerId, request.getVillagers().getFirst().getVillagerId());
        assertEquals("minecraft:farmer", request.getVillagers().getFirst().getPersona().getProfession());
        assertEquals(Occasion.WORK, request.getVillagers().getFirst().getBuckets().getFirst().getOccasion());
    }

    /**
     * Stubs {@code transport.postStreaming} to synchronously deliver each string in {@code lines}
     * to the {@code onLine} consumer, then returns {@code handle}.
     */
    @SuppressWarnings("unchecked")
    private static void deliverLines(InferenceTransport transport,
                                     InferenceStreamHandle handle,
                                     String... lines) {
        doAnswer(invocation -> {
            Consumer<String> onLine = (Consumer<String>) invocation.getArgument(3);
            for (String line : lines) {
                onLine.accept(line);
            }
            return handle;
        }).when(transport).postStreaming(any(), any(), any(), any());
    }

    /**
     * Builds one NDJSON villager line in SIS's wire shape: {@code {"villagerId":"…","buckets":{occasion:[string,…]}}}.
     */
    private static String villagerLine(UUID villagerId, String occasion, String text) {
        return "{\"villagerId\":\"%s\",\"buckets\":{\"%s\":[\"%s\"]}}"
                .formatted(villagerId, occasion, text);
    }

}
