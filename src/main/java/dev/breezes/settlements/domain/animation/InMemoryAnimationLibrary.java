package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.Builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public final class InMemoryAnimationLibrary implements AnimationLibrary {

    private static final KeyframeAnimation DEFAULT_IDLE = KeyframeAnimation.builder()
            .id(ResourceLocationUtil.mod("animation/idle"))
            .durationTicks(0)
            .loopMode(LoopMode.ONCE)
            .blendInTicks(0)
            .blendOutTicks(0)
            .tracks(List.of())
            .build();

    private final Map<AnimationKey, KeyframeAnimation> animations;
    private final KeyframeAnimation idleAnimation;

    @Builder
    private InMemoryAnimationLibrary(@Nonnull Map<AnimationKey, KeyframeAnimation> animations,
                                     @Nullable KeyframeAnimation idleAnimation) {
        this.animations = Map.copyOf(animations);
        this.idleAnimation = idleAnimation == null ? DEFAULT_IDLE : idleAnimation;
    }

    public static InMemoryAnimationLibrary empty() {
        return InMemoryAnimationLibrary.builder()
                .animations(Map.of())
                .idleAnimation(DEFAULT_IDLE)
                .build();
    }

    @Override
    public KeyframeAnimation resolve(@Nonnull AnimationArchetype archetype, @Nonnull ItemCategory category) {
        KeyframeAnimation exact = this.animations.get(AnimationKey.of(archetype, category));
        if (exact != null) {
            return exact;
        }

        KeyframeAnimation generic = this.animations.get(AnimationKey.of(archetype, ItemCategory.GENERIC));
        if (generic != null) {
            return generic;
        }

        KeyframeAnimation idle = this.animations.get(AnimationKey.of(AnimationArchetype.IDLE, ItemCategory.GENERIC));
        return idle == null ? this.idleAnimation : idle;
    }

}
