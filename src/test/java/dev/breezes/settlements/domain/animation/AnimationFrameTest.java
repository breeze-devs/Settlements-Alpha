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

    @Test
    void composeOver_additivePolicyAddsWeightedDeltaFromNeutral() {
        // Arrange
        AnimationFrame base = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 10.0F));
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 6.0F));

        // Act
        AnimationFrame composed = base.composeOver(over, 0.5F);

        // Assert
        assertEquals(13.0F, composed.get(AnimationTestTargets.FLOAT), 0.0001F);
    }

    @Test
    void composeOver_multiplicativePolicyAppliesWeightedFactor() {
        // Arrange
        AnimationFrame base = AnimationFrame.of(Map.of(AnimationTestTargets.MULTIPLICATIVE_FLOAT, 4.0F));
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.MULTIPLICATIVE_FLOAT, 3.0F));

        // Act
        AnimationFrame composed = base.composeOver(over, 0.5F);

        // Assert
        assertEquals(8.0F, composed.get(AnimationTestTargets.MULTIPLICATIVE_FLOAT), 0.0001F);
    }

    @Test
    void composeOver_absolutePolicyOnlyMarksTargetsPresentInOverlay() {
        // Arrange
        AnimationFrame base = AnimationFrame.EMPTY;
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.ABSOLUTE_FLOAT, 12.0F));

        // Act
        AnimationFrame composed = base.composeOver(over, 0.5F);

        // Assert
        assertTrue(composed.has(AnimationTestTargets.ABSOLUTE_FLOAT));
        assertEquals(6.0F, composed.get(AnimationTestTargets.ABSOLUTE_FLOAT), 0.0001F);
        assertFalse(base.has(AnimationTestTargets.ABSOLUTE_FLOAT));
    }
}
