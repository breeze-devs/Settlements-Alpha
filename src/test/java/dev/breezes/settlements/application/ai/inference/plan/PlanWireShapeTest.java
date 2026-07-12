package dev.breezes.settlements.application.ai.inference.plan;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.breezes.settlements.application.ai.inference.HttpInferenceTransport;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.application.ai.inference.monologue.Anchors;
import dev.breezes.settlements.application.ai.inference.monologue.EpisodicEntryDTO;
import dev.breezes.settlements.application.ai.inference.monologue.PersonaBundle;
import dev.breezes.settlements.application.ai.inference.monologue.Snapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the PLAN envelope wire shape against the SIS contract.
 * <p>
 * All DTOs are constructed by hand — no Minecraft objects. The assertions mirror SIS's
 * extra="forbid" validation: any unknown key here would produce a 422 in SIS. This test is the
 * source of truth for the wire shape SIS must mirror for the PLAN capability.
 */
class PlanWireShapeTest {

    private static final Gson GSON = new Gson();
    private HttpInferenceTransport transport;

    @AfterEach
    void tearDown() {
        if (this.transport != null) {
            this.transport.close();
        }
    }

    @Test
    void envelope_topLevelKeysMatchSisContract() {
        // Arrange
        this.transport = newTransport();
        PlanBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        String json = this.transport.renderEnvelope(InferenceCapability.PLAN, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();

        // Assert — SIS RequestEnvelope only allows these five keys (extra="forbid")
        Set<String> actualKeys = envelope.keySet();
        assertTrue(actualKeys.contains("protocolVersion"), "protocolVersion must be present");
        assertTrue(actualKeys.contains("requestId"), "requestId must be present");
        assertTrue(actualKeys.contains("capability"), "capability must be present");
        assertTrue(actualKeys.contains("deadlineMillis"), "deadlineMillis must be present");
        assertTrue(actualKeys.contains("payload"), "payload must be present");
        assertEquals(5, actualKeys.size(), "Envelope must have exactly 5 keys");
    }

    @Test
    void envelope_capabilitySerializesAsPlanString() {
        // Arrange
        this.transport = newTransport();
        PlanBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        String json = this.transport.renderEnvelope(InferenceCapability.PLAN, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();

        // Assert — Gson serializes enum by .name(); SIS expects the string "PLAN".
        // Naming lock: the mod's wire value is "PLAN", not "DAYPLAN" — SIS must be aligned to this.
        assertEquals("PLAN", envelope.get("capability").getAsString());
    }

    @Test
    void villager_snapshotIsPresentEvenWhenEmpty() {
        // Arrange
        this.transport = newTransport();
        PlanBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert — SIS requires snapshot even when empty; must serialize as {"sites":{}}
        assertTrue(firstVillager.has("snapshot"), "snapshot must be present — SIS requires it");
        JsonObject snapshot = firstVillager.getAsJsonObject("snapshot");
        assertTrue(snapshot.has("sites"), "sites key must be present inside snapshot");
        assertEquals(0, snapshot.getAsJsonObject("sites").size(), "sites must be empty this phase");
    }

    @Test
    void villager_dayTypeSerializesAsRawString() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("REST_DAY")
                        .build())
                .build();

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert — dayType is a raw string on the wire; SIS validates it, the mod does not
        assertTrue(firstVillager.has("dayType"), "dayType must be present");
        assertEquals("REST_DAY", firstVillager.get("dayType").getAsString());
    }

