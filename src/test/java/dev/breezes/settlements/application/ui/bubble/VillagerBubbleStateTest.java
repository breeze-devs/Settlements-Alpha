package dev.breezes.settlements.application.ui.bubble;

import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class VillagerBubbleStateTest {

    @Test
    void put_replacesExistingEntryForSameOwnerWithinChannel() {
        VillagerBubbleState state = new VillagerBubbleState();

        BubbleEntry first = createEntry(UUID.randomUUID(), BubbleChannel.BEHAVIOR, "behavior:shear_sheep",
                10, 100, 200, 0);
        BubbleEntry replacement = createEntry(UUID.randomUUID(), BubbleChannel.BEHAVIOR, "behavior:shear_sheep",
                20, 120, 220, 1);

        state.put(first);
        state.put(replacement);

        Assertions.assertTrue(state.getById(first.bubbleId()).isEmpty());
        Assertions.assertEquals(replacement, state.getByOwner(BubbleChannel.BEHAVIOR, "behavior:shear_sheep").orElseThrow());
        Assertions.assertEquals(List.of(replacement), state.getEntries(BubbleChannel.BEHAVIOR));
    }

    @Test
    void pruneExpired_removesOnlyExpiredEntries() {
        VillagerBubbleState state = new VillagerBubbleState();

        BubbleEntry expired = createEntry(UUID.randomUUID(), BubbleChannel.BEHAVIOR, "expired", 0, 100, 150, 0);
        BubbleEntry live = createEntry(UUID.randomUUID(), BubbleChannel.CHAT, null, 0, 120, 300, 1);

        state.put(expired);
        state.put(live);

        List<BubbleEntry> removed = state.pruneExpired(200);

        Assertions.assertEquals(List.of(expired), removed);
        Assertions.assertTrue(state.getById(expired.bubbleId()).isEmpty());
        Assertions.assertEquals(live, state.getById(live.bubbleId()).orElseThrow());
    }

    private static BubbleEntry createEntry(UUID bubbleId,
                                           BubbleChannel channel,
                                           String ownerKey,
                                           int priority,
                                           long createdGameTime,
                                           long expireGameTime,
                                           long sequenceNumber) {
        return BubbleEntry.builder()
                .bubbleId(bubbleId)
                .channel(channel)
                .ownerKey(ownerKey)
                .message(BubbleMessage.builder()
                        .priority(priority)
                        .ttl(ClockTicks.seconds(5))
                        .sourceType("test")
                        .segments(List.of(BubbleSegment.Item.iconOnly(ResourceLocation.withDefaultNamespace("wheat"))))
                        .build())
                .createdGameTime(createdGameTime)
                .expireGameTime(expireGameTime)
                .sequenceNumber(sequenceNumber)
                .build();
    }

}
