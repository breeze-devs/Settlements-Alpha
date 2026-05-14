package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultAnimationResolverTest {

    @Test
    void resolve_delegatesToLibraryUsingMainHandCategory() {
        // Arrange
        KeyframeAnimation animation = mock(KeyframeAnimation.class);
        AnimationLibrary library = mock(AnimationLibrary.class);
        when(library.resolve(AnimationArchetype.SWING_LIGHT, ItemCategory.SWORD)).thenReturn(animation);
        DefaultAnimationResolver resolver = new DefaultAnimationResolver(library);

        // Act
        KeyframeAnimation resolved = resolver.resolve(
                AnimationArchetype.SWING_LIGHT,
                new AnimationSelectionContext(ItemCategory.SWORD));

        // Assert
        assertSame(animation, resolved);
    }

}
