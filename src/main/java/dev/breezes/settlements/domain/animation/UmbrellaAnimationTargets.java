package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UmbrellaAnimationTargets {

    public static final AnimationTarget<Vec3> CANOPY_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:umbrella_canopy.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> NORTH_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_north.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> NORTH_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:umbrella_north.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    // Unit-neutral multiplication preserves the authored mesh scale when the target is absent.
    public static final AnimationTarget<Vector3f> NORTH_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_north.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    public static final AnimationTarget<Vector3f> SOUTH_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_south.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> SOUTH_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:umbrella_south.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> SOUTH_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_south.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    public static final AnimationTarget<Vector3f> EAST_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_east.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> EAST_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:umbrella_east.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> EAST_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_east.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

    public static final AnimationTarget<Vector3f> WEST_ROTATION = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_west.rotation")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f())
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vec3> WEST_TRANSLATION = AnimationTarget.<Vec3>builder()
            .id("model_part:umbrella_west.translation")
            .valueType(Vec3.class)
            .neutralValue(Vec3.ZERO)
            .interpolator(Interpolators.VEC3)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    public static final AnimationTarget<Vector3f> WEST_SCALE = AnimationTarget.<Vector3f>builder()
            .id("model_part:umbrella_west.scale")
            .valueType(Vector3f.class)
            .neutralValue(new Vector3f(1.0F, 1.0F, 1.0F))
            .interpolator(Interpolators.VECTOR3F)
            .policy(AnimationTargetPolicy.MULTIPLICATIVE)
            .build();

}
