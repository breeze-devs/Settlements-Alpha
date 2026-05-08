package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

@ServerScope
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DemandSignalService {

    private static final ClockTicks STALE_SIGNAL_CUTOFF = ClockTicks.hours(72);

    public DemandSignalSet getSignals(@Nonnull BaseVillager villager, long currentGameTime) {
        DemandSignalState currentState = villager.getData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS);
        DemandSignalSet current = currentState.demandSignalSet();
        DemandSignalSet pruned = current.removeStale(currentGameTime, STALE_SIGNAL_CUTOFF.getTicks());
        if (!pruned.equals(current)) {
            villager.setData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS, currentState.withDemandSignalSet(pruned));
        }
        return pruned;
    }

    public void emit(@Nonnull BaseVillager villager,
                     @Nonnull ItemMatch key,
                     int desiredCount,
                     int priorityBoost,
                     @Nullable Integer pricePerUnitOverride,
                     @Nonnull String source,
                     long currentGameTime) {
        DemandSignalState currentState = villager.getData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS);
        DemandSignalSet current = currentState.demandSignalSet();
        long createdGameTime = current.find(key)
                .map(DemandSignal::createdGameTime)
                .orElse(currentGameTime);

        DemandSignal entry = DemandSignal.builder()
                .match(key)
                .desiredCount(desiredCount)
                .priorityBoost(priorityBoost)
                .pricePerUnitOverride(pricePerUnitOverride)
                .source(source)
                .createdGameTime(createdGameTime)
                .lastTouchedGameTime(currentGameTime)
                .build();

        DemandSignalSet updated = current.upsert(entry);
        if (!updated.equals(current)) {
            villager.setData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS, currentState.withDemandSignalSet(updated));
        }
    }

    public void remove(@Nonnull BaseVillager villager, @Nonnull ItemMatch key) {
        DemandSignalState currentState = villager.getData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS);
        DemandSignalSet current = currentState.demandSignalSet();
        DemandSignalSet updated = current.remove(key);
        if (!updated.equals(current)) {
            villager.setData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS, currentState.withDemandSignalSet(updated));
        }
    }

    public int getVersion(@Nonnull BaseVillager villager) {
        return this.pruneAndGetState(villager, villager.level().getGameTime()).version();
    }

    private DemandSignalState pruneAndGetState(@Nonnull BaseVillager villager, long currentGameTime) {
        DemandSignalState currentState = villager.getData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS);
        DemandSignalSet current = currentState.demandSignalSet();
        DemandSignalSet pruned = current.removeStale(currentGameTime, STALE_SIGNAL_CUTOFF.getTicks());
        if (pruned.equals(current)) {
            return currentState;
        }

        DemandSignalState updatedState = currentState.withDemandSignalSet(pruned);
        villager.setData(AttachmentRegistry.VILLAGER_DEMAND_SIGNALS, updatedState);
        return updatedState;
    }

}
