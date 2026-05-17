package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartographerPoses {

    // ---- Shared neutral ----
    public static final Pose ARMS_REST = armsPitch(0);
    public static final Pose HEAD_NEUTRAL = head(0, 0);
    public static final Pose REACH_NONE = reach(Vec3.ZERO);

    // ---- Spyglass survey ----
    private static final float SPYGLASS_ARM_PITCH = -60.0F;
    private static final float SPYGLASS_HEAD_PITCH = -10.0F;
    private static final float SPYGLASS_HEAD_YAW = 25.0F;
    private static final float SPYGLASS_ARM_YAW = 25.0F;
    private static final double SPYGLASS_RIGHT_EYE_X = 0.0D;
    private static final double SPYGLASS_LIFT_Y = -2.2D;
    private static final double SPYGLASS_FORWARD_Z = -0.3D;

    private static final Pose SPYGLASS_HOLD = armsTranslation(SPYGLASS_RIGHT_EYE_X, SPYGLASS_LIFT_Y, SPYGLASS_FORWARD_Z);

    public static final Pose LOOK_CENTER = headAndArms(0.0F, 0.0F).with(SPYGLASS_HOLD);
    public static final Pose LOOK_LEFT = headAndArms(-SPYGLASS_HEAD_YAW, -SPYGLASS_ARM_YAW).with(SPYGLASS_HOLD);
    public static final Pose LOOK_RIGHT = headAndArms(SPYGLASS_HEAD_YAW, SPYGLASS_ARM_YAW).with(SPYGLASS_HOLD);

    // ---- Map marking ----
    // Arms held at chest level, looking down at the map
    public static final Pose MAP_ARMS = armsPitch(10);
    public static final Pose HEAD_DOWN = head(25, 0);
    // Slight forward reach on the writing hand
    public static final Pose MARK_REACH = reach(new Vec3(0, 0, -0.6));

    private static Pose headAndArms(float headYawDegrees, float armYawDegrees) {
        return head(SPYGLASS_HEAD_PITCH, headYawDegrees)
                .with(armsRotation(SPYGLASS_ARM_PITCH, armYawDegrees));
    }

    private static Pose armsPitch(float pitchDegrees) {
        return armsRotation(pitchDegrees, 0.0F);
    }

    private static Pose armsRotation(float pitchDegrees, float yawDegrees) {
        return Pose.of(AnimationTargets.ARMS_ROTATION, RotationUtil.degrees(pitchDegrees, yawDegrees, 0));
    }

    private static Pose head(float pitch, float yaw) {
        return Pose.of(AnimationTargets.HEAD_ROTATION_OVERRIDE, RotationUtil.degrees(pitch, yaw, 0));
    }

    private static Pose reach(Vec3 translation) {
        return Pose.of(AnimationTargets.ARMS_TRANSLATION, translation);
    }

    private static Pose armsTranslation(double x, double y, double z) {
        return reach(new Vec3(x, y, z));
    }

}
