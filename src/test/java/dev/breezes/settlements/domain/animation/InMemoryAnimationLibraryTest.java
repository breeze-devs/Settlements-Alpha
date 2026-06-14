package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;

class InMemoryAnimationLibraryTest {

    @Test
    void resolve_returnsExactAnimationBeforeGenericFallback() {
        // Arrange
        KeyframeAnimation exact = animation("exact");
        KeyframeAnimation generic = animation("generic");
        InMemoryAnimationLibrary library = InMemoryAnimationLibrary.builder()
                .animations(Map.of(
                        AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.AXE), exact,
                        AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.GENERIC), generic))
                .build();

        // Act
        KeyframeAnimation resolved = library.resolve(AnimationArchetype.SWING_HEAVY, ItemCategory.AXE);

        // Assert
        assertSame(exact, resolved);
    }

    @Test
    void resolve_fallsBackToArchetypeGenericAnimation() {
        // Arrange
        KeyframeAnimation generic = animation("generic");
        InMemoryAnimationLibrary library = InMemoryAnimationLibrary.builder()
                .animations(Map.of(AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.GENERIC), generic))
                .build();

        // Act
        KeyframeAnimation resolved = library.resolve(AnimationArchetype.SWING_HEAVY, ItemCategory.MACE);

        // Assert
        assertSame(generic, resolved);
    }

    @Test
    void resolve_fallsBackToIdleAnimationWhenArchetypeIsMissing() {
        // Arrange
        KeyframeAnimation idle = animation("idle");
        InMemoryAnimationLibrary library = InMemoryAnimationLibrary.builder()
                .animations(Map.of(AnimationKey.of(AnimationArchetype.IDLE, ItemCategory.GENERIC), idle))
                .build();

        // Act
        KeyframeAnimation resolved = library.resolve(AnimationArchetype.POINT, ItemCategory.GENERIC);

        // Assert
        assertSame(idle, resolved);
    }

    private static KeyframeAnimation animation(String name) {
        return KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/" + name))
                .durationTicks(10)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(List.of())
                .build();
    }

}
