package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.di.ClientSessionScope;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationKey;
import dev.breezes.settlements.domain.animation.AnimationLibrary;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.CartographerAnimations;
import dev.breezes.settlements.domain.animation.ChopAnimations;
import dev.breezes.settlements.domain.animation.DefaultAnimationResolver;
import dev.breezes.settlements.domain.animation.DefaultIdleLifeAnimatorFactory;
import dev.breezes.settlements.domain.animation.DefaultLocomotionAnimator;
import dev.breezes.settlements.domain.animation.EatingAnimations;
import dev.breezes.settlements.domain.animation.FishingAnimations;
import dev.breezes.settlements.domain.animation.HarvestCropAnimations;
import dev.breezes.settlements.domain.animation.IdleLifeAnimationLibrary;
import dev.breezes.settlements.domain.animation.IdleLifeAnimations;
import dev.breezes.settlements.domain.animation.IdleLifeAnimatorFactory;
import dev.breezes.settlements.domain.animation.InMemoryAnimationLibrary;
import dev.breezes.settlements.domain.animation.InMemoryIdleLifeAnimationLibrary;
import dev.breezes.settlements.domain.animation.InMemoryLocomotionAnimationLibrary;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.animation.LocomotionAnimationLibrary;
import dev.breezes.settlements.domain.animation.LocomotionAnimations;
import dev.breezes.settlements.domain.animation.LocomotionAnimator;
import dev.breezes.settlements.domain.animation.PickUpAnimations;
import dev.breezes.settlements.domain.animation.RepairIronGolemAnimations;
import dev.breezes.settlements.domain.animation.SleepingAnimations;
import dev.breezes.settlements.domain.animation.ThrowEggAnimations;
import dev.breezes.settlements.domain.animation.WaveAnimations;
import dev.breezes.settlements.domain.presentation.ItemCategory;

import java.util.List;
import java.util.Map;

@Module
public abstract class ClientAnimationModule {

    @Binds
    abstract AnimationResolver animationResolver(DefaultAnimationResolver implementation);

    @Binds
    abstract IdleLifeAnimatorFactory idleLifeAnimatorFactory(DefaultIdleLifeAnimatorFactory implementation);

    @Binds
    abstract LocomotionAnimator locomotionAnimator(DefaultLocomotionAnimator implementation);

    @Provides
    @ClientSessionScope
    static AnimationLibrary animationLibrary() {
        return InMemoryAnimationLibrary.builder()
                .animations(Map.ofEntries(
                        Map.entry(AnimationKey.of(AnimationArchetype.SWING_HEAVY, ItemCategory.GENERIC), ChopAnimations.chopCrossedArms()),
                        Map.entry(AnimationKey.of(AnimationArchetype.CAST, ItemCategory.FISHING_ROD), FishingAnimations.cast()),
                        Map.entry(AnimationKey.of(AnimationArchetype.FISHING_WAIT, ItemCategory.FISHING_ROD), FishingAnimations.fishingLoop()),
                        Map.entry(AnimationKey.of(AnimationArchetype.REEL_OUT, ItemCategory.FISHING_ROD), FishingAnimations.fightFish()),
                        Map.entry(AnimationKey.of(AnimationArchetype.REEL_IN, ItemCategory.FISHING_ROD), FishingAnimations.reelIn()),
                        Map.entry(AnimationKey.of(AnimationArchetype.INTERACT, ItemCategory.GENERIC), InteractAnimations.interact()),
                        Map.entry(AnimationKey.of(AnimationArchetype.SURVEY_WITH_SPYGLASS, ItemCategory.SPYGLASS), CartographerAnimations.surveyWithSpyglass()),
                        Map.entry(AnimationKey.of(AnimationArchetype.WRITE_TO_MAP, ItemCategory.MAP), CartographerAnimations.markMap()),
                        Map.entry(AnimationKey.of(AnimationArchetype.EAT, ItemCategory.GENERIC), EatingAnimations.eat()),
                        Map.entry(AnimationKey.of(AnimationArchetype.WAVE, ItemCategory.GENERIC), WaveAnimations.wave()),
                        Map.entry(AnimationKey.of(AnimationArchetype.PICK_UP, ItemCategory.GENERIC), PickUpAnimations.pickUp()),
                        Map.entry(AnimationKey.of(AnimationArchetype.HARVEST, ItemCategory.GENERIC), HarvestCropAnimations.harvestCrop()),
                        Map.entry(AnimationKey.of(AnimationArchetype.THROW, ItemCategory.GENERIC), ThrowEggAnimations.throwEgg()),
                        Map.entry(AnimationKey.of(AnimationArchetype.SLEEP, ItemCategory.GENERIC), SleepingAnimations.sleeping()),
                        Map.entry(AnimationKey.of(AnimationArchetype.REPAIR_IRON_GOLEM, ItemCategory.GENERIC), RepairIronGolemAnimations.repairIronGolem())))
                .build();
    }

    @Provides
    @ClientSessionScope
    static IdleLifeAnimationLibrary idleLifeAnimationLibrary() {
        return InMemoryIdleLifeAnimationLibrary.builder()
                .baseIdle(IdleLifeAnimations.baseIdle())
                .blink(IdleLifeAnimations.blink())
                .fidgets(List.of(
                        IdleLifeAnimations.ponder()))
                .build();
    }

    @Provides
    @ClientSessionScope
    static LocomotionAnimationLibrary locomotionAnimationLibrary() {
        return InMemoryLocomotionAnimationLibrary.builder()
                .animations(Map.of(
                        NavigationType.STROLL, LocomotionAnimations.stroll(),
                        NavigationType.WALK, LocomotionAnimations.walk(),
                        NavigationType.RUN, LocomotionAnimations.jog(),
                        NavigationType.SPRINT, LocomotionAnimations.run(),
                        NavigationType.PANIC, LocomotionAnimations.panicRun()))
                .build();
    }

}
