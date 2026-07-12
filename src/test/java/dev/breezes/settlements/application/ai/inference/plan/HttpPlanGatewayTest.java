package dev.breezes.settlements.application.ai.inference.plan;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The backend streams NDJSON: one {@link VillagerPlanResult} JSON object per line. Tests drive
 * the streaming path by mocking {@link InferenceTransport#postStreaming} to invoke the
 * {@code onLine} consumer synchronously, simulating lines arriving from the backend.
 */
class HttpPlanGatewayTest {

    @Test
    void generate_parsesAndDeliversSingleVillager() {
        // Arrange
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String line = villagerLine(villagerId, "MORNING", "HARVEST_MELON");

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle, line);

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        InferenceStreamHandle handle = gateway.generate(request, deadline, received::add);

        // Assert
        assertSame(mockHandle, handle);
        assertEquals(1, received.size());
        assertEquals(villagerId, received.getFirst().getVillagerId());
        assertEquals("HARVEST_MELON", received.getFirst().getSelections().get("MORNING").getFirst().getId());
        verify(transport).postStreaming(eq(InferenceCapability.PLAN), eq(request), eq(deadline), any());
    }

    @Test
    void generate_parsesAtBearingSelectionIntoPlanSelection() {
        // Arrange — a pinned {id, at} selection object must deserialize both fields
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String line = "{\"villagerId\":\"%s\",\"selections\":{\"EVENING\":[{\"id\":\"eat_food\",\"at\":\"AT_18_00\"}]}}"
                .formatted(villagerId);

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle, line);

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert
        assertEquals(1, received.size());
        PlanSelection selection = received.getFirst().getSelections().get("EVENING").getFirst();
        assertEquals("eat_food", selection.getId());
        assertEquals("AT_18_00", selection.getAt());
    }

    @Test
    void generate_parsesSelectionWithoutAtAsNullAt() {
        // Arrange — an ordinary (non-pinned) selection object must leave `at` null, not throw
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String line = villagerLine(villagerId, "MORNING", "HARVEST_MELON");

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle, line);

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert
        PlanSelection selection = received.getFirst().getSelections().get("MORNING").getFirst();
        assertEquals("HARVEST_MELON", selection.getId());
        assertNull(selection.getAt());
    }

    @Test
    void generate_parsesMultiLineNdjsonStream() {
        // Arrange — two villager lines must both parse (a single fromJson on the whole body would
        // throw on line 2).
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle,
                villagerLine(first, "MORNING", "HARVEST_MELON"),
                villagerLine(second, "AFTERNOON", "TRADE_GOODS"));

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert
        assertEquals(2, received.size());
        assertTrue(received.stream().anyMatch(r -> r.getVillagerId().equals(first)));
        assertTrue(received.stream().anyMatch(r -> r.getVillagerId().equals(second)));
        VillagerPlanResult firstResult = received.stream()
                .filter(r -> r.getVillagerId().equals(first)).findFirst().orElseThrow();
        assertEquals("HARVEST_MELON", firstResult.getSelections().get("MORNING").getFirst().getId());
    }

    @Test
    void generate_skipsMalformedLineButKeepsValidOnes() {
        // Arrange — a garbage line in the middle must not discard valid villagers.
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID valid = UUID.randomUUID();

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle,
                "{ this is not json",
                villagerLine(valid, "MORNING", "HARVEST_MELON"));

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert — only the valid villager arrives; callback is never invoked for the bad line
        assertEquals(1, received.size());
        assertEquals(valid, received.getFirst().getVillagerId());
    }

    @Test
    void generate_lineWithoutSelectionsFieldDeliversEmptySelections() {
        // Arrange — Gson bypasses the @Singular builder, so an omitted `selections` deserializes to a
        // null map. The gateway must coalesce it to empty so the downstream overlay mapping (which
        // iterates getSelections()) never NPEs on the "model chose nothing" shape.
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();
        Duration deadline = Duration.ofSeconds(30);
        UUID villagerId = UUID.randomUUID();
        String line = "{\"villagerId\":\"%s\"}".formatted(villagerId);

        InferenceStreamHandle mockHandle = mock(InferenceStreamHandle.class);
        deliverLines(transport, mockHandle, line);

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        gateway.generate(request, deadline, received::add);

        // Assert — the villager is still delivered, with a non-null empty selections map
        assertEquals(1, received.size());
        assertEquals(villagerId, received.getFirst().getVillagerId());
        assertNotNull(received.getFirst().getSelections(), "selections must be coalesced to non-null");
        assertTrue(received.getFirst().getSelections().isEmpty());
    }

    @Test
    void generate_returnsHandleWithNoCallbacksWhenNoEndpoint() {
        // Arrange — transport returns noOp for missing endpoint (no lines delivered)
        InferenceTransport transport = mock(InferenceTransport.class);
        HttpPlanGateway gateway = new HttpPlanGateway(transport);
        PlanBatchRequest request = PlanBatchRequest.builder().build();

        doAnswer(invocation -> InferenceStreamHandle.noOp())
                .when(transport).postStreaming(any(), any(), any(), any());

        List<VillagerPlanResult> received = new ArrayList<>();

        // Act
        InferenceStreamHandle handle = gateway.generate(request, Duration.ofSeconds(30), received::add);

        // Assert — noOp handle, completion already done, no villagers delivered
        assertTrue(handle.completion().isDone());
        assertTrue(received.isEmpty());
    }

    @Test
    void dtoBuilder_createsPlanBatchPayload() {
        // Arrange
        UUID villagerId = UUID.randomUUID();

        // Act
        PlanBatchRequest request = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .dayType("NORMAL")
                        .option(BehaviorOptionDTO.builder()
                                .id("HARVEST_MELON")
                                .description("Harvest the ripe melons.")
                                .category("WORK")
                                .intensity("MODERATE")
                                .estimatedMinutes(20)
                                .build())
                        .window(PlanWindowDTO.builder().id("MORNING").startTick(0).endTick(6000).build())
                        .build())
                .build();

        // Assert
        assertEquals(villagerId, request.getVillagers().getFirst().getVillagerId());
        assertEquals("NORMAL", request.getVillagers().getFirst().getDayType());
        assertEquals("HARVEST_MELON", request.getVillagers().getFirst().getOptions().getFirst().getId());
        assertEquals("MORNING", request.getVillagers().getFirst().getWindows().getFirst().getId());
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
     * Builds one NDJSON villager line in the response wire shape:
     * {@code {"villagerId":"…","selections":{windowId:[{"id":behaviorId},…]}}}.
     */
    private static String villagerLine(UUID villagerId, String windowId, String behaviorId) {
        return "{\"villagerId\":\"%s\",\"selections\":{\"%s\":[{\"id\":\"%s\"}]}}"
                .formatted(villagerId, windowId, behaviorId);
    }

}
