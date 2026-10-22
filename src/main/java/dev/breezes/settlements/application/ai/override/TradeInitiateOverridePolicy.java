package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
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
 * Override policy that opens a trade when the villager perceived a seller of a wanted item and is free to trade.
 */
@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TradeInitiateOverridePolicy implements OverridePolicy {

    private static final Set<Activity> IDLE_WINDOW_ACTIVITIES = Set.of(Activity.WORK, Activity.MEET, Activity.IDLE);

    @Override
    public OverrideTier tier() {
        return OverrideTier.OPPORTUNISTIC;
    }

    @Override
    public boolean isAdmissibleDuring(@Nullable Activity activity) {
        return activity != null && IDLE_WINDOW_ACTIVITIES.contains(activity);
    }

    @Override
    public Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        IBrain brain = villager.getSettlementsBrain();
        if (brain.getMemory(MemoryTypeRegistry.DEMANDED_ITEM_SELLER).isEmpty()) {
            return Optional.empty();
        }

        // A trade that fell through leaves the seller and the want in place, so the seller alone would re-invite at once
        if (brain.getMemory(MemoryTypeRegistry.TRADE_INITIATE_COOLDOWN).isPresent()) {
            return Optional.empty();
        }

        // Trading is idle-time only, so it yields to any running day-plan behavior
        if (villager.getPlanRuntimeState().isBehaviorActive()) {
            return Optional.empty();
        }

        return Optional.of(new OverrideRequest.CatalogBehavior(BehaviorKey.TRADE_INITIATE));
    }

}
