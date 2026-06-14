package dev.breezes.settlements.domain.ai.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NavigationTypeTest {

    @Test
    void fromNetworkByte_whenEncodedValueIsValid_returnsMatchingNavigationType() {
        // Arrange
        NavigationType expected = NavigationType.RUN;

        // Act
        NavigationType actual = NavigationType.fromNetworkByte(expected.toNetworkByte());

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void fromNetworkByte_whenEncodedValueIsOutOfRange_returnsStroll() {
        // Arrange
        byte encoded = (byte) NavigationType.values().length;

        // Act
        NavigationType actual = NavigationType.fromNetworkByte(encoded);

        // Assert
        assertEquals(NavigationType.WALK, actual);
    }

}
