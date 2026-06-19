package dev.breezes.settlements.domain.ai.worldevent;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link WorldEvent} builder — verifies that the three new WS3a fields
 * (outcome, reason, detail) are carried correctly and that events built without them
 * leave them null.
 * No Minecraft types are used.
 */
class WorldEventTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();

    // -------------------------------------------------------------------------
    // New fields round-trip through builder
    // -------------------------------------------------------------------------

    @Test
    void builder_carryOutcome() {
        // Arrange + Act
        WorldEvent event = minimalEvent()
                .outcome(EventOutcome.SUCCESS)
                .build();

        // Assert
        assertEquals(EventOutcome.SUCCESS, event.getOutcome());
    }

    @Test
    void builder_carryFailureOutcome() {
        // Arrange + Act
        WorldEvent event = minimalEvent()
                .outcome(EventOutcome.FAILURE)
                .build();

        // Assert
        assertEquals(EventOutcome.FAILURE, event.getOutcome());
    }

    @Test
    void builder_carryReason() {
        // Arrange + Act
        WorldEvent event = minimalEvent()
                .reason("no bed available")
                .build();

        // Assert
        assertEquals("no bed available", event.getReason());
    }

    @Test
    void builder_carryDetail() {
        // Arrange + Act
        WorldEvent event = minimalEvent()
                .detail("3 melons")
                .build();

        // Assert
        assertEquals("3 melons", event.getDetail());
    }

    @Test
    void builder_allThreeFieldsTogetherRoundTrip() {
        // Arrange + Act
        WorldEvent event = minimalEvent()
                .outcome(EventOutcome.FAILURE)
                .reason("haggling fell through")
                .detail("4 bread for 1 emerald")
                .build();

        // Assert
        assertEquals(EventOutcome.FAILURE, event.getOutcome());
        assertEquals("haggling fell through", event.getReason());
        assertEquals("4 bread for 1 emerald", event.getDetail());
    }

    // -------------------------------------------------------------------------
    // Absent new fields leave nulls — existing events are unaffected
    // -------------------------------------------------------------------------

    @Test
    void builder_outcomeAbsent_isNull() {
        // Arrange + Act — no outcome set
        WorldEvent event = minimalEvent().build();

        // Assert — must be null so existing events remain unaffected downstream
        assertNull(event.getOutcome());
    }

    @Test
    void builder_reasonAbsent_isNull() {
        // Arrange + Act
        WorldEvent event = minimalEvent().build();

        // Assert
        assertNull(event.getReason());
    }

    @Test
    void builder_detailAbsent_isNull() {
        // Arrange + Act
        WorldEvent event = minimalEvent().build();

        // Assert
        assertNull(event.getDetail());
    }

    @Test
    void builder_existingMetadataFieldUnaffectedByNewFields() {
        // Arrange — existing metadata field must be completely unaffected
        WorldEvent event = minimalEvent()
                .metadata("harvest_melon")
                .outcome(EventOutcome.SUCCESS)
                .build();

        // Assert
        assertEquals("harvest_melon", event.getMetadata());
        assertEquals(EventOutcome.SUCCESS, event.getOutcome());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Returns a builder pre-seeded with the mandatory fields so individual tests
     * only have to set the field(s) under test.
     */
    private static WorldEvent.WorldEventBuilder minimalEvent() {
        return WorldEvent.builder()
                .sequence(1L)
                .gameTick(100L)
                .type(WorldEventType.CROP_HARVESTED)
                .actorId(ACTOR_ID)
                .posX(0).posY(64).posZ(0)
                .chunkX(0).chunkZ(0);
    }

}
