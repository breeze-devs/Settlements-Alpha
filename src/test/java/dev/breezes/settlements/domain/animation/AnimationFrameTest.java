package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationFrameTest {

    @Test
    void get_returnsTargetNeutralWhenValueIsMissing() {
        // Arrange
        AnimationFrame frame = AnimationFrame.EMPTY;

        // Act
        float value = frame.get(AnimationTestTargets.FLOAT);

        // Assert
        assertEquals(0.0F, value, 0.0001F);
    }

    @Test
    void get_returnsProvidedFallbackWhenValueIsMissing() {
        // Arrange
        AnimationFrame frame = AnimationFrame.EMPTY;

        // Act
        float value = frame.get(AnimationTestTargets.FLOAT, 7.0F);

        // Assert
        assertEquals(7.0F, value, 0.0001F);
    }

    @Test
    void has_reportsWhetherTargetWasSampled() {
        // Arrange
        AnimationFrame frame = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 3.0F));

        // Act, Assert
        assertTrue(frame.has(AnimationTestTargets.FLOAT));
        assertFalse(frame.has(AnimationTestTargets.OTHER_FLOAT));
    }

    @Test
    void blendTo_usesNeutralForTargetsMissingOnEitherSide() {
        // Arrange
        AnimationFrame from = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 10.0F));
        AnimationFrame to = AnimationFrame.of(Map.of(AnimationTestTargets.OTHER_FLOAT, 20.0F));

        // Act
        AnimationFrame blended = from.blendTo(to, 0.5F);

        // Assert
        assertEquals(5.0F, blended.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(10.0F, blended.get(AnimationTestTargets.OTHER_FLOAT), 0.0001F);
    }
}
