package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwingPoses {

    public static final Pose ARMS_REST = arms(0.0F);
    public static final Pose ARMS_RAISED = arms(-40.0F);
    public static final Pose ARMS_IMPACT = arms(35.0F);

    private static Pose arms(float pitchDegrees) {
        return Pose.of(AnimationTargets.ARMS_ROTATION, RotationUtil.degrees(pitchDegrees, 0.0F, 0.0F));
    }

}
