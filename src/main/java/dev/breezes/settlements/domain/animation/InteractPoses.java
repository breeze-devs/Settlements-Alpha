package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InteractPoses {

    public static final Pose REST = arms(0.0F);
    public static final Pose INTERACT_PEAK = arms(-30.0F);

    private static Pose arms(float pitchDegrees) {
        return Pose.of(AnimationTargets.ARMS_CROSSED_ROTATION, RotationUtil.degrees(pitchDegrees, 0.0F, 0.0F));
    }

}
