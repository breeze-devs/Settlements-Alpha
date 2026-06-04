package dev.breezes.settlements.application.ai.courtship;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourtshipBeatTest {

    @Test
    void motionFor_returnsPresenterMotionForPresenter() {
        // Arrange
        CourtshipBeat beat = new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.POINT, null);

        // Act
        AnimationArchetype motion = beat.motionFor(CourtshipRole.PRESENTER);

        // Assert
        assertEquals(AnimationArchetype.INTERACT, motion);
    }

    @Test
    void motionFor_returnsReceiverMotionForReceiver() {
        // Arrange
        CourtshipBeat beat = new CourtshipBeat(AnimationArchetype.INTERACT, AnimationArchetype.POINT, null);

        // Act
        AnimationArchetype motion = beat.motionFor(CourtshipRole.RECEIVER);

        // Assert
        assertEquals(AnimationArchetype.POINT, motion);
    }

}
