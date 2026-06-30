package dev.breezes.settlements.application.ai.inference.monologue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.HttpInferenceTransport;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Phase 0 wire shape of the MONOLOGUE envelope against the SIS contract.
 * <p>
 * All DTOs are constructed by hand — no Minecraft objects. The assertions mirror SIS's
 * extra="forbid" validation: any unknown key here would produce a 422 in SIS.
 */
class MonologueWireShapeTest {

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
        MonologueBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        String json = this.transport.renderEnvelope(InferenceCapability.MONOLOGUE, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();

        // Assert — SIS RequestEnvelope only allows these five keys (extra="forbid")
        Set<String> actualKeys = envelope.keySet();
        assertTrue(actualKeys.contains("protocolVersion"), "protocolVersion must be present");
        assertTrue(actualKeys.contains("requestId"), "requestId must be present");
        assertTrue(actualKeys.contains("capability"), "capability must be present");
        assertTrue(actualKeys.contains("deadlineMillis"), "deadlineMillis must be present");
        assertTrue(actualKeys.contains("payload"), "payload must be present");
        assertFalse(actualKeys.contains("deadlineSlackMillis"),
                "deadlineSlackMillis must be absent — SIS extra=forbid rejects it");
        assertEquals(5, actualKeys.size(), "Envelope must have exactly 5 keys");
    }

    @Test
    void envelope_capabilitySerializesAsMonologueString() {
        // Arrange
        this.transport = newTransport();
        MonologueBatchRequest payload = minimalBatchRequest(UUID.randomUUID());

        // Act
        String json = this.transport.renderEnvelope(InferenceCapability.MONOLOGUE, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();

        // Assert — Gson serializes enum by .name(); SIS expects the string "MONOLOGUE"
        assertEquals("MONOLOGUE", envelope.get("capability").getAsString());
    }

    @Test
    void persona_professionAndFacetsAndAnchorsBodyArePresent() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PersonaBundle persona = PersonaBundle.builder()
                .name("Aldric")
                .profession("minecraft:farmer")
                .facet(DialogueFacet.WAS_CURED)
                .anchors(Anchors.builder()
                        .body(new int[]{10, 64, 20})
                        .build())
                .build();
        MonologueBatchRequest payload = batchRequestWithPersona(villagerId, persona);

        // Act
        JsonObject firstVillager = firstVillager(payload);
        JsonObject personaJson = firstVillager.getAsJsonObject("persona");

        // Assert — profession (not professionKey), facets nested under persona, anchors present
        assertNotNull(personaJson, "persona must be present");
        assertFalse(personaJson.has("professionKey"), "professionKey must not appear — wire key is 'profession'");
        assertTrue(personaJson.has("profession"), "profession must be present");
        assertEquals("minecraft:farmer", personaJson.get("profession").getAsString());

        // Facets must be a JSON array of enum-name strings
        assertTrue(personaJson.has("facets"), "facets must be nested under persona");
        JsonArray facetsArray = personaJson.getAsJsonArray("facets");
        assertEquals(1, facetsArray.size());
        assertEquals("WAS_CURED", facetsArray.get(0).getAsString(),
                "Facet must serialize as enum .name(), not as an object");

        // Anchors body must be a 3-element int array
        assertTrue(personaJson.has("anchors"), "anchors must be present");
        JsonArray body = personaJson.getAsJsonObject("anchors").getAsJsonArray("body");
        assertEquals(3, body.size(), "body must be [x,y,z]");
        assertEquals(10, body.get(0).getAsInt());
        assertEquals(64, body.get(1).getAsInt());
        assertEquals(20, body.get(2).getAsInt());
    }

    @Test
    void anchors_nullJobSiteAndHomeAreOmittedFromWire() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PersonaBundle persona = PersonaBundle.builder()
                .name("Marta")
                .profession("minecraft:none")
                // jobSite and home deliberately absent — villager has no job site / bed
                .anchors(Anchors.builder()
                        .body(new int[]{5, 63, 7})
                        .build())
                .build();
        MonologueBatchRequest payload = batchRequestWithPersona(villagerId, persona);

        // Act
        JsonObject anchorsJson = firstVillager(payload)
                .getAsJsonObject("persona")
                .getAsJsonObject("anchors");

