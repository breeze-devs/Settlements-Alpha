package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Override policy that fires an opportunistic item pickup when
 * {@link dev.breezes.settlements.application.ai.sensors.DemandedGroundItemSensor} has flagged a
 * demanded item nearby and the villager is free.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CollectDemandedItemOverridePolicy implements OverridePolicy {

    private static final OverridePrecedence PRECEDENCE = OverridePrecedence.builder()
            .tier(OverrideTier.OPPORTUNISTIC)
            .order(10)
            .build();

    private static final Set<Activity> IDLE_WINDOW_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    @Override
    public OverridePrecedence precedence() {
        return PRECEDENCE;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        return activity != null && IDLE_WINDOW_ACTIVITIES.contains(activity);
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        boolean itemNearby = villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.DEMANDED_GROUND_ITEM_NEARBY)
                .orElse(false);
        if (!itemNearby) {
            return Optional.empty();
        }

        // Pickup is idle-time only, so it yields to any running day-plan behavior
        if (villager.getPlanRuntimeState().isBehaviorActive()) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.COLLECT_DEMANDED_ITEM));
    }

}
