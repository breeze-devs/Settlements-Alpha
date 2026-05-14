package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EasingTest {

    @Test
    void apply_clampsProgressBeforeEasing() {
        // Arrange, Act, Assert
        assertEquals(0.0F, Easing.LINEAR.apply(-1.0F), 0.0001F);
        assertEquals(1.0F, Easing.LINEAR.apply(2.0F), 0.0001F);
    }

    @Test
    void easeInOut_isSymmetricAroundHalfProgress() {
        // Arrange
        float early = Easing.EASE_IN_OUT.apply(0.25F);
        float late = Easing.EASE_IN_OUT.apply(0.75F);

        // Act, Assert
        assertEquals(early, 1.0F - late, 0.0001F);
    }

    @Test
    void step_holdsUntilEndOfSegment() {
        // Arrange, Act, Assert
        assertEquals(0.0F, Easing.STEP.apply(0.99F), 0.0001F);
        assertEquals(1.0F, Easing.STEP.apply(1.0F), 0.0001F);
    }
}