    @Test
    void villager_optionsCarryIdDescriptionCategoryIntensityAndMinutes() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .option(BehaviorOptionDTO.builder()
                                .id("HARVEST_MELON")
                                .description("Harvest the ripe melons.")
                                .category("WORK")
                                .intensity("MODERATE")
                                .estimatedMinutes(20)
                                .build())
                        .build())
                .build();

        // Act
        JsonArray options = firstVillager(payload).getAsJsonArray("options");

        // Assert
        assertEquals(1, options.size());
        JsonObject entry = options.get(0).getAsJsonObject();
        assertEquals("HARVEST_MELON", entry.get("id").getAsString());
        assertEquals("Harvest the ripe melons.", entry.get("description").getAsString());
        assertEquals("WORK", entry.get("category").getAsString());
        assertEquals("MODERATE", entry.get("intensity").getAsString());
        assertEquals(20, entry.get("estimatedMinutes").getAsInt());
        assertFalse(entry.has("requiredItems"), "requiredItems must be omitted when unset");
        assertFalse(entry.has("produces"), "produces must be omitted when unset");
    }

    @Test
    void villager_optionsIncludeRequiredItemsAndProducesWhenSet() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .option(BehaviorOptionDTO.builder()
                                .id("BAKE_BREAD")
                                .description("Bake a batch of bread.")
                                .requiredItems("wheat")
                                .produces("bread")
                                .category("WORK")
                                .intensity("LIGHT")
                                .estimatedMinutes(10)
                                .build())
                        .build())
                .build();

        // Act
        JsonObject entry = firstVillager(payload).getAsJsonArray("options").get(0).getAsJsonObject();

        // Assert
        assertEquals("wheat", entry.get("requiredItems").getAsString());
        assertEquals("bread", entry.get("produces").getAsString());
    }

    @Test
    void villager_windowsCarryIdAndTickBounds() {
        // startTick/endTick are civil-space ticks (0 = midnight) since the P1c civil-time sweep —
        // this test only locks the wire SHAPE, so the fixture values below are arbitrary ints.
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .window(PlanWindowDTO.builder().id("MORNING").startTick(0).endTick(6000).build())
                        .build())
                .build();

        // Act
        JsonArray windows = firstVillager(payload).getAsJsonArray("windows");

        // Assert
        assertEquals(1, windows.size());
        JsonObject window = windows.get(0).getAsJsonObject();
        assertEquals("MORNING", window.get("id").getAsString());
        assertEquals(0, window.get("startTick").getAsInt());
        assertEquals(6000, window.get("endTick").getAsInt());
    }

    @Test
    void villager_walletAndHungerAreAlwaysPresent() {
        // Arrange
        this.transport = newTransport();
        PlanBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert
        assertTrue(firstVillager.has("wallet"), "wallet must be present");
        assertTrue(firstVillager.has("hunger"), "hunger must be present");
    }

    @Test
    void villager_walletSerializesCurrencyToAmount() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .wallet(Map.of("emeralds", 12))
                        .build())
                .build();

        // Act
        JsonObject wallet = firstVillager(payload).getAsJsonObject("wallet");

        // Assert
        assertEquals(12, wallet.get("emeralds").getAsInt());
    }

    @Test
    void villager_hungerSerializesAsNumber() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .hunger(0.35f)
                        .build())
                .build();

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert
        assertEquals(0.35f, firstVillager.get("hunger").getAsFloat(), 0.0001f);
    }

    @Test
    void villager_demandsPreserveSuppliedOrder() {
        // Arrange — priority 100 supplied before 80; the wire order must reflect caller order,
        // not a re-sort by this DTO (the caller is responsible for priority-sorting).
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .demand(DemandDTO.builder().token("minecraft:bread").shortfall(4).priority(100).build())
                        .demand(DemandDTO.builder().token("c:foods").shortfall(2).priority(80).build())
                        .build())
                .build();

        // Act
        JsonArray demands = firstVillager(payload).getAsJsonArray("demands");

        // Assert
        assertEquals(2, demands.size());
        JsonObject first = demands.get(0).getAsJsonObject();
        assertEquals("minecraft:bread", first.get("token").getAsString());
        assertEquals(4, first.get("shortfall").getAsInt());
        assertEquals(100, first.get("priority").getAsInt());

        JsonObject second = demands.get(1).getAsJsonObject();
        assertEquals("c:foods", second.get("token").getAsString());
        assertEquals(80, second.get("priority").getAsInt());
    }

    @Test
    void villager_surplusCarriesTokenSellableAndDumpable() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .surplus(SurplusDTO.builder().token("minecraft:wheat").sellable(32).dumpable(6).build())
                        .build())
                .build();

        // Act
        JsonArray surplus = firstVillager(payload).getAsJsonArray("surplus");

        // Assert
        assertEquals(1, surplus.size());
        JsonObject entry = surplus.get(0).getAsJsonObject();
        assertEquals("minecraft:wheat", entry.get("token").getAsString());
        assertEquals(32, entry.get("sellable").getAsInt());
        assertEquals(6, entry.get("dumpable").getAsInt());
    }

    @Test
    void villager_episodicEntriesReuseMonologueShape() {
        // Arrange — confirms EpisodicEntryDTO is reused, not re-declared, and serializes identically
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        EpisodicEntryDTO entry = EpisodicEntryDTO.builder()
                .eventType("RESOURCE_HARVESTED")
                .perspective("FIRST_HAND_PARTICIPANT")
                .hop(0)
                .ageTicks(120L)
                .build();
        PlanBatchRequest payload = PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .episodic(entry)
                        .build())
                .build();

        // Act
        JsonArray episodic = firstVillager(payload).getAsJsonArray("episodic");

        // Assert
        assertEquals(1, episodic.size());
        JsonObject episodicJson = episodic.get(0).getAsJsonObject();
        assertEquals("RESOURCE_HARVESTED", episodicJson.get("eventType").getAsString());
        assertEquals("FIRST_HAND_PARTICIPANT", episodicJson.get("perspective").getAsString());
        assertFalse(episodicJson.has("actor"), "null actor must be omitted, matching the MONOLOGUE contract");
    }

    @Test
    void villager_personaReusesMonologueShape() {
        // Arrange — confirms PersonaBundle is reused, not re-declared, and serializes identically
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PlanBatchRequest payload = minimalBatchRequest(villagerId);

        // Act
        JsonObject personaJson = firstVillager(payload).getAsJsonObject("persona");

        // Assert
        assertNotNull(personaJson, "persona must be present");
        assertEquals("minecraft:farmer", personaJson.get("profession").getAsString());
        assertTrue(personaJson.has("anchors"), "anchors must be present");
    }

    @Test
    void response_selectionsAreObjectsWithOptionalAt() {
        // Arrange — response-side shape (SIS -> mod), not the request envelope: `at` present serializes
        // as the literal TimeOfDay enum name, and `at` absent is omitted entirely (matching every
        // other optional field on the PLAN wire, e.g. EpisodicEntryDTO's actor).
        UUID villagerId = UUID.randomUUID();
        VillagerPlanResult result = VillagerPlanResult.builder()
                .villagerId(villagerId)
                .selections(Map.of(
                        "EVENING", List.of(PlanSelection.builder().id("eat_food").at("AT_18_00").build()),
                        "MORNING", List.of(PlanSelection.builder().id("harvest_ripe_crops").build())))
                .build();

        // Act
        JsonObject json = JsonParser.parseString(GSON.toJson(result)).getAsJsonObject();
        JsonObject pinnedSelection = json.getAsJsonObject("selections")
                .getAsJsonArray("EVENING").get(0).getAsJsonObject();
        JsonObject plainSelection = json.getAsJsonObject("selections")
                .getAsJsonArray("MORNING").get(0).getAsJsonObject();

        // Assert
        assertEquals("eat_food", pinnedSelection.get("id").getAsString());
        assertEquals("AT_18_00", pinnedSelection.get("at").getAsString());
        assertEquals("harvest_ripe_crops", plainSelection.get("id").getAsString());
        assertFalse(plainSelection.has("at"), "at must be omitted entirely when unset, not serialized as null");
    }

    @Test
    void response_atDeserializesBackToTheLiteralEnumName() {
        // Arrange — round-trip through Gson the way HttpPlanGateway consumes an NDJSON line
        String line = "{\"villagerId\":\"%s\",\"selections\":{\"EVENING\":[{\"id\":\"eat_food\",\"at\":\"AT_18_00\"}]}}"
                .formatted(UUID.randomUUID());

        // Act
        VillagerPlanResult result = GSON.fromJson(line, VillagerPlanResult.class);

        // Assert
        assertEquals("AT_18_00", result.getSelections().get("EVENING").getFirst().getAt());
    }

    private JsonObject firstVillager(PlanBatchRequest payload) {
        String json = this.transport.renderEnvelope(InferenceCapability.PLAN, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();
        JsonArray villagers = envelope.getAsJsonObject("payload").getAsJsonArray("villagers");
        return villagers.get(0).getAsJsonObject();
    }

    private static PersonaBundle minimalPersona() {
        return PersonaBundle.builder()
                .name("Aldric")
                .profession("minecraft:farmer")
                .anchors(Anchors.builder()
                        .body(new int[]{0, 64, 0})
                        .build())
                .build();
    }

    /**
     * Minimal valid request: body-only anchors, empty snapshot, normal day type, no
     * options/windows, empty wallet. wallet is populated (not left null) here because it is
     * always-present on the wire, same as snapshot — an assembler-supplied field, not an optional
     * one, so the minimal fixture must not rely on Gson's null-omission for it.
     */
    private static PlanBatchRequest minimalBatchRequest(UUID villagerId) {
        return PlanBatchRequest.builder()
                .villager(VillagerPlanRequest.builder()
                        .villagerId(villagerId)
                        .persona(minimalPersona())
                        .snapshot(Snapshot.builder().build())
                        .dayType("NORMAL")
                        .wallet(Map.of())
                        .build())
                .build();
    }

    private static HttpInferenceTransport newTransport() {
        InferenceConfig config = new InferenceConfig("http://localhost:9999", "", "en_us", 50);
        return new HttpInferenceTransport(config);
    }

}
