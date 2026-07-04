package dev.breezes.settlements.domain.ai.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PackedPos}. Pure bit math — no Minecraft types involved.
 */
class PackedPosTest {

    @Test
    void asLong_thenUnpack_roundTripsPositiveCoords() {
        // Arrange
        long packed = PackedPos.asLong(100, 64, 200);

        // Act, Assert
        assertEquals(100, PackedPos.x(packed));
        assertEquals(64, PackedPos.y(packed));
        assertEquals(200, PackedPos.z(packed));
    }

    @Test
    void asLong_thenUnpack_roundTripsNegativeCoords() {
        // Arrange
        long packed = PackedPos.asLong(-100, -32, -200);

        // Act, Assert
        assertEquals(-100, PackedPos.x(packed));
        assertEquals(-32, PackedPos.y(packed));
        assertEquals(-200, PackedPos.z(packed));
    }

    @Test
    void asLong_thenUnpack_roundTripsZero() {
        // Arrange
        long packed = PackedPos.asLong(0, 0, 0);

        // Act, Assert
        assertEquals(0, PackedPos.x(packed));
        assertEquals(0, PackedPos.y(packed));
        assertEquals(0, PackedPos.z(packed));
    }

    @Test
    void asLong_thenUnpack_roundTripsOverworldYExtremes() {
        // Arrange — vanilla overworld build range is roughly -64..320, well within the 12-bit
        // signed Y range (-2048..2047) this packing supports.
        long packedMin = PackedPos.asLong(0, -64, 0);
        long packedMax = PackedPos.asLong(0, 320, 0);

        // Act, Assert
        assertEquals(-64, PackedPos.y(packedMin));
        assertEquals(320, PackedPos.y(packedMax));
    }

    @Test
    void asLong_thenUnpack_roundTripsMaxSignedCoordRange() {
        // Arrange — X/Z are 26-bit signed (-33554432..33554431); Y is 12-bit signed (-2048..2047)
        long packed = PackedPos.asLong(33_554_431, 2047, -33_554_432);

        // Act, Assert
        assertEquals(33_554_431, PackedPos.x(packed));
        assertEquals(2047, PackedPos.y(packed));
        assertEquals(-33_554_432, PackedPos.z(packed));
    }

    @Test
    void asLong_mixedSignCoords_roundTripIndependently() {
        // Arrange — proves the three fields do not bleed into each other when signs differ
        long packed = PackedPos.asLong(-5, 70, 12345);

        // Act, Assert
        assertEquals(-5, PackedPos.x(packed));
        assertEquals(70, PackedPos.y(packed));
        assertEquals(12345, PackedPos.z(packed));
    }

}
