package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.application.ai.naming.VillagerNameResolver;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.PackedPos;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EpisodicEntryAssembler}.
 * <p>
 * Uses real {@link VillagerNameResolver} (pure, deterministic) and hand-built
 * {@link KnowledgeEntry} objects. No Minecraft types are involved.
 */
class EpisodicEntryAssemblerTest {

    private static final UUID OBSERVER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID TARGET_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");
    private static final UUID SOURCE_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000004");

    private static final long CURRENT_TICK = 10_000L;
    private static final long ADMITTED_TICK = 9_000L;

    /**
     * Episodic cap used by the assembler under test; mirrors a server-configured ceiling.
     */
    private static final int MAX_EPISODIC_ENTRIES = 24;

    private VillagerNameResolver nameResolver;
    private EpisodicEntryAssembler assembler;
    private VillagerKnowledgeStore store;

    @BeforeEach
    void setUp() {
        this.nameResolver = new VillagerNameResolver();
        this.assembler = new EpisodicEntryAssembler(this.nameResolver, inferenceConfigWithCap(MAX_EPISODIC_ENTRIES));
        this.store = new VillagerKnowledgeStore();
    }

    private static InferenceConfig inferenceConfigWithCap(int maxEpisodicEntries) {
        return new InferenceConfig(true, "http://localhost:9999", "", "en_us", maxEpisodicEntries);
    }

    @Test
    void assemble_emptyStore_returnsEmptyList() {
        // Arrange — store is empty

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void assemble_nonSeedWorthyEventType_entryExcluded() {
        // Arrange — BEHAVIOR_STARTED is not seed-worthy (isSeedWorthy() == false)
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.BEHAVIOR_STARTED, 5.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — lifecycle noise is filtered before reaching the DTO list
        assertTrue(result.isEmpty());
    }

    @Test
    void assemble_tradeInviteSent_entryExcluded() {
        // Arrange — TRADE_INVITE_SENT is not seed-worthy
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.TRADE_INVITE_SENT, 3.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void assemble_missingEventTypeMetadata_entryExcluded() {
        // Arrange — entry has no event_type key in metadata
        KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, null, Map.of(), 2.0f, null);
        this.store.admit(entry);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — unknown event type treated as non-seed-worthy
        assertTrue(result.isEmpty());
    }

    @Test
    void assemble_hop0_actorEqualsObserver_perspectiveIsParticipant() {
        // Arrange — actor_id matches observerId: the villager did this themselves
        this.store.admit(directEntry(OBSERVER_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.size());
        assertEquals("FIRST_HAND_PARTICIPANT", result.get(0).getPerspective());
    }

    @Test
    void assemble_hop0_actorAbsent_perspectiveIsParticipant() {
        // Arrange — no actor_id in metadata (common for self-recorded terminal deeds)
        this.store.admit(directEntry(null, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — absent actor defaults to PARTICIPANT, not BYSTANDER, to avoid "I saw someone harvest"
        assertEquals(1, result.size());
        assertEquals("FIRST_HAND_PARTICIPANT", result.get(0).getPerspective());
    }

    @Test
    void assemble_hop0_actorDifferentFromObserver_perspectiveIsBystander() {
        // Arrange — the villager witnessed someone else's deed
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.size());
        assertEquals("FIRST_HAND_BYSTANDER", result.get(0).getPerspective());
    }

    @Test
    void assemble_hop1_perspectiveIsHearsay() {
        // Arrange — hearsay entry (hop = 1)
        this.store.admit(hearsayEntry(SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.size());
        assertEquals("HEARSAY", result.get(0).getPerspective());
    }

    @Test
    void assemble_participant_actorFieldIsNull() {
        // Arrange — PARTICIPANT perspective: actor name must be null (SIS renders "I")
        this.store.admit(directEntry(OBSERVER_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertNull(result.get(0).getActor());
    }

    @Test
    void assemble_bystander_actorFieldIsResolvedName() {
        // Arrange
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));
        String expectedActorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedActorName, result.get(0).getActor());
    }

    @Test
    void assemble_hearsay_actorFieldIsResolvedName() {
        // Arrange
        this.store.admit(hearsayEntry(SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));
        String expectedActorName = this.nameResolver.resolve(ACTOR_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedActorName, result.get(0).getActor());
    }

    @Test
    void assemble_tradeCompleted_targetFieldIsResolvedName() {
        // Arrange — TRADE_COMPLETED: relatedEntity is the trading partner
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED, 2.5f));
        String expectedTargetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedTargetName, result.get(0).getTarget());
    }

