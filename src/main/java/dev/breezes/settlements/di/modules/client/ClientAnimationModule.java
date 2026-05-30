package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.di.ClientSessionScope;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationKey;
import dev.breezes.settlements.domain.animation.AnimationLibrary;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.CartographerAnimations;
import dev.breezes.settlements.domain.animation.DefaultAnimationResolver;
import dev.breezes.settlements.domain.animation.EatingAnimations;
import dev.breezes.settlements.domain.animation.FishingAnimations;
import dev.breezes.settlements.domain.animation.InMemoryAnimationLibrary;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.animation.SwingAnimations;
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
                .animations(Map.ofEntries(
                        Map.entry(AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.GENERIC), SwingAnimations.swingHeavy()),
                        Map.entry(AnimationKey.of(AnimationArchetype.CAST, ItemCategory.FISHING_ROD), FishingAnimations.cast()),
                        Map.entry(AnimationKey.of(AnimationArchetype.REEL_OUT, ItemCategory.FISHING_ROD), FishingAnimations.jigFight()),
                        Map.entry(AnimationKey.of(AnimationArchetype.REEL_IN, ItemCategory.FISHING_ROD), FishingAnimations.reelYank()),
                        Map.entry(AnimationKey.of(AnimationArchetype.INTERACT, ItemCategory.GENERIC), InteractAnimations.interact()),
                        Map.entry(AnimationKey.of(AnimationArchetype.SURVEY_WITH_SPYGLASS, ItemCategory.SPYGLASS), CartographerAnimations.surveyWithSpyglass()),
                        Map.entry(AnimationKey.of(AnimationArchetype.WRITE_TO_MAP, ItemCategory.MAP), CartographerAnimations.markMap()),
                        Map.entry(AnimationKey.of(AnimationArchetype.EAT, ItemCategory.GENERIC), EatingAnimations.eat())))
                .build();
    }

}
