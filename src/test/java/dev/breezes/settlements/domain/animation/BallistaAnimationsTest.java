package dev.breezes.settlements.domain.animation;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.breezes.settlements.domain.animation.AnimationFrameAssertions.assertSamePose;
import static dev.breezes.settlements.domain.animation.AnimationFrameAssertions.targetsKeyedBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaAnimationsTest {

    private static final KeyframeAnimation WIND = BallistaAnimations.wind();
    private static final KeyframeAnimation FIRE = BallistaAnimations.fire();

    private static final double AT_REST_TOLERANCE = 1.0E-4;

    @Test
    void fire_startsOnThePoseWindEndsOn_forEveryTargetEitherClipKeys() {
        // Arrange: a target only one clip keys reads as neutral in the other, which is what the machine draws for that
        // bone once the clip that does not key it takes over
        List<AnimationTarget<?>> targets = targetsKeyedBy(WIND, FIRE);

        // Act
        AnimationFrame windEnd = WIND.sample(WIND.getDurationTicks());
        AnimationFrame fireStart = FIRE.sample(0.0F);

        // Assert: any difference is a snap on the tick a cocked machine fires
        assertSamePose(targets, windEnd, fireStart, "fire's first frame against wind's last");
    }

    @Test
    void wind_startsOnTheRestPose() {
        // Act
        AnimationFrame windStart = WIND.sample(0.0F);

        // Assert: an unwound machine holds the rest pose, so any offset here is a snap on the tick a wind starts
        assertSamePose(targetsKeyedBy(WIND), AnimationFrame.EMPTY, windStart, "wind's first frame against rest");
    }

    @Test
    void fire_endsOnTheRestPose() {
        // Act
        AnimationFrame fireEnd = FIRE.sample(FIRE.getDurationTicks());

        // Assert: a fired machine holds the rest pose, so any offset here is a snap on the tick the clip runs out
        assertSamePose(targetsKeyedBy(FIRE), AnimationFrame.EMPTY, fireEnd, "fire's last frame against rest");
    }

    @Test
    void fire_pusherReachesRest_onTheReleaseTickAndNoSooner() {
        // Act, Assert: a release tick before the pusher arrives launches the bolt while the seated one is still drawn
        // travelling forward, and one after it leaves the seated bolt parked at rest
        for (int tick = 0; tick < BallistaAnimations.FIRE_RELEASE_TICK; tick++) {
            assertFalse(pusherAtRest(FIRE.sample(tick)), "pusher already at rest on tick " + tick);
        }
        assertTrue(pusherAtRest(FIRE.sample(BallistaAnimations.FIRE_RELEASE_TICK)),
                "pusher not at rest on the release tick");
    }

    private static boolean pusherAtRest(AnimationFrame frame) {
        Vec3 offset = frame.get(BallistaAnimationTargets.PUSHER_TRANSLATION);
        return offset.length() <= AT_REST_TOLERANCE;
    }

}
