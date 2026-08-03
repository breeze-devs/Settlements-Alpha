package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationLayerTest {

    private static final int BLEND_IN_TICKS = 4;
    private static final int DURATION_TICKS = 10;
    private static final int BLEND_OUT_TICKS = 4;

    @Test
    void weight_risesAcrossBlendInForANewLayer() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("action", BLEND_IN_TICKS, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(animation, 0L);

        // Act
        float halfwayIn = layer.weight(BLEND_IN_TICKS / 2, 0.0F);
        float fullyIn = layer.weight(BLEND_IN_TICKS, 0.0F);

        // Assert
        assertEquals(0.5F, halfwayIn, 0.0001F);
        assertEquals(1.0F, fullyIn, 0.0001F);
    }

    @Test
    void weight_fallsFromItsCurrentWeightWhenClearedDuringBlendIn() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held(
                "sustained", BLEND_IN_TICKS, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(animation, 0L);
        long clearGameTime = 1L;
        float weightAtClear = layer.weight(clearGameTime, 0.0F);

        // Act
        layer.beginClear(clearGameTime);
        float nextWeight = layer.weight(clearGameTime + 1L, 0.0F);
        float laterWeight = layer.weight(clearGameTime + 2L, 0.0F);

        // Assert
        assertEquals(0.25F, weightAtClear, 0.0001F);
        assertTrue(nextWeight < weightAtClear);
        assertTrue(laterWeight < nextWeight);
    }

    @Test
    void weight_holdsFullThroughDurationThenFadesOutForATransientLayer() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("action", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(animation, 0L);

        // Act
        float atDuration = layer.weight(DURATION_TICKS, 0.0F);
        float halfwayOut = layer.weight(DURATION_TICKS + BLEND_OUT_TICKS / 2, 0.0F);
        float fullyOut = layer.weight(DURATION_TICKS + BLEND_OUT_TICKS, 0.0F);

        // Assert
        assertEquals(1.0F, atDuration, 0.0001F);
        assertEquals(0.5F, halfwayOut, 0.0001F);
        assertEquals(0.0F, fullyOut, 0.0001F);
    }

    @Test
    void weight_fadesAPersistentLayerOutOnlyOnceCleared() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("sustained", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(animation, 0L);
        long clearGameTime = DURATION_TICKS * 3L;

        // Act
        float wellPastDuration = layer.weight(clearGameTime, 0.0F);
        layer.beginClear(clearGameTime);
        float halfwayOut = layer.weight(clearGameTime + BLEND_OUT_TICKS / 2, 0.0F);
        float fullyOut = layer.weight(clearGameTime + BLEND_OUT_TICKS, 0.0F);

        // Assert
        assertEquals(1.0F, wellPastDuration, 0.0001F);
        assertEquals(0.5F, halfwayOut, 0.0001F);
        assertEquals(0.0F, fullyOut, 0.0001F);
    }

    @Test
    void weight_doesNotRecoverWhenClearedMidwayThroughItsOwnBlendOut() {
        // Arrange: a one-shot the behavior only returns to idle after the clip already ran out, so the
        // explicit clear lands on a layer that is halfway through fading on its own.
        KeyframeAnimation animation = AnimationTestClips.held("action", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(animation, 0L);
        long clearGameTime = DURATION_TICKS + BLEND_OUT_TICKS / 2;
        float weightBeforeClear = layer.weight(clearGameTime, 0.0F);

        // Act
        layer.beginClear(clearGameTime);
        float weightAfterClear = layer.weight(clearGameTime, 0.0F);

        // Assert: the clear must not restart the fade and hand the pose back at full strength.
        assertEquals(weightBeforeClear, weightAfterClear, 0.0001F);
        assertTrue(weightAfterClear < 1.0F);
    }

    @Test
    void beginClear_ignoresARepeatCallSoTheFadeKeepsRunningDown() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("sustained", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(animation, 0L);

        // Act
        layer.beginClear(0L);
        layer.beginClear(BLEND_OUT_TICKS / 2);

        // Assert: weight follows the first clear, so the fade is already half spent rather than restarted.
        assertEquals(0.5F, layer.weight(BLEND_OUT_TICKS / 2, 0.0F), 0.0001F);
    }

    @Test
    void replace_resumesFromTheWeightTheLayerAlreadyHeld() {
        // Arrange: a new action arrives while the outgoing one is halfway through its blend-out.
        KeyframeAnimation outgoing = AnimationTestClips.held("outgoing", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(outgoing, 0L);
        long replaceGameTime = DURATION_TICKS + BLEND_OUT_TICKS / 2;
        float weightBeforeReplace = layer.weight(replaceGameTime, 0.0F);

        // Act
        KeyframeAnimation incoming = AnimationTestClips.held("incoming", BLEND_IN_TICKS, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        layer.replace(incoming, replaceGameTime, true);
        float weightAtReplace = layer.weight(replaceGameTime, 0.0F);
        float weightPartwayIn = layer.weight(replaceGameTime + BLEND_IN_TICKS / 2, 0.0F);
        float weightFullyIn = layer.weight(replaceGameTime + BLEND_IN_TICKS, 0.0F);

        // Assert: it picks up where the fade left off and climbs, rather than snapping back to full.
        assertEquals(weightBeforeReplace, weightAtReplace, 0.0001F);
        assertTrue(weightPartwayIn > weightAtReplace);
        assertEquals(1.0F, weightFullyIn, 0.0001F);
    }

    @Test
    void replace_cancelsAPendingClear() {
        // Arrange
        KeyframeAnimation outgoing = AnimationTestClips.held("outgoing", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(outgoing, 0L);
        layer.beginClear(0L);

        // Act
        KeyframeAnimation incoming = AnimationTestClips.held("incoming", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        layer.replace(incoming, 0L, false);

        // Assert: the layer is wanted again, so it neither fades nor retires.
        assertEquals(1.0F, layer.weight(BLEND_OUT_TICKS * 2L, 0.0F), 0.0001F);
        assertFalse(layer.isExpired(BLEND_OUT_TICKS * 2L, 0.0F));
    }

    @Test
    void isExpired_retiresATransientLayerOnceItsDurationAndBlendOutAreSpent() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("action", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(animation, 0L);

        // Act, Assert
        assertFalse(layer.isExpired(DURATION_TICKS + BLEND_OUT_TICKS - 1, 0.0F));
        assertTrue(layer.isExpired(DURATION_TICKS + BLEND_OUT_TICKS, 0.0F));
    }

    @Test
    void isExpired_retiresAClipWithNoBlendOutOnlyOncePastItsDuration() {
        // Arrange: with nothing to fade across, the clip owns its final tick outright.
        KeyframeAnimation animation = AnimationTestClips.held("action", 0, DURATION_TICKS, 0, 1.0F);
        AnimationLayer layer = AnimationLayer.transientAction(animation, 0L);

        // Act, Assert
        assertFalse(layer.isExpired(DURATION_TICKS, 0.0F));
        assertTrue(layer.isExpired(DURATION_TICKS, 0.1F));
    }

    @Test
    void isExpired_retiresAClearedLayerOnceItsBlendOutIsSpent() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("sustained", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(animation, 0L);

        // Act
        layer.beginClear(0L);

        // Assert
        assertFalse(layer.isExpired(BLEND_OUT_TICKS - 1, 0.0F));
        assertTrue(layer.isExpired(BLEND_OUT_TICKS, 0.0F));
    }

    @Test
    void isExpired_neverRetiresAPersistentLayerThatWasNotCleared() {
        // Arrange
        KeyframeAnimation animation = AnimationTestClips.held("sustained", 0, DURATION_TICKS, BLEND_OUT_TICKS, 1.0F);
        AnimationLayer layer = AnimationLayer.persistent(animation, 0L);

        // Act, Assert
        assertFalse(layer.isExpired(DURATION_TICKS * 100L, 0.0F));
    }

}
