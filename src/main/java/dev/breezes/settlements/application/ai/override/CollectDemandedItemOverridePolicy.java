package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Override policy that fires an opportunistic item pickup when
 * {@code DemandedGroundItemSensor} has flagged a demanded item nearby and the villager is
 * genuinely idle — never a work interruption.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CollectDemandedItemOverridePolicy implements OverridePolicy {

    public static final int PRIORITY = 50;

    private static final Set<Activity> IDLE_WINDOW_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        boolean itemNearby = villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEM_NEARBY)
                .orElse(false);
        if (!itemNearby) {
            return Optional.empty();
        }

        boolean planBehaviorActive = villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE)
                .orElse(false);
        if (planBehaviorActive) {
            return Optional.empty();
        }

        Optional<Activity> activeActivity = villager.getBrain().getActiveNonCoreActivity();
        if (activeActivity.isEmpty() || !IDLE_WINDOW_ACTIVITIES.contains(activeActivity.get())) {
            return Optional.empty();
        }

        return Optional.of(OverrideRequest.builder().behaviorKey(BehaviorKey.COLLECT_DEMANDED_ITEM).build());
    }

}
