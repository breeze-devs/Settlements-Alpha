package dev.breezes.settlements.application.ai.behavior.publication;

import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the yield→structured detail auto-derivation logic in
 * {@link BehaviorOutcomePublisher#buildDetailFields}.
 * <p>
 * Pure Java — no Minecraft objects involved.
 */
class BehaviorOutcomePublisherDetailTest {

    @Test
    void buildDetailFields_magnitudeAndUnitNoun_returnsItemAndCount() {
        // Arrange — a harvest deed with 3 melons
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "melons");
        outcome.recordYield(3);

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert — auto-derived from magnitude + unitNoun
        assertEquals("melons", result.get("item"));
        assertEquals("3", result.get("count"));
    }

    @Test
    void buildDetailFields_zeroMagnitudeWithUnitNoun_emitsCountZero() {
        // Arrange — a harvest deed that completed but yielded nothing (an empty harvest)
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, "wheat");

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert — a unit noun marks this as a yield deed, so count 0 is meaningful, not absent:
        // it lets SIS narrate the empty-handed outcome ("tried to harvest, came back with nothing").
        assertEquals("wheat", result.get("item"));
        assertEquals("0", result.get("count"));
    }

    @Test
    void buildDetailFields_magnitudeWithoutUnitNoun_noAutoDetail() {
        // Arrange — magnitude set but no unitNoun (atypical, but valid)
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.RESOURCE_HARVESTED, null);
        outcome.recordYield(5);

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert — auto-derive requires both magnitude and unitNoun
        assertNull(result);
    }

    @Test
    void buildDetailFields_explicitFieldsOnly_returnsExplicitFields() {
        // Arrange — a social deed with no yield but with explicit trade detail
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TRADE_COMPLETED, null);
        outcome.putDetailField("item", "bread");
        outcome.putDetailField("count", "4");
        outcome.putDetailField("price", "1 emerald");

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert
        assertEquals("bread", result.get("item"));
        assertEquals("4", result.get("count"));
        assertEquals("1 emerald", result.get("price"));
    }

    @Test
    void buildDetailFields_explicitFieldsOverrideAutoDerive() {
        // Arrange — yield deed with explicit item override (e.g. richer label than unitNoun)
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.SHEEP_SHEARED, "wool");
        outcome.recordYield(2);
        // Explicit "item" should override the auto-derived "wool"
        outcome.putDetailField("item", "blue wool");

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert — explicit wins for "item"; "count" is still auto-derived
        assertEquals("blue wool", result.get("item"));
        assertEquals("2", result.get("count"));
    }

    @Test
    void buildDetailFields_noMagnitudeNoExplicit_returnsNull() {
        // Arrange — blank deed with nothing recorded
        BehaviorOutcome outcome = BehaviorOutcome.blank();

        // Act
        Map<String, String> result = BehaviorOutcomePublisher.buildDetailFields(outcome);

        // Assert — null rather than empty map so Gson omits the field on the wire
        assertNull(result);
    }

}
