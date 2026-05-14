package dev.breezes.settlements.domain.animation;

final class AnimationTestTargets {

    static final AnimationTarget<Float> FLOAT = AnimationTarget.<Float>builder()
            .id("test:float")
            .valueType(Float.class)
            .neutralValue(0.0F)
            .interpolator(Interpolators.FLOAT)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    static final AnimationTarget<Float> OTHER_FLOAT = AnimationTarget.<Float>builder()
            .id("test:other_float")
            .valueType(Float.class)
            .neutralValue(0.0F)
            .interpolator(Interpolators.FLOAT)
            .policy(AnimationTargetPolicy.ADDITIVE)
            .build();

    private AnimationTestTargets() {
    }
}
