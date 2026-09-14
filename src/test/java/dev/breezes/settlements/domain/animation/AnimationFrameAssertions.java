package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationFrameAssertions {

    // Loose enough for a value interpolated onto a keyframe to match that keyframe, far tighter than anything visible
    private static final double TOLERANCE = 1.0E-4;

    /**
     * Every target any of the clips keys, each once.
     */
    public static List<AnimationTarget<?>> targetsKeyedBy(KeyframeAnimation... clips) {
        Set<AnimationTarget<?>> targets = new LinkedHashSet<>();
        for (KeyframeAnimation clip : clips) {
            for (AnimationTrack<?> track : clip.getTracks()) {
                targets.add(track.getTarget());
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Asserts the two frames pose each target alike. A target a frame does not hold reads as its neutral value, which
     * is what a consumer applying the frame draws for it.
     */
    public static void assertSamePose(Collection<AnimationTarget<?>> targets,
                                      AnimationFrame expected,
                                      AnimationFrame actual,
                                      String context) {
        for (AnimationTarget<?> target : targets) {
            Object expectedValue = expected.get(target);
            Object actualValue = actual.get(target);
            assertTrue(distance(expectedValue, actualValue) <= TOLERANCE,
                    () -> context + ": " + target.getId() + " expected " + expectedValue + " but was " + actualValue);
        }
    }

    private static double distance(Object expected, Object actual) {
        if (expected instanceof Vector3f expectedVector && actual instanceof Vector3f actualVector) {
            return expectedVector.distance(actualVector);
        }
        if (expected instanceof Vec3 expectedVector && actual instanceof Vec3 actualVector) {
            return expectedVector.distanceTo(actualVector);
        }
        throw new IllegalArgumentException("No distance between " + expected.getClass() + " and " + actual.getClass());
    }

}
