package dev.breezes.settlements.domain.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoseTest {

    @Test
    void withTarget_addsTypedTargetValue() {
        // Arrange
        Pose pose = Pose.EMPTY;

        // Act
        Pose resolved = pose.with(AnimationTestTargets.FLOAT, 3.0F);

        // Assert
        assertTrue(resolved.find(AnimationTestTargets.FLOAT).isPresent());
        assertEquals(3.0F, resolved.find(AnimationTestTargets.FLOAT).orElseThrow(), 0.0001F);
        assertTrue(resolved.targets().contains(AnimationTestTargets.FLOAT));
        assertFalse(pose.targets().contains(AnimationTestTargets.FLOAT));
    }

    @Test
    void find_returnsEmptyWhenTargetIsMissing() {
        // Arrange
        Pose pose = Pose.of(AnimationTestTargets.FLOAT, 1.0F);

        // Act
        boolean present = pose.find(AnimationTestTargets.OTHER_FLOAT).isPresent();

        // Assert
        assertFalse(present);
    }

    @Test
    void withPose_mergesDisjointTargets() {
        // Arrange
        Pose base = Pose.of(AnimationTestTargets.FLOAT, 1.0F);
        Pose overlay = Pose.of(AnimationTestTargets.OTHER_FLOAT, 2.0F);

        // Act
        Pose resolved = base.with(overlay);

        // Assert
        assertEquals(1.0F, resolved.find(AnimationTestTargets.FLOAT).orElseThrow(), 0.0001F);
        assertEquals(2.0F, resolved.find(AnimationTestTargets.OTHER_FLOAT).orElseThrow(), 0.0001F);
    }

    @Test
    void withPose_overlayWinsOnOverlappingTargets() {
        // Arrange
        Pose base = Pose.of(AnimationTestTargets.FLOAT, 1.0F);
        Pose overlay = Pose.of(AnimationTestTargets.FLOAT, 2.0F);

        // Act
        Pose resolved = base.with(overlay);

        // Assert
        assertEquals(2.0F, resolved.find(AnimationTestTargets.FLOAT).orElseThrow(), 0.0001F);
    }

}
