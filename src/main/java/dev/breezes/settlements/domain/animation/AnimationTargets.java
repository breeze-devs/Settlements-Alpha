package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationTargets {

    public static final AnimationTarget<Vector3f> ARMS_CROSSED_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arms_crossed.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARMS_CROSSED_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arms_crossed.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ARMS_STRAIGHT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arms_straight.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARMS_STRAIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arms_straight.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ARM_CROSSED_LEFT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arm_crossed_left.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARM_CROSSED_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arm_crossed_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ARM_CROSSED_RIGHT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arm_crossed_right.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARM_CROSSED_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arm_crossed_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ARM_STRAIGHT_LEFT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arm_straight_left.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARM_STRAIGHT_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arm_straight_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ARM_STRAIGHT_RIGHT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arm_straight_right.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARM_STRAIGHT_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arm_straight_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> HEAD_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:head.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ABSOLUTE)
            .build();

    public static final AnimationTarget<Vector3f> BODY_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:body.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> BODY_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:body.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Nose secondary-motion targets
    public static final AnimationTarget<Vector3f> NOSE_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:nose.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> NOSE_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:nose.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Leg overrides use ABSOLUTE policy (not additive) so an animation can suppress the vanilla walk-swing entirely
    public static final AnimationTarget<Vector3f> LEG_LEFT_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:leg_left.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ABSOLUTE)
            .build();

    public static final AnimationTarget<Vector3f> LEG_RIGHT_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:leg_right.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ABSOLUTE)
            .build();

    // Root motion targets let animations displace and rotate the entire entity in model space,
    // enabling effects like ground-pound impact recoil, kill-aura spin, or hover bob without
    // touching individual bone positions.
    public static final AnimationTarget<Vec3> ROOT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:root.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> ROOT_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:root.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Face expression targets for boss/emote animations.
    // PROVISIONAL: these ids and axes need validation against the first authored face animation;
    // rename or add rotation variants once a real authored clip confirms the right convention.
    public static final AnimationTarget<Vec3> MONOBROW_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:monobrow.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> MONOBROW_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:monobrow.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> MOUTH_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:mouth.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> EYELID_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:eyelid_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> EYELID_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:eyelid_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> PUPIL_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:pupil_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> PUPIL_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:pupil_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

}
