package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.naming.VillagerNameResolver;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MonologueSeedProjector}.
 * <p>
 * All seeds are flat third-person clauses; no first-person/perspective encoding.
 * Covers: flat rendering for all observer positions, hearsay prefix, directionality
 * correctness, completions-only allowlist, dedup keeping max weight, weight ordering,
 * cap, unknown-type fallback, and no raw UUIDs in output.
 * <p>
 * No Minecraft types are used.
 */
class MonologueSeedProjectorTest {

    private static final UUID OBSERVER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TARGET_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID SOURCE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    private VillagerNameResolver nameResolver;
    private MonologueSeedProjector projector;
    private VillagerKnowledgeStore store;

    @BeforeEach
    void setUp() {
        this.nameResolver = new VillagerNameResolver();
        this.projector = new MonologueSeedProjector(this.nameResolver);
        this.store = new VillagerKnowledgeStore();
    }

    // -------------------------------------------------------------------------
    // Empty store
    // -------------------------------------------------------------------------

    @Test
    void project_returnsEmptyListForEmptyStore() {
        // Arrange — store is empty

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert
        assertTrue(seeds.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Flat third-person rendering — same clause regardless of observer position
    // -------------------------------------------------------------------------

    @Test
    void project_bystander_rendersFlatThirdPerson() {
        // Arrange — observer is not actor or target
        KnowledgeEntry entry = directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 3.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — flat third-person, no "I saw", no "I traded"
        assertEquals(1, seeds.size());
        assertEquals(actorName + " traded with " + targetName, seeds.get(0));
    }

    @Test
    void project_observerIsActor_sameClauseAsBystander() {
        // Arrange — observer is the actor; should produce identical clause to a bystander view
        KnowledgeEntry entry = directEntry(OBSERVER_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 3.0f);
        this.store.admit(entry);

        String observerName = this.nameResolver.resolve(OBSERVER_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — flat third-person using the observer's own name as actor, not "I"
        assertEquals(1, seeds.size());
        assertEquals(observerName + " traded with " + targetName, seeds.get(0));
    }

    @Test
    void project_observerIsTarget_sameClauseAsBystander() {
        // Arrange — observer is the target; should produce identical clause to a bystander view
        KnowledgeEntry entry = directEntry(ACTOR_ID, OBSERVER_ID, WorldEventType.TRADE_COMPLETED, 3.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String observerName = this.nameResolver.resolve(OBSERVER_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — actor in actor slot, observer in target slot — no perspective flip
        assertEquals(1, seeds.size());
        assertEquals(actorName + " traded with " + observerName, seeds.get(0));
    }

    // -------------------------------------------------------------------------
    // Directionality correctness
    // -------------------------------------------------------------------------

    @Test
    void project_directionalEvent_actorAndTargetInCorrectSlots() {
        // Arrange — COURTSHIP_COMPLETED: actor X courted target Y
        KnowledgeEntry entry = directEntry(ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_COMPLETED, 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — actor initiated, target received; must render in that order
        assertEquals(1, seeds.size());
        assertEquals(actorName + " courted " + targetName, seeds.get(0));
    }

    @Test
    void project_directionalEvent_reversedActorTarget_distinctOutput() {
        // Arrange — same event type but actor/target swapped; should produce different clause
        KnowledgeEntry entry = directEntry(TARGET_ID, ACTOR_ID, WorldEventType.COURTSHIP_COMPLETED, 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — direction is reversed; clause must reflect the actual actor
        assertEquals(1, seeds.size());
        assertEquals(targetName + " courted " + actorName, seeds.get(0));
    }

    // -------------------------------------------------------------------------
    // Hearsay perspective (hop > 0)
    // -------------------------------------------------------------------------

    @Test
    void project_hearsay_prefixesWithSourceNameSays() {
        // Arrange — a hearsay trade entry
        KnowledgeEntry entry = hearsayEntry(SOURCE_ID, ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 3.0f);
        this.store.admit(entry);

        String sourceName = this.nameResolver.resolve(SOURCE_ID);
        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — "{source} says {actor} traded with {target}"
        assertEquals(1, seeds.size());
        assertEquals(sourceName + " says " + actorName + " traded with " + targetName, seeds.get(0));
    }

    @Test
    void project_hearsay_withNullTarget_stillRenders() {
        // Arrange — hearsay with no target
        KnowledgeEntry entry = hearsayEntry(SOURCE_ID, ACTOR_ID, null, WorldEventType.CROP_HARVESTED, 2.0f);
        this.store.admit(entry);

        String sourceName = this.nameResolver.resolve(SOURCE_ID);
        String actorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — target slot falls back to generic; no crash
        assertEquals(1, seeds.size());
        assertEquals(sourceName + " says " + actorName + " harvested crops", seeds.get(0));
    }

    // -------------------------------------------------------------------------
    // Completions-only allowlist
    // -------------------------------------------------------------------------

    @Test
    void project_inviteEvents_producedNoSeeds() {
        // Arrange — invite events should be filtered out entirely
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_INVITE_SENT, 5.0f));

        VillagerKnowledgeStore store2 = new VillagerKnowledgeStore();
        store2.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_INVITE_SENT, 5.0f));

        // Act
        List<String> seeds1 = this.projector.project(OBSERVER_ID, this.store);
        List<String> seeds2 = this.projector.project(OBSERVER_ID, store2);

        // Assert — no seeds; invites are lifecycle noise
        assertTrue(seeds1.isEmpty(), "TRADE_INVITE_SENT should produce no seeds");
        assertTrue(seeds2.isEmpty(), "COURTSHIP_INVITE_SENT should produce no seeds");
    }

    @Test
    void project_behaviorStarted_producesNoSeeds() {
        // Arrange — behavior start is infrastructure noise
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.BEHAVIOR_STARTED, 5.0f));

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert
        assertTrue(seeds.isEmpty(), "BEHAVIOR_STARTED should produce no seeds");
    }

    @Test
    void project_behaviorCompleted_producesNoSeeds() {
        // Arrange — generic lifecycle completion is low-signal noise, not seed-worthy
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.BEHAVIOR_COMPLETED, 5.0f));

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert
        assertTrue(seeds.isEmpty(), "BEHAVIOR_COMPLETED should produce no seeds");
    }

    @Test
    void project_completionEvents_allProduceSeeds() {
        // Arrange — every type marked isSeedWorthy() should produce at least one seed;
        // this replaces the former hardcoded SEED_WORTHY_TYPES allowlist check.
        List<WorldEventType> seedWorthyTypes = Arrays.stream(WorldEventType.values())
                .filter(WorldEventType::isSeedWorthy)
                .toList();

        for (WorldEventType type : seedWorthyTypes) {
            VillagerKnowledgeStore localStore = new VillagerKnowledgeStore();
            localStore.admit(directEntry(ACTOR_ID, TARGET_ID, type, 1.0f));

            // Act + Assert
            List<String> seeds = this.projector.project(OBSERVER_ID, localStore);
            assertFalse(seeds.isEmpty(), "Expected a seed for seed-worthy type: " + type);
        }
    }

    // -------------------------------------------------------------------------
    // Unknown event type fallback
    // -------------------------------------------------------------------------

    @Test
    void project_unknownEventType_fallsBackToObservationTypePhrasing_neverThrows() {
        // Arrange — entry with no event_type in metadata (simulates an unknown future type)
        KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "raw content",
                ObservationType.SOCIAL,
                100L, 100L,
                TARGET_ID,
                Map.of("actor_id", ACTOR_ID.toString()),
                2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — falls back to ObservationType-level phrasing; must include actor name; no crash
        assertEquals(1, seeds.size());
        assertTrue(seeds.get(0).contains(actorName),
                "Expected actor name in fallback seed: " + seeds.get(0));
    }

    // -------------------------------------------------------------------------
    // Detail seam
    // -------------------------------------------------------------------------

    @Test
    void project_withDetail_overridesGenericObjectInClause() {
        // Arrange — CROP_HARVESTED entry with a detail value
        KnowledgeEntry entry = directEntryWithDetail(
                ACTOR_ID, null, WorldEventType.CROP_HARVESTED, "3 melons", 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — generic "crops" is replaced by the detail string
        assertEquals(1, seeds.size());
        assertEquals(actorName + " harvested 3 melons", seeds.get(0));
    }

    @Test
    void project_withoutDetail_usesGenericObject() {
        // Arrange — CROP_HARVESTED entry without detail
        KnowledgeEntry entry = directEntry(ACTOR_ID, null, WorldEventType.CROP_HARVESTED, 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — generic fallback object used
        assertEquals(1, seeds.size());
        assertEquals(actorName + " harvested crops", seeds.get(0));
    }

    // -------------------------------------------------------------------------
    // Dedup keeping max weight
    // -------------------------------------------------------------------------

    @Test
    void project_dedupsOnRenderedString_keepsHighestWeight() {
        // Arrange — two entries that will render to the same string; highest weight wins.
        // Use a custom-capacity store so both entries survive admission dedup by origin id.
        KnowledgeEntry lowWeight = directEntry(ACTOR_ID, TARGET_ID, WorldEventType.CROP_HARVESTED, 1.0f);
        KnowledgeEntry highWeight = directEntry(ACTOR_ID, TARGET_ID, WorldEventType.CROP_HARVESTED, 5.0f);

        VillagerKnowledgeStore storeWithBoth = new VillagerKnowledgeStore(10);
        storeWithBoth.admit(lowWeight);
        storeWithBoth.admit(highWeight);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, storeWithBoth);

        // Assert — only one distinct seed
        assertEquals(1, seeds.size());
    }

    @Test
    void project_dedupsAcrossEntries_distinctSeedsPreserved() {
        // Arrange — two entries with different event types (render differently)
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 2.0f));
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.CROP_HARVESTED, 1.0f));

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — two distinct seeds are kept
        assertEquals(2, seeds.size());
    }

    // -------------------------------------------------------------------------
    // Ordering and cap
    // -------------------------------------------------------------------------

    @Test
    void project_ordersHighestWeightFirst() {
        // Arrange — three distinct entries with different weights; only seed-worthy types used
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.CROP_HARVESTED, 0.5f));
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.SHEEP_SHEARED, 5.0f));
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 2.0f));

        String actorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — highest weight (SHEEP_SHEARED, 5.0) comes first
        assertEquals(actorName + " sheared a sheep", seeds.get(0));
    }

    @Test
    void project_capsAtMaxSeeds() {
        // Arrange — more entries than MAX_SEEDS, each with a distinct actor so they render distinctly
        for (int i = 0; i < MonologueSeedProjector.MAX_SEEDS + 30; i++) {
            UUID actor = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", i + 1));
            this.store.admit(directEntry(actor, null, WorldEventType.CROP_HARVESTED, 1.0f));
        }

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — cap is enforced
        assertTrue(seeds.size() <= MonologueSeedProjector.MAX_SEEDS,
                "Expected at most MAX_SEEDS=" + MonologueSeedProjector.MAX_SEEDS
                        + " seeds, got: " + seeds.size());
        assertFalse(seeds.isEmpty(), "Expected at least one seed");
    }

    // -------------------------------------------------------------------------
    // No raw UUIDs in output
    // -------------------------------------------------------------------------

    @Test
    void project_noRawUuidsInOutput() {
        // Arrange — a typical trade entry
        KnowledgeEntry entry = directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 3.0f);
        this.store.admit(entry);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — seeds must not contain raw UUID strings (8-4-4-4-12 hex pattern)
        for (String seed : seeds) {
            assertFalse(seed.matches(".*[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.*"),
                    "Seed contains a raw UUID: " + seed);
        }
    }

    // -------------------------------------------------------------------------
    // WS3a: FAILURE outcome — end-to-end rendering through projector
    // -------------------------------------------------------------------------

    @Test
    void project_failedCourtship_rendersTriedToCourtButReason() {
        // Arrange — a FAILURE courtship with a reason
        KnowledgeEntry entry = directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_COMPLETED,
                EventOutcome.FAILURE, "no bed available", 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert — must render the attempt form, not "courted"
        assertEquals(1, seeds.size());
        assertEquals(actorName + " tried to court " + targetName + " but no bed available",
                seeds.get(0));
    }

    @Test
    void project_failedCourtship_noReason_usesGenericFallback() {
        // Arrange
        KnowledgeEntry entry = directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_COMPLETED,
                EventOutcome.FAILURE, null, 2.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert
        assertEquals(1, seeds.size());
        assertEquals(actorName + " tried to court " + targetName + " but it fell through",
                seeds.get(0));
    }

    @Test
    void project_failedTrade_rendersTriedToTradeButReason() {
        // Arrange
        KnowledgeEntry entry = directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED,
                EventOutcome.FAILURE, "haggling fell through", 3.0f);
        this.store.admit(entry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seeds = this.projector.project(OBSERVER_ID, this.store);

        // Assert
        assertEquals(1, seeds.size());
        assertEquals(actorName + " tried to trade with " + targetName + " but haggling fell through",
                seeds.get(0));
    }

    @Test
    void project_successCourtship_rendersCourtedNot_triedToCourt_regressionGuard() {
        // Arrange — explicit SUCCESS outcome must render identically to no outcome at all
        KnowledgeEntry successEntry = directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_COMPLETED,
                EventOutcome.SUCCESS, null, 2.0f);
        KnowledgeEntry noOutcomeEntry = directEntry(ACTOR_ID, TARGET_ID,
                WorldEventType.COURTSHIP_COMPLETED, 2.0f);

        VillagerKnowledgeStore storeSuccess = new VillagerKnowledgeStore();
        storeSuccess.admit(successEntry);
        VillagerKnowledgeStore storeNoOutcome = new VillagerKnowledgeStore();
        storeNoOutcome.admit(noOutcomeEntry);

        String actorName = this.nameResolver.resolve(ACTOR_ID);
        String targetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<String> seedsSuccess = this.projector.project(OBSERVER_ID, storeSuccess);
        List<String> seedsNoOutcome = this.projector.project(OBSERVER_ID, storeNoOutcome);

        // Assert — both must render the completed-act form, not the attempt form
        assertEquals(1, seedsSuccess.size());
        assertEquals(actorName + " courted " + targetName, seedsSuccess.get(0));
        assertEquals(seedsSuccess.get(0), seedsNoOutcome.get(0));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Direct (hop=0) entry with actor, optional target, and event_type metadata.
     */
    private static KnowledgeEntry directEntry(@Nullable UUID actorId,
                                              @Nullable UUID targetId,
                                              WorldEventType eventType,
                                              float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType, null);
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "raw content",
                ObservationType.SOCIAL,
                100L, 100L,
                targetId,
                metadata,
                weight);
    }

    /**
     * Direct (hop=0) entry with a detail value under {@link SeedPhrasebook#METADATA_KEY_DETAIL}.
     */
    private static KnowledgeEntry directEntryWithDetail(@Nullable UUID actorId,
                                                        @Nullable UUID targetId,
                                                        WorldEventType eventType,
                                                        String detail,
                                                        float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType, detail);
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "raw content",
                ObservationType.SOCIAL,
                100L, 100L,
                targetId,
                metadata,
                weight);
    }

    /**
     * Direct (hop=0) entry with outcome and optional reason metadata.
     */
    private static KnowledgeEntry directEntryWithOutcomeAndReason(@Nullable UUID actorId,
                                                                  @Nullable UUID targetId,
                                                                  WorldEventType eventType,
                                                                  EventOutcome outcome,
                                                                  @Nullable String reason,
                                                                  float weight) {
        Map<String, String> metadata = buildMetadataWithOutcome(actorId, eventType, outcome, reason);
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "raw content",
                ObservationType.SOCIAL,
                100L, 100L,
                targetId,
                metadata,
                weight);
    }

    /**
     * Hearsay (hop=1) entry.
     */
    private static KnowledgeEntry hearsayEntry(UUID sourceId,
                                               @Nullable UUID actorId,
                                               @Nullable UUID targetId,
                                               WorldEventType eventType,
                                               float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType, null);
        KnowledgeEntry base = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(),
                "raw content",
                ObservationType.SOCIAL,
                100L, 100L,
                targetId,
                metadata,
                weight);
        return KnowledgeEntry.fromHearsay(base, sourceId, 200L, weight * 0.8f);
    }

    private static Map<String, String> buildMetadata(@Nullable UUID actorId,
                                                     WorldEventType eventType,
                                                     @Nullable String detail) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("event_type", eventType.name());
        if (actorId != null) {
            metadata.put("actor_id", actorId.toString());
        }
        if (detail != null) {
            metadata.put(SeedPhrasebook.METADATA_KEY_DETAIL, detail);
        }
        return Map.copyOf(metadata);
    }

    private static Map<String, String> buildMetadataWithOutcome(@Nullable UUID actorId,
                                                                WorldEventType eventType,
                                                                @Nullable EventOutcome outcome,
                                                                @Nullable String reason) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("event_type", eventType.name());
        if (actorId != null) {
            metadata.put("actor_id", actorId.toString());
        }
        if (outcome != null) {
            metadata.put(SeedPhrasebook.METADATA_KEY_OUTCOME, outcome.name());
        }
        if (reason != null) {
            metadata.put(SeedPhrasebook.METADATA_KEY_REASON, reason);
        }
        return Map.copyOf(metadata);
    }

}
