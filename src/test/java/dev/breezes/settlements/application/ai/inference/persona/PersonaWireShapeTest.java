package dev.breezes.settlements.application.ai.inference.persona;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.genetics.GeneSignal;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.personality.OriginType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the PERSONA request/response wire shape against the SIS contract.
 * <p>
 * All DTOs are constructed by hand — no Minecraft objects, no {@code GeneticsProfile} (which
 * touches {@code Mth}) — matching the {@code MonologueWireShapeTest} approach for MONOLOGUE.
 */
class PersonaWireShapeTest {

    private static final Gson GSON = new Gson();

    @Test
    void request_topLevelKeysMatchSisContract() {
        // Arrange
        PersonaVillagerRequest villager = PersonaVillagerRequest.builder()
                .villagerId(new UUID(1, 2))
                .spawnType(OriginType.BRED)
                .isNitwit(true)
                .geneSignal(new GeneSignal(GeneType.STRENGTH, 0.5))
                .lineage(PersonaLineage.builder()
                        .parentA(PersonaLineageSide.builder().adjective("brave").characterSketchDigest("digestA").build())
                        .parentB(PersonaLineageSide.builder().adjective("kind").characterSketchDigest("digestB").build())
                        .build())
                .build();
        PersonaBatchRequest request = PersonaBatchRequest.builder()
                .villager(villager)
                .build();

        // Act
        String json = GSON.toJson(request);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        // Assert — top level
        assertTrue(root.has("villagers"), "villagers must be present");
        JsonObject firstVillager = root.getAsJsonArray("villagers").get(0).getAsJsonObject();

        // Assert — per-villager keys, byte-identical to the SIS contract
        assertTrue(firstVillager.has("villagerId"), "villagerId must be present");
        assertEquals(new UUID(1, 2).toString(), firstVillager.get("villagerId").getAsString());

        assertTrue(firstVillager.has("spawnType"), "spawnType must be present");
        assertEquals("BRED", firstVillager.get("spawnType").getAsString(), "spawnType must serialize as the OriginType enum name");

        assertTrue(firstVillager.has("isNitwit"), "isNitwit must be present under that exact field name");
        assertTrue(firstVillager.get("isNitwit").getAsBoolean());

        assertTrue(firstVillager.has("geneSignals"), "geneSignals must be present");
        JsonArray geneSignals = firstVillager.getAsJsonArray("geneSignals");
        assertEquals(1, geneSignals.size());
        JsonObject signal = geneSignals.get(0).getAsJsonObject();
        assertEquals("STRENGTH", signal.get("dimension").getAsString());
        assertEquals(0.5, signal.get("value").getAsDouble());
        assertFalse(signal.has("rarity"), "rarity was dropped — the raw value is the whole gene signal");

        assertTrue(firstVillager.has("lineage"), "lineage must be present when set");
        JsonObject lineage = firstVillager.getAsJsonObject("lineage");
        assertTrue(lineage.has("parentA"), "parentA must be present");
        assertTrue(lineage.has("parentB"), "parentB must be present");
        assertEquals("digestA", lineage.getAsJsonObject("parentA").get("characterSketchDigest").getAsString());
        assertEquals("brave", lineage.getAsJsonObject("parentA").getAsJsonArray("adjectives").get(0).getAsString());
    }

    @Test
    void request_lineageIsOmittedWhenNull() {
        // Arrange — the common case this wave always produces: no lineage capture yet.
        PersonaVillagerRequest villager = PersonaVillagerRequest.builder()
                .villagerId(UUID.randomUUID())
                .spawnType(OriginType.WORLDGEN)
                .isNitwit(false)
                .build();
        PersonaBatchRequest request = PersonaBatchRequest.builder().villager(villager).build();

        // Act
        String json = GSON.toJson(request);
        JsonObject firstVillager = JsonParser.parseString(json).getAsJsonObject()
                .getAsJsonArray("villagers").get(0).getAsJsonObject();

        // Assert — Gson's default null-omission means a null lineage simply does not appear.
        assertFalse(firstVillager.has("lineage"), "lineage must be omitted, not sent as null");
    }

    @Test
    void response_parsesStreamedResultLineWithNullSpeechStyle() {
        // Arrange — sample NDJSON line as SIS would emit it.
        UUID villagerId = UUID.randomUUID();
        String line = "{\"villagerId\":\"" + villagerId + "\",\"adjectives\":[\"a\",\"b\",\"c\"],\"characterSketch\":\"x\",\"speechStyle\":null}";

        // Act
        PersonaVillagerResult result = GSON.fromJson(line, PersonaVillagerResult.class);

        // Assert
        assertEquals(villagerId, result.getVillagerId());
        assertEquals(3, result.getAdjectives().size());
        assertEquals("x", result.getCharacterSketch());
        assertNull(result.getSpeechStyle(), "speechStyle must tolerate null");
    }

}
