package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link SeedPhrasebook}.
 * Verifies clause construction for each known {@link WorldEventType}, the detail seam,
 * the null-event-type fallback, and the {@link ObservationType} coarse fallback.
 * No Minecraft types are used.
 */
class SeedPhrasebookTest {

    private static final String ACTOR = "Aldric";
    private static final String TARGET = "Beatrix";

    // -------------------------------------------------------------------------
    // Null event type — should never throw
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_nullEventType_returnsFallbackPhrase() {
        // Arrange — null simulates an unknown future type

        // Act
        String clause = SeedPhrasebook.phraseClause(null, ACTOR, TARGET, null);

        // Assert
        assertNotNull(clause);
        assertFalse(clause.isBlank());
        assertEquals(ACTOR + " did something", clause);
    }

    // -------------------------------------------------------------------------
    // Completions with target
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_tradeCompleted_withTarget_noDetail() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, null);

        // Assert
        assertEquals(ACTOR + " traded with " + TARGET, clause);
    }

    @Test
    void phraseClause_courtshipCompleted_withTarget_noDetail() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.COURTSHIP_COMPLETED, ACTOR, TARGET, null);

        // Assert
        assertEquals(ACTOR + " courted " + TARGET, clause);
    }

    // -------------------------------------------------------------------------
    // Completions without target
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_tradeCompleted_nullTarget_usesSomeoneFallback() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TRADE_COMPLETED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " traded with someone", clause);
    }

    @Test
    void phraseClause_sheepSheared_noTargetNoDetail() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.SHEEP_SHEARED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " sheared a sheep", clause);
    }

    @Test
    void phraseClause_sheepDyed_noTargetNoDetail() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.SHEEP_DYED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " dyed a sheep", clause);
    }

    @Test
    void phraseClause_cropHarvested_noTargetNoDetail() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.CROP_HARVESTED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " harvested crops", clause);
    }

    @Test
    void phraseClause_tipConfirmed() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TIP_CONFIRMED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " confirmed a rumour", clause);
    }

    @Test
    void phraseClause_tipRefuted() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TIP_REFUTED, ACTOR, null, null);

        // Assert
        assertEquals(ACTOR + " disproved a rumour", clause);
    }

    // -------------------------------------------------------------------------
    // Detail seam
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_cropHarvested_withDetail_overridesGenericObject() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.CROP_HARVESTED, ACTOR, null, "3 melons");

        // Assert — "crops" replaced by the supplied detail
        assertEquals(ACTOR + " harvested 3 melons", clause);
    }

    @Test
    void phraseClause_tradeCompleted_withDetailAndTarget_rendersItemsWithPartner() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, "4 bread for 1 emerald");

        // Assert — items and partner both render: "traded <items> with <partner>"
        assertEquals(ACTOR + " traded 4 bread for 1 emerald with " + TARGET, clause);
    }

    @Test
    void phraseClause_tradeCompleted_withDetail_noTarget_rendersItemsOnly() {
        // Arrange + Act — partner unknown but items present: no "with someone" filler
        String clause = SeedPhrasebook.phraseClause(WorldEventType.TRADE_COMPLETED, ACTOR, null, "4 bread for 1 emerald");

        // Assert
        assertEquals(ACTOR + " traded 4 bread for 1 emerald", clause);
    }

    @Test
    void phraseClause_sheepSheared_withDetail_overridesGenericObject() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.SHEEP_SHEARED, ACTOR, null, "3 wool");

        // Assert
        assertEquals(ACTOR + " sheared 3 wool", clause);
    }

    @Test
    void phraseClause_sheepDyed_withDetail_overridesGenericObject() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.SHEEP_DYED, ACTOR, null, "3 sheep");

        // Assert
        assertEquals(ACTOR + " dyed 3 sheep", clause);
    }

    // -------------------------------------------------------------------------
    // Lifecycle / invite types — phrasebook handles defensively but won't be hit normally
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_inviteAndLifecycleTypes_returnNonNullNonBlankPhrase() {
        // Arrange — invites and starts are blocked upstream by the projector allowlist; the
        // phrasebook returns something defensively rather than throwing.
        WorldEventType[] defensiveTypes = {
                WorldEventType.TRADE_INVITE_SENT,
                WorldEventType.COURTSHIP_INVITE_SENT,
                WorldEventType.BEHAVIOR_STARTED,
                WorldEventType.BEHAVIOR_COMPLETED,
                WorldEventType.BEHAVIOR_FAILED,
        };

        for (WorldEventType type : defensiveTypes) {
            // Act
            String clause = SeedPhrasebook.phraseClause(type, ACTOR, TARGET, null);

            // Assert — must not be null or blank
            assertNotNull(clause, "Expected non-null for type: " + type);
            assertFalse(clause.isBlank(), "Expected non-blank for type: " + type);
        }
    }

    @Test
    void phraseClause_behaviorCompleted_withBehaviorMetadata_rendersReadableTask() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.BEHAVIOR_COMPLETED, ACTOR, null, null,
                null, null, "wash_leather");

        // Assert
        assertEquals(ACTOR + " finished wash leather", clause);
    }

    @Test
    void phraseClause_behaviorFailed_withBehaviorMetadataAndReason_rendersTaskAttempt() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.BEHAVIOR_FAILED, ACTOR, null, null,
                EventOutcome.FAILURE, "continue condition failed", "harvest_pumpkin");

        // Assert
        assertEquals(ACTOR + " tried to harvest pumpkin but continue condition failed", clause);
    }

    @Test
    void phraseClause_behaviorFailed_withoutReason_usesGenericFallback() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.BEHAVIOR_FAILED, ACTOR, null, null,
                EventOutcome.FAILURE, null, "harvest_pumpkin");

        // Assert
        assertEquals(ACTOR + " tried to harvest pumpkin but it fell through", clause);
    }

    @Test
    void phraseClause_cropHarvested_withZeroDetail_remainsBenignDeedSignal() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(WorldEventType.CROP_HARVESTED, ACTOR, null, "0 pumpkins");

        // Assert
        assertEquals(ACTOR + " harvested 0 pumpkins", clause);
    }

    // -------------------------------------------------------------------------
    // WS3a: FAILURE outcome — "tried to ... but ..." phrasing
    // -------------------------------------------------------------------------

    @Test
    void phraseClause_tradeCompleted_failure_withTargetAndReason() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, null,
                EventOutcome.FAILURE, "haggling fell through");

        // Assert
        assertEquals(ACTOR + " tried to trade with " + TARGET + " but haggling fell through", clause);
    }

    @Test
    void phraseClause_tradeCompleted_failure_noTarget_withReason() {
        // Arrange + Act — no partner known
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.TRADE_COMPLETED, ACTOR, null, null,
                EventOutcome.FAILURE, "price disagreement");

        // Assert — no "with someone" filler on failure path
        assertEquals(ACTOR + " tried to trade but price disagreement", clause);
    }

    @Test
    void phraseClause_tradeCompleted_failure_noReason_usesGenericFallback() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, null,
                EventOutcome.FAILURE, null);

        // Assert — generic fallback when reason is absent
        assertEquals(ACTOR + " tried to trade with " + TARGET + " but it fell through", clause);
    }

    @Test
    void phraseClause_courtshipCompleted_failure_withTargetAndReason() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.COURTSHIP_COMPLETED, ACTOR, TARGET, null,
                EventOutcome.FAILURE, "no bed available");

        // Assert
        assertEquals(ACTOR + " tried to court " + TARGET + " but no bed available", clause);
    }

    @Test
    void phraseClause_courtshipCompleted_failure_noTarget_withReason() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.COURTSHIP_COMPLETED, ACTOR, null, null,
                EventOutcome.FAILURE, "no one responded");

        // Assert
        assertEquals(ACTOR + " tried to court someone but no one responded", clause);
    }

    @Test
    void phraseClause_courtshipCompleted_failure_noReason_usesGenericFallback() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.COURTSHIP_COMPLETED, ACTOR, TARGET, null,
                EventOutcome.FAILURE, null);

        // Assert
        assertEquals(ACTOR + " tried to court " + TARGET + " but it fell through", clause);
    }

    @Test
    void phraseClause_successOutcome_rendersExactlyAsNullOutcome() {
        // Arrange — SUCCESS and absent outcome must produce identical output (regression guard)
        String successClause = SeedPhrasebook.phraseClause(
                WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, null,
                EventOutcome.SUCCESS, null);
        String nullOutcomeClause = SeedPhrasebook.phraseClause(
                WorldEventType.TRADE_COMPLETED, ACTOR, TARGET, null);

        // Assert — no behavioral difference between explicit SUCCESS and absent outcome
        assertEquals(nullOutcomeClause, successClause);
    }

    @Test
    void phraseClause_cropHarvested_failure_rendersSuccessForm() {
        // Arrange — resource events have no failure variant; FAILURE falls through to success form
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.CROP_HARVESTED, ACTOR, null, null,
                EventOutcome.FAILURE, "some reason");

        // Assert — success form, no crash
        assertEquals(ACTOR + " harvested crops", clause);
    }

    @Test
    void phraseClause_sheepSheared_failure_rendersSuccessForm() {
        // Arrange
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.SHEEP_SHEARED, ACTOR, null, null,
                EventOutcome.FAILURE, "some reason");

        // Assert
        assertEquals(ACTOR + " sheared a sheep", clause);
    }

    @Test
    void phraseClause_sheepDyed_failure_rendersSuccessForm() {
        // Arrange
        String clause = SeedPhrasebook.phraseClause(
                WorldEventType.SHEEP_DYED, ACTOR, null, null,
                EventOutcome.FAILURE, "some reason");

        // Assert
        assertEquals(ACTOR + " dyed a sheep", clause);
    }

    // -------------------------------------------------------------------------
    // ObservationType fallback
    // -------------------------------------------------------------------------

    @Test
    void phraseFallback_social_includesActorName() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseFallback(ACTOR, ObservationType.SOCIAL);

        // Assert
        assertEquals(ACTOR + " interacted with someone", clause);
    }

    @Test
    void phraseFallback_resource_includesActorName() {
        // Arrange + Act
        String clause = SeedPhrasebook.phraseFallback(ACTOR, ObservationType.RESOURCE);

        // Assert
        assertEquals(ACTOR + " gathered resources", clause);
    }

    @Test
    void phraseFallback_allTypes_returnNonNullNonBlank() {
        // Arrange + Act + Assert — exhaustive check that no ObservationType throws
        for (ObservationType type : ObservationType.values()) {
            String clause = SeedPhrasebook.phraseFallback(ACTOR, type);
            assertNotNull(clause, "Expected non-null for ObservationType: " + type);
            assertFalse(clause.isBlank(), "Expected non-blank for ObservationType: " + type);
        }
    }

}
