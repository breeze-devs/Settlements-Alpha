package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The ballista bones its clips drive, each value an offset from the bone's rest pose.
 * <p>
 * The aim bone has no target: aim is applied procedurally and never enters a clip.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaAnimationTargets {

    public static final AnimationTarget<Vector3f> GIMBAL_ROTATION = rotation("gimbal");
    public static final AnimationTarget<Vec3> GIMBAL_TRANSLATION = translation("gimbal");

    public static final AnimationTarget<Vector3f> LIMB_LEFT_OUTER_ROTATION = rotation("limb_left_outer");
    public static final AnimationTarget<Vector3f> LIMB_LEFT_TIP_ROTATION = rotation("limb_left_tip");
    public static final AnimationTarget<Vector3f> STRING_LEFT_ROTATION = rotation("string_left");

    public static final AnimationTarget<Vector3f> LIMB_RIGHT_OUTER_ROTATION = rotation("limb_right_outer");
    public static final AnimationTarget<Vector3f> LIMB_RIGHT_TIP_ROTATION = rotation("limb_right_tip");
    public static final AnimationTarget<Vector3f> STRING_RIGHT_ROTATION = rotation("string_right");

    public static final AnimationTarget<Vec3> PUSHER_TRANSLATION = translation("pusher");

    public static final AnimationTarget<Vector3f> CRANKSHAFT_ROTATION = rotation("crankshaft");

    // Unit-neutral multiplication preserves the authored rope length when the target is absent
    public static final AnimationTarget<Vector3f> CONNECTOR_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:ballista_connector.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    private static AnimationTarget<Vector3f> rotation(String bone) {
        return AnimationTarget.<Vector3f>builder()
                .id("model_part:ballista_" + bone + ".rotation")
                .valueType(Vector3f.class)
                .neutralValue(new Vector3f())
                .interpolator(Interpolators.VECTOR3F)
                .policy(AnimationTargetPolicy.ADDITIVE)
                .build();
    }

    private static AnimationTarget<Vec3> translation(String bone) {
        return AnimationTarget.<Vec3>builder()
                .id("model_part:ballista_" + bone + ".translation")
                .valueType(Vec3.class)
                .neutralValue(Vec3.ZERO)
                .interpolator(Interpolators.VEC3)
                .policy(AnimationTargetPolicy.ADDITIVE)
                .build();
    }

}
