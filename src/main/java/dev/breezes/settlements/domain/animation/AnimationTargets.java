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

    // Head and leg rotations seize their bone rather than adding to it, so a clip can suppress vanilla
    // look-tracking or the walk-swing outright. They are coverage-tracked so that seizure fades in and
    // out with the owning layer instead of wrenching the bone off whatever pose it was already holding.
    public static final AnimationTarget<Vector3f> HEAD_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:head.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.BLENDED_ABSOLUTE)
            .build();

    public static final AnimationTarget<Vec3> HEAD_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:head.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
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

    public static final AnimationTarget<Vector3f> LEG_LEFT_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:leg_left.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.BLENDED_ABSOLUTE)
            .build();

    public static final AnimationTarget<Vector3f> LEG_RIGHT_ROTATION_OVERRIDE = AnimationTarget.<Vector3f>builder()
            .id("model_part:leg_right.rotation_override")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.BLENDED_ABSOLUTE)
            .build();

    public static final AnimationTarget<Vec3> LEG_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:leg_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> LEG_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:leg_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Root motion displaces and rotates the whole entity in model space, for effects like impact recoil
    // or a hover bob that no single bone can carry.
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

    // TODO: validate the face target ids and axes below against the first authored face animation, then
    //  rename them or add rotation variants to match whatever convention that clip settles on.
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

    public static final AnimationTarget<Vector3f> MOUTH_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:mouth.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Unit-neutral multiplication preserves the authored mesh scale when the target is absent.
    public static final AnimationTarget<Vector3f> MOUTH_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:mouth.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
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

    // The eyeball sits between the eyelid and the pupil in the rig, so scaling it squashes the visible
    // eye while carrying the pupil along with it — that is how the clips author squints and wide eyes.
    public static final AnimationTarget<Vec3> EYEBALL_LEFT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:eyeball_left.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> EYEBALL_RIGHT_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:eyeball_right.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> EYEBALL_LEFT_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:eyeball_left.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    public static final AnimationTarget<Vector3f> EYEBALL_RIGHT_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:eyeball_right.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
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

    public static final AnimationTarget<Vector3f> PUPIL_LEFT_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:pupil_left.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    public static final AnimationTarget<Vector3f> PUPIL_RIGHT_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:pupil_right.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

}