        // Assert — Gson default omits null fields; absent anchor positions must not appear
        assertFalse(anchorsJson.has("jobSite"), "jobSite must be omitted when null — SIS expects absence, not null");
        assertFalse(anchorsJson.has("home"), "home must be omitted when null — SIS expects absence, not null");
        assertTrue(anchorsJson.has("body"), "body must always be present");
    }

    @Test
    void villager_snapshotIsPresentEvenWhenEmpty() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        MonologueBatchRequest payload = minimalBatchRequest(villagerId);

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert — SIS requires snapshot even when empty; must serialize as {"sites":{}}
        assertTrue(firstVillager.has("snapshot"), "snapshot must be present — SIS requires it");
        JsonObject snapshot = firstVillager.getAsJsonObject("snapshot");
        assertTrue(snapshot.has("sites"), "sites key must be present inside snapshot");
        assertEquals(0, snapshot.getAsJsonObject("sites").size(), "sites must be empty this phase");
    }

    @Test
    void villager_populatedSnapshotSerializesTokenKeyedCoordArrays() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        PersonaBundle persona = PersonaBundle.builder()
                .name("Aldric")
                .profession("minecraft:farmer")
                .anchors(Anchors.builder()
                        .body(new int[]{0, 64, 0})
                        .build())
                .build();
        Snapshot snapshot = Snapshot.builder()
                .site("RIPE_MELON", List.of(new int[]{20, 60, 22}))
                .build();
        MonologueBatchRequest payload = MonologueBatchRequest.builder()
                .locale("en_us")
                .villager(VillagerMonologueRequest.builder()
                        .villagerId(villagerId)
                        .persona(persona)
                        .snapshot(snapshot)
                        .bucket(OccasionBucketSpec.builder()
                                .occasion(Occasion.IDLE)
                                .lineCount(8)
                                .build())
                        .build())
                .build();

        // Act
        JsonObject sites = firstVillager(payload)
                .getAsJsonObject("snapshot")
                .getAsJsonObject("sites");

        // Assert — sites.RIPE_MELON = [[20,60,22]]
        assertTrue(sites.has("RIPE_MELON"), "wire token RIPE_MELON must key the coord list");
        JsonArray melon = sites.getAsJsonArray("RIPE_MELON");
        assertEquals(1, melon.size());
        JsonArray coord = melon.get(0).getAsJsonArray();
        assertEquals(3, coord.size(), "each coord must be [x,y,z]");
        assertEquals(20, coord.get(0).getAsInt());
        assertEquals(60, coord.get(1).getAsInt());
        assertEquals(22, coord.get(2).getAsInt());
    }

    @Test
    void villager_noSeedsAndNoTopLevelFacets() {
        // Arrange
        this.transport = newTransport();
        UUID villagerId = UUID.randomUUID();
        MonologueBatchRequest payload = minimalBatchRequest(villagerId);

        // Act
        JsonObject firstVillager = firstVillager(payload);

        // Assert — seeds and top-level facets are gone; SIS extra=forbid would 422 them
        assertFalse(firstVillager.has("seeds"), "seeds must not appear — phrasing now lives in SIS");
        assertFalse(firstVillager.has("facets"), "top-level facets must not appear — they moved under persona");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JsonObject firstVillager(MonologueBatchRequest payload) {
        String json = this.transport.renderEnvelope(InferenceCapability.MONOLOGUE, payload, Duration.ofSeconds(30));
        JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();
        JsonArray villagers = envelope.getAsJsonObject("payload").getAsJsonArray("villagers");
        return villagers.get(0).getAsJsonObject();
    }

    /**
     * Minimal valid request: body-only anchors, empty snapshot, one bucket.
     */
    private static MonologueBatchRequest minimalBatchRequest(UUID villagerId) {
        PersonaBundle persona = PersonaBundle.builder()
                .name("Aldric")
                .profession("minecraft:farmer")
                .anchors(Anchors.builder()
                        .body(new int[]{0, 64, 0})
                        .build())
                .build();
        return batchRequestWithPersona(villagerId, persona);
    }

    private static MonologueBatchRequest batchRequestWithPersona(UUID villagerId, PersonaBundle persona) {
        return MonologueBatchRequest.builder()
                .locale("en_us")
                .villager(VillagerMonologueRequest.builder()
                        .villagerId(villagerId)
                        .persona(persona)
                        .snapshot(Snapshot.builder().build())
                        .bucket(OccasionBucketSpec.builder()
                                .occasion(Occasion.IDLE)
                                .lineCount(8)
                                .build())
                        .build())
                .build();
    }

    private static HttpInferenceTransport newTransport() {
        InferenceConfig config = new InferenceConfig("http://localhost:9999", "", "en_us", 50);
        return new HttpInferenceTransport(config);
    }

}
