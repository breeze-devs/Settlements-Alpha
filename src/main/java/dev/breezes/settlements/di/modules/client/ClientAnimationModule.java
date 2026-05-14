package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.di.ClientSessionScope;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationKey;
import dev.breezes.settlements.domain.animation.AnimationLibrary;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.ButcheringAnimations;
import dev.breezes.settlements.domain.animation.DefaultAnimationResolver;
import dev.breezes.settlements.domain.animation.InMemoryAnimationLibrary;
import dev.breezes.settlements.domain.presentation.ItemCategory;

import java.util.Map;

@Module
public abstract class ClientAnimationModule {

    @Binds
    abstract AnimationResolver animationResolver(DefaultAnimationResolver implementation);

    @Provides
    @ClientSessionScope
    static AnimationLibrary animationLibrary() {
        return InMemoryAnimationLibrary.builder()
                .animations(Map.of(
                        AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.AXE), ButcheringAnimations.swingHeavyAxe()))
                .build();
    }

}
