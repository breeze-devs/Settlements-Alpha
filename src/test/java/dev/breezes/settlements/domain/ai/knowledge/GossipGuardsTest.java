package dev.breezes.settlements.domain.ai.knowledge;

import dev.breezes.settlements.domain.ai.observation.ObservationType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two gossip guards end-to-end using {@link VillagerKnowledgeStore}
 * and {@link KnowledgeEntry} provenance fields.
 * No Minecraft types or mocks needed.
 */
class GossipGuardsTest {

    // -------------------------------------------------------------------------
    // Guard 1 — origin-id dedupe (news does not ping-pong back as fresh)
    // -------------------------------------------------------------------------

    @Test
    void guard1_receiverDoesNotAdmitEntryTwiceViaAnyPath() {
        // Arrange – villager A observes a fact
        UUID originId = UUID.randomUUID();
        KnowledgeEntry directFact = KnowledgeEntry.fromDirectObservation(
                originId, "zombie near the farm", ObservationType.THREAT,
                100L, 100L, null, Map.of(), 3.0f);

        VillagerKnowledgeStore receiverStore = new VillagerKnowledgeStore();

        // Act – first receipt (directly via observation)
        AdmitResult firstAdmit = receiverStore.admit(directFact);

        // Act – second receipt (same fact arriving via gossip from a different source; different source
        // so this counts as corroboration, not a silent duplicate)
        KnowledgeEntry hearsayVersion = KnowledgeEntry.fromHearsay(
                directFact, UUID.randomUUID(), 200L, 1.5f);
        AdmitResult secondAdmit = receiverStore.admit(hearsayVersion);

        // Assert – Guard 1: same origin-id is deduplicated regardless of the path.
        // The hearsay source differs from null (the first-hand source) so it corroborates.
        assertEquals(AdmitResult.NEW_ENTRY, firstAdmit, "First admission should succeed");
        assertEquals(AdmitResult.CORROBORATED_EXISTING, secondAdmit,
                "Second admission (same origin-id, different source) should corroborate");
        assertTrue(receiverStore.knows(originId));
    }

    @Test
    void guard1_newsCannotPingPongFromReceiverBackToInitiator() {
        // Arrange – initiator shares fact with receiver
        UUID originId = UUID.randomUUID();
        KnowledgeEntry original = KnowledgeEntry.fromDirectObservation(
                originId, "trade completed at market", ObservationType.SOCIAL,
                50L, 50L, null, Map.of(), 2.5f);

        UUID initiatorId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        VillagerKnowledgeStore initiatorStore = new VillagerKnowledgeStore();
        VillagerKnowledgeStore receiverStore = new VillagerKnowledgeStore();

        initiatorStore.admit(original);

        KnowledgeEntry hearsay = KnowledgeEntry.fromHearsay(original, initiatorId, 60L, 1.5f);
        receiverStore.admit(hearsay);

        // Act – receiver attempts to "share back" the same fact to the initiator.
        // The initiator's stored source is null (first-hand); the ping-back source is receiverId —
        // different source, so this would normally corroborate. The origin-id dedup still fires.
        KnowledgeEntry pingBack = KnowledgeEntry.fromHearsay(hearsay, receiverId, 70L, 0.9f);
        AdmitResult pingBackResult = initiatorStore.admit(pingBack);

        // Assert – initiator already knows this fact; it corroborates (different source) rather than
        // being a new entry — still no new storage happens, which is the important invariant.
        assertNotEquals(AdmitResult.NEW_ENTRY, pingBackResult,
                "Ping-pong back to initiator must not create a new entry");
    }

    // -------------------------------------------------------------------------
    // Guard 2 — hop cap (settlement never becomes a perfect-information network)
    // -------------------------------------------------------------------------

    @Test
    void guard2_entryAtCapIsStoredButFlagged() {
        // Arrange
        UUID originId = UUID.randomUUID();
        KnowledgeEntry original = KnowledgeEntry.fromDirectObservation(
                originId, "honey ready at hive", ObservationType.RESOURCE,
                10L, 10L, null, Map.of(), 2.0f);

        // Chain through MAX_HOP_COUNT hops
        KnowledgeEntry current = original;
        for (int i = 0; i < KnowledgeEntry.MAX_HOP_COUNT; i++) {
            current = KnowledgeEntry.fromHearsay(current, UUID.randomUUID(), 20L + i, 1.0f);
        }

        VillagerKnowledgeStore store = new VillagerKnowledgeStore();

        // Act
        AdmitResult result = store.admit(current);

        // Assert – entry at exactly the cap is stored locally...
        assertEquals(AdmitResult.NEW_ENTRY, result, "Entry at the hop cap should be stored");
        // ...but is not shareable (Guard 2 prevents further propagation)
        assertFalse(current.isShareable(),
                "Entry at the hop cap should not be shareable — propagation must stop here");
    }

    @Test
    void guard2_entryBeyondCapIsRejected() {
        // Arrange – manually build an entry at hop = MAX_HOP_COUNT + 1
        UUID originId = UUID.randomUUID();
        KnowledgeEntry original = KnowledgeEntry.fromDirectObservation(
                originId, "wool ready to shear", ObservationType.RESOURCE,
                10L, 10L, null, Map.of(), 2.0f);

        KnowledgeEntry current = original;
        for (int i = 0; i <= KnowledgeEntry.MAX_HOP_COUNT; i++) {
            // This produces hop = MAX_HOP_COUNT + 1 after the loop
            current = KnowledgeEntry.fromHearsay(current, UUID.randomUUID(), 20L + i, 1.0f);
        }

        VillagerKnowledgeStore store = new VillagerKnowledgeStore();

        // Act
        AdmitResult result = store.admit(current);

        // Assert – Guard 2: entries beyond the cap are rejected outright
        assertEquals(AdmitResult.REJECTED_HOP_CAP, result, "Entry beyond the hop cap must be rejected by Guard 2");
        assertTrue(store.isEmpty());
    }

    @Test
    void guard2_entryBelowCapIsShareable() {
        // Arrange
        UUID originId = UUID.randomUUID();
        KnowledgeEntry firstHand = KnowledgeEntry.fromDirectObservation(
                originId, "smith at the forge", ObservationType.TASK_COMPLETION,
                10L, 10L, null, Map.of(), 1.5f);

        KnowledgeEntry oneHop = KnowledgeEntry.fromHearsay(firstHand, UUID.randomUUID(), 20L, 1.0f);

        // Assert – one hop below cap is shareable
        assertTrue(firstHand.isShareable(), "First-hand entry (hop 0) must be shareable");
        assertTrue(oneHop.isShareable(), "Hop-1 entry is still below cap and must be shareable");
    }

}
