package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FishingPoses {

    public static final Pose REST = arms(0.0F);
    public static final Pose CAST_WIND_UP = arms(-40.0F);
    public static final Pose CAST_RELEASE = arms(30.0F);
    public static final Pose CAST_SETTLE = arms(12.0F);
    public static final Pose JIG_LOW = arms(18.0F);
    public static final Pose JIG_HIGH = arms(-15.0F);
    public static final Pose YANK_PEAK = arms(-55.0F);

    private static Pose arms(float pitchDegrees) {
        return Pose.of(AnimationTargets.ARMS_ROTATION, RotationUtil.degrees(pitchDegrees, 0.0F, 0.0F));
    }

}
