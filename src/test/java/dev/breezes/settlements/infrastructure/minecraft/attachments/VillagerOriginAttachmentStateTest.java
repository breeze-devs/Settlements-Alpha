package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.personality.OriginType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerOriginAttachmentStateTest {

    @Test
    void empty_isUninitializedWithUnknownOrigin() {
        // Arrange, Act
        VillagerOriginAttachmentState state = VillagerOriginAttachmentState.empty();

        // Assert
        assertFalse(state.initialized());
        assertEquals(OriginType.UNKNOWN, state.origin());
    }

    @Test
    void of_isInitializedWithTheGivenOrigin() {
        // Arrange, Act
        VillagerOriginAttachmentState state = VillagerOriginAttachmentState.of(OriginType.BRED);

        // Assert
        assertTrue(state.initialized());
        assertEquals(OriginType.BRED, state.origin());
    }

    @Test
    void of_canStampUnknownExplicitlyAsDistinctFromNeverStamped() {
        // Arrange, Act — same origin value as empty(), but initialized() must differ.
        VillagerOriginAttachmentState state = VillagerOriginAttachmentState.of(OriginType.UNKNOWN);

        // Assert
        assertTrue(state.initialized());
        assertEquals(OriginType.UNKNOWN, state.origin());
    }

}
