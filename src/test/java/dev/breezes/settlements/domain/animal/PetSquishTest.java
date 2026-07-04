package dev.breezes.settlements.domain.animal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PetSquishTest {

    @Test
    void factorsAt_beforeAndAfterWindow_isIdentity() {
        // Arrange, Act, Assert
        assertSame(PetSquish.IDENTITY, PetSquish.factorsAt(-1.0F));
        assertSame(PetSquish.IDENTITY, PetSquish.factorsAt(PetSquish.DURATION_TICKS));
        assertSame(PetSquish.IDENTITY, PetSquish.factorsAt(PetSquish.DURATION_TICKS + 5.0F));
    }

    @Test
    void factorsAt_start_isRest() {
        // Arrange, Act
        PetSquish.Factors factors = PetSquish.factorsAt(0.0F);

        // Assert -- the damped sine begins at zero so there is no pop on the first frame
        assertEquals(1.0F, factors.xz(), 0.0001F);
        assertEquals(1.0F, factors.y(), 0.0001F);
    }

    @Test
    void factorsAt_earlyImpact_flattensAndWidens() {
        // Arrange -- a quarter through the first half-bounce is near the peak squash
        float elapsed = PetSquish.DURATION_TICKS / 6.0F;

        // Act
        PetSquish.Factors factors = PetSquish.factorsAt(elapsed);

        // Assert -- squashed animals are shorter than rest and wider than rest
        assertTrue(factors.y() < 1.0F, "expected vertical flattening, got " + factors.y());
        assertTrue(factors.xz() > 1.0F, "expected horizontal widening, got " + factors.xz());
    }

    @Test
    void factorsAt_rebound_stretchesTallerThanRest() {
        // Arrange -- past the first zero crossing the spring overshoots upward (stretch)
        float elapsed = PetSquish.DURATION_TICKS * 0.5F;

        // Act
        PetSquish.Factors factors = PetSquish.factorsAt(elapsed);

        // Assert
        assertTrue(factors.y() > 1.0F, "expected rebound stretch, got " + factors.y());
        assertTrue(factors.xz() < 1.0F, "expected girth pinch on stretch, got " + factors.xz());
    }

    @Test
    void factorsAt_scaleFactorsStayPositiveAcrossWindow() {
        // Arrange, Act, Assert -- a negative or zero scale would invert or collapse the model
        for (float elapsed = 0.0F; elapsed < PetSquish.DURATION_TICKS; elapsed += 0.1F) {
            PetSquish.Factors factors = PetSquish.factorsAt(elapsed);
            assertTrue(factors.xz() > 0.0F && factors.y() > 0.0F,
                    "non-positive scale at elapsed=" + elapsed + " -> " + factors);
        }
    }

}