    @Test
    void assemble_courtshipCompleted_targetFieldIsResolvedName() {
        // Arrange — COURTSHIP_COMPLETED: relatedEntity is the courtship partner
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_COMPLETED, 2.5f));
        String expectedTargetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedTargetName, result.get(0).getTarget());
    }

    @Test
    void assemble_emeraldsDonated_targetFieldIsResolvedName() {
        // Arrange — EMERALDS_DONATED: relatedEntity is the donation recipient
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.EMERALDS_DONATED, 2.5f));
        String expectedTargetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedTargetName, result.get(0).getTarget());
    }

    @Test
    void assemble_courtshipRejected_targetFieldIsResolvedName() {
        // Arrange — COURTSHIP_REJECTED: relatedEntity is the spurned presenter
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.COURTSHIP_REJECTED, 2.5f));
        String expectedTargetName = this.nameResolver.resolve(TARGET_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedTargetName, result.get(0).getTarget());
    }

    @Test
    void assemble_resourceHarvested_targetFieldIsNullEvenWithRelatedEntity() {
        // Arrange — RESOURCE_HARVESTED: relatedEntity might be set but is not a nameable person
        this.store.admit(directEntry(ACTOR_ID, TARGET_ID, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — non-social event types must not resolve target to avoid bogus human names
        assertNull(result.get(0).getTarget());
    }

    @Test
    void assemble_sheepSheared_targetFieldIsNull() {
        // Arrange
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.SHEEP_SHEARED, 1.8f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertNull(result.get(0).getTarget());
    }

    @Test
    void assemble_hearsay_sourceFieldIsResolvedName() {
        // Arrange
        this.store.admit(hearsayEntry(SOURCE_ID, ACTOR_ID, null, WorldEventType.TRADE_COMPLETED, 2.0f));
        String expectedSourceName = this.nameResolver.resolve(SOURCE_ID);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(expectedSourceName, result.get(0).getSource());
    }

    @Test
    void assemble_firstHand_sourceFieldIsNull() {
        // Arrange
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — first-hand entries have no gossip source; field must be absent on wire
        assertNull(result.get(0).getSource());
    }

    @Test
    void assemble_outcomeAndReasonPresentInMetadata_passthroughToDto() {
        // Arrange
        this.store.admit(directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED,
                EventOutcome.FAILURE, "haggling fell through", 2.5f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — raw string pass-through, not enum re-encoding
        assertEquals("FAILURE", result.get(0).getOutcome());
        assertEquals("haggling fell through", result.get(0).getReason());
    }

    @Test
    void assemble_successOutcome_passthroughAsString() {
        // Arrange
        this.store.admit(directEntryWithOutcomeAndReason(
                ACTOR_ID, TARGET_ID, WorldEventType.TRADE_COMPLETED,
                EventOutcome.SUCCESS, null, 2.5f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals("SUCCESS", result.get(0).getOutcome());
        assertNull(result.get(0).getReason());
    }

    @Test
    void assemble_noOutcomeMetadata_outcomeAndReasonAreNull() {
        // Arrange — no outcome or reason key in metadata
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — absent metadata → null fields → Gson omits from wire
        assertNull(result.get(0).getOutcome());
        assertNull(result.get(0).getReason());
    }

    @Test
    void assemble_ageTicks_isCurrentTickMinusAdmittedAtTick() {
        // Arrange — entry admitted at tick 9000; current tick is 10000
        this.store.admit(directEntryWithAdmitTick(ACTOR_ID, WorldEventType.RESOURCE_HARVESTED, ADMITTED_TICK, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(CURRENT_TICK - ADMITTED_TICK, result.get(0).getAgeTicks());
    }

    @Test
    void assemble_ageTicks_clampedToZeroWhenCurrentTickBeforeAdmitted() {
        // Arrange — simulate future-dated admittedAtTick (clock skew / test setup artifact)
        long futureAdmittedTick = CURRENT_TICK + 500;
        this.store.admit(directEntryWithAdmitTick(ACTOR_ID, WorldEventType.RESOURCE_HARVESTED, futureAdmittedTick, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — clamped to zero, never negative
        assertEquals(0L, result.get(0).getAgeTicks());
    }

    @Test
    void assemble_dedupeByOriginId_prefersLowerHop() {
        // Arrange — same originObservationId, one first-hand (hop=0) and one hearsay (hop=1)
        UUID sharedOriginId = UUID.randomUUID();
        KnowledgeEntry firstHand = buildEntryWithOriginId(
                sharedOriginId, OBSERVER_ID, null, WorldEventType.RESOURCE_HARVESTED, 0, 1.0f);
        KnowledgeEntry hearsay = buildHearsayWithOriginId(
                sharedOriginId, SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 1, 5.0f);

        VillagerKnowledgeStore storeWithBoth = new VillagerKnowledgeStore(10);
        storeWithBoth.admit(firstHand);
        storeWithBoth.admit(hearsay);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, storeWithBoth, CURRENT_TICK);

        // Assert — exactly one entry; the first-hand entry (lower hop) wins
        assertEquals(1, result.size());
        assertEquals("FIRST_HAND_PARTICIPANT", result.get(0).getPerspective());
    }

    @Test
    void assemble_dedupeByOriginId_collapsesSameOriginToSingleEntry() {
        // Arrange — two entries sharing one originObservationId. The store admits the first and
        // treats the same-source repeat as a duplicate, so exactly one survives all the way to the
        // wire (the assembler's per-origin dedup mirrors the store's per-origin keying).
        UUID sharedOriginId = UUID.randomUUID();
        KnowledgeEntry first = buildHearsayWithOriginId(
                sharedOriginId, SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 1, 5.0f);
        KnowledgeEntry duplicate = buildHearsayWithOriginId(
                sharedOriginId, SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 1, 1.0f);

        VillagerKnowledgeStore storeWithBoth = new VillagerKnowledgeStore(10);
        storeWithBoth.admit(first);
        storeWithBoth.admit(duplicate);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, storeWithBoth, CURRENT_TICK);

        // Assert — the shared origin id collapses to exactly one episodic DTO on the wire
        assertEquals(1, result.size());
        assertEquals(WorldEventType.RESOURCE_HARVESTED.name(), result.get(0).getEventType());
    }

    @Test
    void assemble_sortsByWeightDescending() {
        // Arrange — three seed-worthy entries with distinct weights
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 1.0f));
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.SHEEP_SHEARED, 5.0f));
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.COW_MILKED, 3.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — highest weight first
        assertEquals(3, result.size());
        assertEquals("SHEEP_SHEARED", result.get(0).getEventType());
        assertEquals("COW_MILKED", result.get(1).getEventType());
        assertEquals("RESOURCE_HARVESTED", result.get(2).getEventType());
    }

    @Test
    void assemble_capsAtMaxEpisodicEntries() {
        // Arrange — more entries than the cap; each with a distinct actor to ensure unique origin ids
        int overCap = MAX_EPISODIC_ENTRIES + 10;
        VillagerKnowledgeStore largeStore = new VillagerKnowledgeStore(overCap + 10);
        for (int i = 0; i < overCap; i++) {
            UUID actor = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", i + 1));
            largeStore.admit(directEntry(actor, null, WorldEventType.RESOURCE_HARVESTED, 1.0f));
        }

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, largeStore, CURRENT_TICK);

        // Assert
        assertEquals(MAX_EPISODIC_ENTRIES, result.size());
    }

    @Test
    void assemble_detailPrefixedMetadataKeys_populateDetailMapOnDto() {
        // Arrange — entry carrying detail.item and detail.count metadata
        Map<String, String> metadata = new HashMap<>(buildMetadata(ACTOR_ID, WorldEventType.RESOURCE_HARVESTED));
        metadata.put("detail.item", "melons");
        metadata.put("detail.count", "3");
        KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, null, Map.copyOf(metadata), 2.0f, null);
        this.store.admit(entry);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — prefix stripped, slots present in the nested map
        assertEquals(1, result.size());
        Map<String, String> detail = result.get(0).getDetail();
        assertEquals("melons", detail.get("item"));
        assertEquals("3", detail.get("count"));
    }

    @Test
    void assemble_noDetailPrefixedKeys_detailIsNull() {
        // Arrange — entry with no detail.* keys
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — absent detail → null so Gson omits the field on the wire
        assertEquals(1, result.size());
        assertNull(result.get(0).getDetail());
    }

    @Test
    void assemble_detailPlayerSlot_populatesPlayerNameOnDetailMap() {
        // Arrange — a sighting whose detail.player slot carries the player's display name,
        // matching what WorldEventEmitter.emitSighting snapshots for a Player subject
        Map<String, String> metadata = new HashMap<>(buildMetadata(ACTOR_ID, WorldEventType.PLAYER_SIGHTED));
        metadata.put(ObservationMetadataKeys.DETAIL_PREFIX + "player", "Steve");
        KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, null, Map.copyOf(metadata), 2.0f, null);
        this.store.admit(entry);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — prefix stripped, display name surfaces under the bare "player" slot
        assertEquals(1, result.size());
        Map<String, String> detail = result.get(0).getDetail();
        assertEquals("Steve", detail.get("player"));
    }

    @Test
    void assemble_tradeDetailWithAllSlots_allSlotsPresent() {
        // Arrange — trade entry with item, count, and price
        Map<String, String> metadata = new HashMap<>(buildMetadata(ACTOR_ID, WorldEventType.TRADE_COMPLETED));
        metadata.put("detail.item", "bread");
        metadata.put("detail.count", "4");
        metadata.put("detail.price", "1 emerald");
        KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, TARGET_ID, Map.copyOf(metadata), 2.5f, null);
        this.store.admit(entry);

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.size());
        Map<String, String> detail = result.get(0).getDetail();
        assertEquals("bread", detail.get("item"));
        assertEquals("4", detail.get("count"));
        assertEquals("1 emerald", detail.get("price"));
    }

    @Test
    void assemble_hopFieldMatchesEntryHop() {
        // Arrange — hearsay entry at hop=1
        this.store.admit(hearsayEntry(SOURCE_ID, ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.get(0).getHop());
    }

    @Test
    void assemble_firstHandEntry_hopIsZero() {
        // Arrange
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(0, result.get(0).getHop());
    }

    @Test
    void assemble_packedPosPresent_posPopulatedAsFlooredInts() {
        // Arrange — floor(-5.5) = -6; packing happens at the producer (PerceptionPipeline), so the
        // test packs an already-floored coordinate directly, proving floor semantics were applied
        // before packing rather than truncation toward zero.
        long packedPos = PackedPos.asLong(123, 64, -6);
        this.store.admit(directEntryWithPos(ACTOR_ID, WorldEventType.ZOMBIE_SIGHTED, packedPos, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert
        assertEquals(1, result.size());
        int[] pos = result.get(0).getPos();
        assertArrayEquals(new int[]{123, 64, -6}, pos);
    }

    @Test
    void assemble_packedPosAbsent_posIsNull() {
        // Arrange — directEntry helper passes a null packedPos
        this.store.admit(directEntry(ACTOR_ID, null, WorldEventType.RESOURCE_HARVESTED, 2.0f));

        // Act
        List<EpisodicEntryDTO> result = this.assembler.assemble(OBSERVER_ID, this.store, CURRENT_TICK);

        // Assert — absent pos → null so Gson omits the field on the wire
        assertEquals(1, result.size());
        assertNull(result.get(0).getPos());
    }

    private static KnowledgeEntry directEntryWithPos(@Nullable UUID actorId,
                                                     WorldEventType eventType,
                                                     long packedPos,
                                                     float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType);
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, null, metadata, weight, packedPos);
    }

    private static KnowledgeEntry directEntry(@Nullable UUID actorId,
                                              @Nullable UUID targetId,
                                              WorldEventType eventType,
                                              float weight) {
        return directEntryWithAdmitTick(actorId, eventType, ADMITTED_TICK, weight, targetId);
    }

    private static KnowledgeEntry directEntryWithAdmitTick(@Nullable UUID actorId,
                                                           WorldEventType eventType,
                                                           long admitTick,
                                                           float weight) {
        return directEntryWithAdmitTick(actorId, eventType, admitTick, weight, null);
    }

    private static KnowledgeEntry directEntryWithAdmitTick(@Nullable UUID actorId,
                                                           WorldEventType eventType,
                                                           long admitTick,
                                                           float weight,
                                                           @Nullable UUID targetId) {
        Map<String, String> metadata = buildMetadata(actorId, eventType);
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                admitTick, admitTick, targetId, metadata, weight, null);
    }

    private static KnowledgeEntry directEntryWithOutcomeAndReason(@Nullable UUID actorId,
                                                                  @Nullable UUID targetId,
                                                                  WorldEventType eventType,
                                                                  EventOutcome outcome,
                                                                  @Nullable String reason,
                                                                  float weight) {
        Map<String, String> metadata = new HashMap<>(buildMetadata(actorId, eventType));
        metadata.put(ObservationMetadataKeys.OUTCOME, outcome.name());
        if (reason != null) {
            metadata.put(ObservationMetadataKeys.REASON, reason);
        }
        return KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, targetId, Map.copyOf(metadata), weight, null);
    }

    private static KnowledgeEntry hearsayEntry(UUID sourceId,
                                               @Nullable UUID actorId,
                                               @Nullable UUID targetId,
                                               WorldEventType eventType,
                                               float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType);
        KnowledgeEntry base = KnowledgeEntry.fromDirectObservation(
                UUID.randomUUID(), ObservationType.SOCIAL,
                ADMITTED_TICK, ADMITTED_TICK, targetId, metadata, weight, null);
        return KnowledgeEntry.fromHearsay(base, sourceId, ADMITTED_TICK + 100L, weight * 0.8f);
    }

    /**
     * Builds a first-hand entry with a predetermined origin id (for dedupe tests).
     */
    private static KnowledgeEntry buildEntryWithOriginId(UUID originId,
                                                         @Nullable UUID actorId,
                                                         @Nullable UUID targetId,
                                                         WorldEventType eventType,
                                                         int hop,
                                                         float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType);
        return KnowledgeEntry.builder()
                .originObservationId(originId)
                .type(ObservationType.SOCIAL)
                .originTimestampTick(ADMITTED_TICK)
                .admittedAtTick(ADMITTED_TICK)
                .relatedEntity(targetId)
                .metadata(metadata)
                .packedPos(null)
                .source(null)
                .hop(hop)
                .weight(weight)
                .originalWeight(weight)
                .corroborationCount(0)
                .build();
    }

    /**
     * Builds a hearsay entry with a predetermined origin id (for dedupe tests).
     */
    private static KnowledgeEntry buildHearsayWithOriginId(UUID originId,
                                                           UUID sourceId,
                                                           @Nullable UUID actorId,
                                                           @Nullable UUID targetId,
                                                           WorldEventType eventType,
                                                           int hop,
                                                           float weight) {
        Map<String, String> metadata = buildMetadata(actorId, eventType);
        return KnowledgeEntry.builder()
                .originObservationId(originId)
                .type(ObservationType.SOCIAL)
                .originTimestampTick(ADMITTED_TICK)
                .admittedAtTick(ADMITTED_TICK + 100L)
                .relatedEntity(targetId)
                .metadata(metadata)
                .packedPos(null)
                .source(sourceId)
                .hop(hop)
                .weight(weight)
                .originalWeight(weight)
                .corroborationCount(0)
                .build();
    }

    private static Map<String, String> buildMetadata(@Nullable UUID actorId, WorldEventType eventType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ObservationMetadataKeys.EVENT_TYPE, eventType.name());
        if (actorId != null) {
            metadata.put(ObservationMetadataKeys.ACTOR_ID, actorId.toString());
        }
        return Map.copyOf(metadata);
    }

}
