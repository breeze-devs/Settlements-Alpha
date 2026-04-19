package dev.breezes.settlements.infrastructure.rendering.bubbles;

import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleEntrySnapshot;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.infrastructure.rendering.bubbles.canvas.SpeechBubble;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class BubbleManagerTest {

    @Test
    void applySnapshot_preservesIncomingServerOrderForStoredEntries() {
        BubbleManager manager = new TestBubbleManager();

        BubbleEntrySnapshot first = createSnapshot(BubbleChannel.SYSTEM, 1, 300, 3);
        BubbleEntrySnapshot second = createSnapshot(BubbleChannel.BEHAVIOR, 99, 100, 1);
        BubbleEntrySnapshot third = createSnapshot(BubbleChannel.CHAT, 50, 200, 2);

        manager.applySnapshot(List.of(first, second, third), 0L);

        List<UUID> storedOrder = new ArrayList<>(manager.getBubblesById().keySet());
        Assertions.assertEquals(List.of(first.bubbleId(), second.bubbleId(), third.bubbleId()), storedOrder);
    }

    @Test
    void getActiveBubble_usesRenderComparatorOrderingNotInsertionOrder() {
        BubbleManager manager = new TestBubbleManager();

        BubbleEntrySnapshot lowerPriorityChat = createSnapshot(BubbleChannel.CHAT, 1, 300, 3);
        BubbleEntrySnapshot higherPriorityBehavior = createSnapshot(BubbleChannel.BEHAVIOR, 100, 100, 1);

        manager.applySnapshot(List.of(lowerPriorityChat, higherPriorityBehavior), 0L);

        TestSpeechBubble active = (TestSpeechBubble) manager.getActiveBubble().orElseThrow();
        Assertions.assertEquals(higherPriorityBehavior.bubbleId(), active.bubbleId());
    }

    @Test
    void applySnapshot_retainedBubbleIdPreservesObjectIdentity() {
        TestBubbleManager manager = new TestBubbleManager();
        UUID retainedBubbleId = UUID.randomUUID();

        BubbleEntrySnapshot first = createSnapshot(retainedBubbleId, BubbleChannel.CHAT, 10, 100, 1);
        manager.applySnapshot(List.of(first), 0L);
        SpeechBubble firstBubbleRef = manager.getActiveBubble().orElseThrow();

        BubbleEntrySnapshot second = createSnapshot(retainedBubbleId, BubbleChannel.SYSTEM, 50, 200, 2);
        manager.applySnapshot(List.of(second), 0L);
        SpeechBubble secondBubbleRef = manager.getActiveBubble().orElseThrow();

        Assertions.assertSame(firstBubbleRef, secondBubbleRef);
        Assertions.assertEquals(1, manager.getCreateCount(retainedBubbleId));
    }

    @Test
    void applySnapshot_removedBubbleIdsArePrunedAndExpired() {
        TestBubbleManager manager = new TestBubbleManager();
        UUID keepId = UUID.randomUUID();
        UUID removeId = UUID.randomUUID();

        BubbleEntrySnapshot keep = createSnapshot(keepId, BubbleChannel.CHAT, 1, 100, 1);
        BubbleEntrySnapshot remove = createSnapshot(removeId, BubbleChannel.CHAT, 2, 101, 2);
        manager.applySnapshot(List.of(keep, remove), 0L);

        TestSpeechBubble removedBubbleRef = manager.getCreatedBubble(removeId);
        manager.applySnapshot(List.of(keep), 0L);

        Assertions.assertTrue(manager.getBubblesById().containsKey(keepId));
        Assertions.assertFalse(manager.getBubblesById().containsKey(removeId));
        Assertions.assertTrue(removedBubbleRef.expired());
    }

    @Test
    void applySnapshot_newBubbleIdsCreateNewInstances() {
        TestBubbleManager manager = new TestBubbleManager();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        BubbleEntrySnapshot first = createSnapshot(firstId, BubbleChannel.CHAT, 1, 100, 1);
        BubbleEntrySnapshot second = createSnapshot(secondId, BubbleChannel.CHAT, 1, 101, 2);

        manager.applySnapshot(List.of(first), 0L);
        manager.applySnapshot(List.of(first, second), 0L);

        Assertions.assertEquals(1, manager.getCreateCount(firstId));
        Assertions.assertEquals(1, manager.getCreateCount(secondId));
        Assertions.assertTrue(manager.getBubblesById().containsKey(secondId));
    }

    @Test
    void getActiveBubble_usesComparatorOrderingAfterReconciliation() {
        TestBubbleManager manager = new TestBubbleManager();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        BubbleEntrySnapshot first = createSnapshot(firstId, BubbleChannel.CHAT, 100, 100, 1);
        BubbleEntrySnapshot second = createSnapshot(secondId, BubbleChannel.CHAT, 50, 101, 2);
        manager.applySnapshot(List.of(first, second), 0L);

        TestSpeechBubble initialActive = (TestSpeechBubble) manager.getActiveBubble().orElseThrow();
        Assertions.assertEquals(firstId, initialActive.bubbleId());

        BubbleEntrySnapshot firstUpdated = createSnapshot(firstId, BubbleChannel.CHAT, 10, 200, 3);
        BubbleEntrySnapshot secondUpdated = createSnapshot(secondId, BubbleChannel.CHAT, 99, 201, 4);
        manager.applySnapshot(List.of(firstUpdated, secondUpdated), 0L);

        TestSpeechBubble reconciledActive = (TestSpeechBubble) manager.getActiveBubble().orElseThrow();
        Assertions.assertEquals(secondId, reconciledActive.bubbleId());
        Assertions.assertEquals(1, manager.getCreateCount(firstId));
        Assertions.assertEquals(1, manager.getCreateCount(secondId));
    }

    private static BubbleEntrySnapshot createSnapshot(BubbleChannel channel,
                                                      int priority,
                                                      long createdGameTime,
                                                      long sequenceNumber) {
        return createSnapshot(UUID.randomUUID(), channel, priority, createdGameTime, sequenceNumber, 0);
    }

    private static BubbleEntrySnapshot createSnapshot(UUID bubbleId,
                                                      BubbleChannel channel,
                                                      int priority,
                                                      long createdGameTime,
                                                      long sequenceNumber) {
        return createSnapshot(bubbleId, channel, priority, createdGameTime, sequenceNumber, 0);
    }

    private static BubbleEntrySnapshot createSnapshot(UUID bubbleId,
                                                      BubbleChannel channel,
                                                      int priority,
                                                      long createdGameTime,
                                                      long sequenceNumber,
                                                      int contentVersion) {
        return BubbleEntrySnapshot.builder()
                .bubbleId(bubbleId)
                .channel(channel)
                .ownerKey(null)
                .priority(priority)
                .expireGameTime(createdGameTime + 200)
                .createdGameTime(createdGameTime)
                .sequenceNumber(sequenceNumber)
                .contentVersion(contentVersion)
                .sourceType("test")
                .segments(List.of(BubbleSegment.Item.iconOnly(ResourceLocation.withDefaultNamespace("wheat"))))
                .build();
    }

    @Test
    void applySnapshot_bumpedContentVersion_rebuildsBubbleInstance() {
        // Arrange
        TestBubbleManager manager = new TestBubbleManager();
        UUID bubbleId = UUID.randomUUID();
        BubbleEntrySnapshot initial = createSnapshot(bubbleId, BubbleChannel.BEHAVIOR, 10, 100, 1, 0);
        manager.applySnapshot(List.of(initial), 0L);
        SpeechBubble firstBubble = manager.getActiveBubble().orElseThrow();

        // Act — same UUID, bumped contentVersion signals segment change
        BubbleEntrySnapshot updated = createSnapshot(bubbleId, BubbleChannel.BEHAVIOR, 10, 100, 1, 1);
        manager.applySnapshot(List.of(updated), 0L);

        // Assert — stale rendering instance must be discarded and a fresh one created
        SpeechBubble secondBubble = manager.getActiveBubble().orElseThrow();
        Assertions.assertNotSame(firstBubble, secondBubble);
        Assertions.assertEquals(2, manager.getCreateCount(bubbleId));
    }

    @Test
    void applySnapshot_unchangedContentVersion_preservesBubbleInstance() {
        // Arrange
        TestBubbleManager manager = new TestBubbleManager();
        UUID bubbleId = UUID.randomUUID();
        BubbleEntrySnapshot initial = createSnapshot(bubbleId, BubbleChannel.BEHAVIOR, 10, 100, 1, 0);
        manager.applySnapshot(List.of(initial), 0L);
        SpeechBubble firstBubble = manager.getActiveBubble().orElseThrow();

        // Act — same UUID, same contentVersion (metadata-only refresh, e.g. priority change)
        BubbleEntrySnapshot refreshed = createSnapshot(bubbleId, BubbleChannel.BEHAVIOR, 99, 200, 2, 0);
        manager.applySnapshot(List.of(refreshed), 0L);

        // Assert — same instance preserved so animation state (e.g. sprite frame clocks) is not reset
        SpeechBubble secondBubble = manager.getActiveBubble().orElseThrow();
        Assertions.assertSame(firstBubble, secondBubble);
        Assertions.assertEquals(1, manager.getCreateCount(bubbleId));
    }

    private static final class TestBubbleManager extends BubbleManager {

        private final Map<UUID, Integer> createCounts = new HashMap<>();
        private final Map<UUID, TestSpeechBubble> createdBubbles = new HashMap<>();

        @Override
        protected Optional<SpeechBubble> createBubble(BubbleEntrySnapshot entry, long currentGameTime) {
            this.createCounts.merge(entry.bubbleId(), 1, Integer::sum);
            TestSpeechBubble bubble = new TestSpeechBubble(entry.bubbleId());
            this.createdBubbles.put(entry.bubbleId(), bubble);
            return Optional.of(bubble);
        }

        private int getCreateCount(UUID bubbleId) {
            return this.createCounts.getOrDefault(bubbleId, 0);
        }

        private TestSpeechBubble getCreatedBubble(UUID bubbleId) {
            return this.createdBubbles.get(bubbleId);
        }

    }

    private static final class TestSpeechBubble implements SpeechBubble {

        private final UUID bubbleId;
        private boolean expired;

        private TestSpeechBubble(UUID bubbleId) {
            this.bubbleId = bubbleId;
            this.expired = false;
        }

        private UUID bubbleId() {
            return this.bubbleId;
        }

        private boolean expired() {
            return this.expired;
        }

        @Override
        public void render(RenderParameter parameter) {
        }

        @Override
        public void tick(double deltaTick) {
        }

        @Override
        public boolean isExpired() {
            return this.expired;
        }

        @Override
        public void setExpired() {
            this.expired = true;
        }

        @Override
        public void reset() {
            this.expired = false;
        }

    }

}
