package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class EatingPoses {

    // Arm raised close to the mouth, food hand approaching face
    public static final Pose ARMS_EAT_HIGH = arms(-60.0F);
    // Subtle dip between chew cycles
    public static final Pose ARMS_EAT_LOW = arms(-50.0F);

    private static Pose arms(float pitchDegrees) {
        return Pose.of(AnimationTargets.ARMS_ROTATION, RotationUtil.degrees(pitchDegrees, 0.0F, 0.0F));
    }

}
