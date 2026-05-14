package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationTargets {

    public static final AnimationTarget<Vector3f> ARMS_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:arms.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> ARMS_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:arms.translation")
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

}
