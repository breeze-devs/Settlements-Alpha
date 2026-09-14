package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaAimGuideRendererTest {

    @Test
    void showingAlpha_isFull_untilTheFadeBegins() {
        // Arrange
        long fadeBegins = BallistaAimGuideRenderer.SHOWN_FOR_MILLIS - BallistaAimGuideRenderer.FADE_OUT_MILLIS;

        // Act, Assert
        assertEquals(1.0F, BallistaAimGuideRenderer.computeTimeOpacity(0L));
        assertEquals(1.0F, BallistaAimGuideRenderer.computeTimeOpacity(fadeBegins));
    }

    @Test
    void showingAlpha_fadesTowardTheEndOfTheShowing() {
        // Arrange
        long halfwayThroughTheFade = BallistaAimGuideRenderer.SHOWN_FOR_MILLIS
                - BallistaAimGuideRenderer.FADE_OUT_MILLIS / 2L;

        // Act
        float alpha = BallistaAimGuideRenderer.computeTimeOpacity(halfwayThroughTheFade);

        // Assert: a guide cut off at full strength would blink out rather than fade
        assertTrue(alpha > 0.0F && alpha < 1.0F, "alpha " + alpha);
    }

    @Test
    void showingAlpha_isNothing_onceTheShowingIsOver() {
        // Act, Assert
        assertEquals(0.0F, BallistaAimGuideRenderer.computeTimeOpacity(BallistaAimGuideRenderer.SHOWN_FOR_MILLIS));
        assertEquals(0.0F, BallistaAimGuideRenderer.computeTimeOpacity(BallistaAimGuideRenderer.SHOWN_FOR_MILLIS * 10L));
    }

}
