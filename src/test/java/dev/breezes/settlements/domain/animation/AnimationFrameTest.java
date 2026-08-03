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

    @Test
    void composeOver_blendedAbsolutePreservesAuthoredValueAndTracksApplicationWeight() {
        // Arrange
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 12.0F));

        // Act
        AnimationFrame composed = AnimationFrame.EMPTY.composeOver(over, 0.5F);

        // Assert
        assertEquals(12.0F, composed.get(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
        assertEquals(0.5F, composed.applicationWeight(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void composeOver_fullWeightOnAnEmptyFrameYieldsTheOverlayUnchanged() {
        // Arrange: one target per policy, because the shortcut for this case has to agree with the
        // general fold for every one of them or a layer pops the frame its weight reaches full.
        AnimationFrame over = AnimationFrame.of(Map.of(
                AnimationTestTargets.FLOAT, 12.0F,
                AnimationTestTargets.MULTIPLICATIVE_FLOAT, 3.0F,
                AnimationTestTargets.ABSOLUTE_FLOAT, 7.0F,
                AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 5.0F));

        // Act
        AnimationFrame composed = AnimationFrame.EMPTY.composeOver(over, 1.0F);

        // Assert
        assertEquals(12.0F, composed.get(AnimationTestTargets.FLOAT), 0.0001F);
        assertEquals(3.0F, composed.get(AnimationTestTargets.MULTIPLICATIVE_FLOAT), 0.0001F);
        assertEquals(7.0F, composed.get(AnimationTestTargets.ABSOLUTE_FLOAT), 0.0001F);
        assertEquals(5.0F, composed.get(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
        assertEquals(1.0F, composed.applicationWeight(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void composeOver_approachesTheOverlayContinuouslyAsWeightReachesFull() {
        // Arrange: fading a base out as an overlay fades in is how the sleep transition is folded, so the
        // full-weight shortcut has to be the limit of the general fold rather than a separate answer.
        AnimationFrame base = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 10.0F));
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.FLOAT, 12.0F));

        // Act
        AnimationFrame nearlyFull = AnimationFrame.EMPTY
                .composeOver(base, 0.01F)
                .composeOver(over, 0.99F);
        AnimationFrame full = AnimationFrame.EMPTY
                .composeOver(base, 0.0F)
                .composeOver(over, 1.0F);

        // Assert
        assertEquals(full.get(AnimationTestTargets.FLOAT), nearlyFull.get(AnimationTestTargets.FLOAT), 0.2F);
    }

    @Test
    void composeOver_leavesAnUncoveredTargetOutOfTheFrameEntirely() {
        // Arrange: coverage of zero is never stored, so a target nothing owns stays absent rather than
        // reading as present-but-invisible to the applier.
        AnimationFrame over = AnimationFrame.of(Map.of(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 12.0F));

        // Act
        AnimationFrame blended = over.blendTo(AnimationFrame.EMPTY, 1.0F);

        // Assert
        assertFalse(blended.has(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT));
        assertEquals(0.0F, blended.applicationWeight(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void composeOver_blendedAbsoluteUsesSourceOverCoverage() {
        // Arrange
        AnimationFrame baseValue = AnimationFrame.of(Map.of(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 4.0F));
        AnimationFrame overValue = AnimationFrame.of(Map.of(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 12.0F));
        AnimationFrame partialBase = AnimationFrame.EMPTY.composeOver(baseValue, 0.5F);

        // Act
        AnimationFrame composed = partialBase.composeOver(overValue, 0.5F);

        // Assert
        assertEquals(9.3333F, composed.get(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
        assertEquals(0.75F, composed.applicationWeight(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
    }

    @Test
    void blendTo_missingBlendedAbsoluteFadesCoverageWithoutChangingAuthoredValue() {
        // Arrange
        AnimationFrame from = AnimationFrame.of(Map.of(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT, 12.0F));

        // Act
        AnimationFrame blended = from.blendTo(AnimationFrame.EMPTY, 0.5F);

        // Assert
        assertEquals(12.0F, blended.get(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
        assertEquals(0.5F, blended.applicationWeight(AnimationTestTargets.BLENDED_ABSOLUTE_FLOAT), 0.0001F);
    }
}
