package dev.breezes.settlements.domain.personality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class OriginTypeTest {

    @Test
    void values_areExactlyTheSurvivalCanonicalSet() {
        // Arrange, Act
        OriginType[] values = OriginType.values();

        // Assert — this enum is a cross-repo wire contract (PERSONA `spawnType`); renaming,
        // reordering, or adding a constant here is a change SIS's request DTO must mirror.
        assertArrayEquals(new OriginType[]{OriginType.BRED, OriginType.WORLDGEN, OriginType.ZOMBIE_CONVERTED, OriginType.UNKNOWN}, values);
    }

}
