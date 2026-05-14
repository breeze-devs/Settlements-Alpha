package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationArchetypeTest {

    @Test
    void fromNetworkByte_whenEncodedValueIsValid_returnsMatchingArchetype() {
        // Arrange
        AnimationArchetype expected = AnimationArchetype.SWING_HEAVY;

        // Act
        AnimationArchetype actual = AnimationArchetype.fromNetworkByte(expected.toNetworkByte());

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void fromNetworkByte_whenEncodedValueIsOutOfRange_returnsIdle() {
        // Arrange
        byte encoded = (byte) AnimationArchetype.values().length;

        // Act
        AnimationArchetype actual = AnimationArchetype.fromNetworkByte(encoded);

        // Assert
        assertEquals(AnimationArchetype.IDLE, actual);
    }

}
