package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Returns a precondition that passes when the villager holds enough of the matched item.
     * Emits a demand signal when the villager is short so TakeFromChest/Trade can procure it.
     * The gate and signal are suppressed entirely when bypassInventoryRequirements is on.
     */
    public ICondition<BaseVillager> requireItem(@Nonnull ItemMatch match,
                                                int quantity,
                                                int priorityBoost,
                                                @Nonnull String source) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("requireItem quantity must be positive");
        }
        String name = "RequireItem:" + match.asDebugString();
        return ICondition.named(name, villager -> {
            if (villager == null) {
                return false;
            }
            if (GeneralConfig.bypassInventoryRequirements) {
                return true;
            }
            if (villager.getSettlementsInventory().countMatching(match) >= quantity) {
                // Clear any signal previously emitted for this match so chest/trade stop procuring
                this.remove(villager, match);
                return true;
            }
            this.emit(villager, match, quantity, priorityBoost, null, source, villager.level().getGameTime());
            return false;
        });
    }

    /**
     * Like requireItem, but accepts any one of the provided matches.
     * Emits one signal per unsatisfied match so the procurement network can fulfil whichever is easiest.
     */
    public ICondition<BaseVillager> requireAny(@Nonnull List<ItemMatch> matches,
                                               int quantity,
                                               int priorityBoost,
                                               @Nonnull String source) {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("requireAny matches must not be empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("requireAny quantity must be positive");
        }
        String name = "RequireAny:" + matches.stream()
                .map(ItemMatch::asDebugString)
                .collect(Collectors.joining("|"));
        return ICondition.named(name, villager -> {
            if (villager == null) {
                return false;
            }
            if (GeneralConfig.bypassInventoryRequirements) {
                return true;
            }
            for (ItemMatch match : matches) {
                if (villager.getSettlementsInventory().countMatching(match) >= quantity) {
                    // Any-of satisfied — clear signals for all alternatives, not just the satisfied one
                    for (ItemMatch m : matches) {
                        this.remove(villager, m);
                    }
                    return true;
                }
            }
            // Nothing satisfies — emit one signal per match so chest/trade can pick whichever the world makes easiest
            long now = villager.level().getGameTime();
            for (ItemMatch match : matches) {
                this.emit(villager, match, quantity, priorityBoost, null, source, now);
            }
            return false;
        });
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
