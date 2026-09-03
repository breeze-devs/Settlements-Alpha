package dev.breezes.settlements.infrastructure.rendering.animation;

import dev.breezes.settlements.di.ClientSessionScope;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.IdleLifeAnimatorFactory;
import dev.breezes.settlements.domain.animation.LayerStackAnimators;
import dev.breezes.settlements.domain.animation.LocomotionAnimator;
import dev.breezes.settlements.domain.animation.UmbrellaAnimatorFactory;
import dev.breezes.settlements.domain.animation.VillagerAnimator;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

@CustomLog
@ClientSessionScope
public final class ClientAnimatorRegistry {

    private static final int PRUNE_INTERVAL_LOOKUPS = 600;

    private final AnimationResolver animationResolver;
    private final IdleLifeAnimatorFactory idleLifeAnimatorFactory;
    private final LocomotionAnimator locomotionAnimator;
    private final UmbrellaAnimatorFactory umbrellaAnimatorFactory;
    private final Map<Integer, VillagerAnimator> animatorsByEntityId = new ConcurrentHashMap<>();
    private int lookupCount;

    @Inject
    ClientAnimatorRegistry(@Nonnull AnimationResolver animationResolver,
                           @Nonnull IdleLifeAnimatorFactory idleLifeAnimatorFactory,
                           @Nonnull LocomotionAnimator locomotionAnimator,
                           @Nonnull UmbrellaAnimatorFactory umbrellaAnimatorFactory) {
        this.animationResolver = animationResolver;
        this.idleLifeAnimatorFactory = idleLifeAnimatorFactory;
        this.locomotionAnimator = locomotionAnimator;
        this.umbrellaAnimatorFactory = umbrellaAnimatorFactory;
    }

    public VillagerAnimator getOrCreate(@Nonnull BaseVillager villager) {
        return this.getOrCreate(villager.getId(), entityId -> villager.level().getEntity(entityId) != null);
    }

    VillagerAnimator getOrCreate(int entityId, @Nonnull IntPredicate entityExists) {
        this.prunePeriodically(entityExists);
        return this.animatorsByEntityId.computeIfAbsent(entityId, ignored ->
                new VillagerAnimator(this.animationResolver, LayerStackAnimators.builder()
                        .idleLife(this.idleLifeAnimatorFactory.create(entityId))
                        .locomotion(this.locomotionAnimator)
                        .umbrella(this.umbrellaAnimatorFactory.create())
                        .build(), entityId));
    }

    public int size() {
        return this.animatorsByEntityId.size();
    }

    private void prunePeriodically(@Nonnull IntPredicate entityExists) {
        this.lookupCount++;
        if (this.lookupCount < PRUNE_INTERVAL_LOOKUPS) {
            return;
        }

        this.lookupCount = 0;
        // Rendering lifetime follows client-level entity presence; this single check covers despawn,
        // death, chunk unload, and dimension changes without coupling entities to renderer state.
        int sizeBefore = this.animatorsByEntityId.size();
        this.animatorsByEntityId.keySet().removeIf(entityId -> !entityExists.test(entityId));
        int pruned = sizeBefore - this.animatorsByEntityId.size();
        if (pruned > 0) {
            log.debug("ClientAnimatorRegistry: pruned {} stale animator(s), {} remaining", pruned, this.animatorsByEntityId.size());
        }
    }

}
