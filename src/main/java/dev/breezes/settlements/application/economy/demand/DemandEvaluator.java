package dev.breezes.settlements.application.economy.demand;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DemandEvaluator {

    public static final int DEFAULT_BASE_PRICE_PER_UNIT = 4;

    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DemandSignalService demandSignalService;

    // TODO: efficiency optimization. This can likely be optimized
    public List<ActiveDemand> resolve(@Nonnull BaseVillager villager) {
        long currentGameTime = villager.level().getGameTime();
        VillagerProfessionKey profession = VillagerProfessionKey.fromResourceLocation(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())
        );

        List<DemandEntry> staticDemands = this.tradeCatalogRegistry.demandsFor(profession);
        Map<ItemMatch, ActiveDemand> activeByMatch = new LinkedHashMap<>();
        Map<ItemMatch, DemandEntry> staticDemandByMatch = new LinkedHashMap<>();

        for (DemandEntry demand : staticDemands) {
            staticDemandByMatch.put(demand.match(), demand);
            int currentCount = countMatchingInventory(villager, demand.match());
            int shortfall = Math.max(demand.desiredMinCount() - currentCount, 0);
            if (shortfall <= 0) {
                continue;
            }

            activeByMatch.put(demand.match(), ActiveDemand.builder()
                    .match(demand.match())
                    .desiredCount(shortfall)
                    .priority(demand.basePriority())
                    .basePricePerUnit(demand.basePricePerUnit())
                    .origin(ActiveDemand.Origin.STATIC_SHORTFALL)
                    .build());
        }

        for (DemandSignal signal : this.demandSignalService.getSignals(villager, currentGameTime).entries()) {
            DemandEntry staticDemand = staticDemandByMatch.get(signal.match());
            if (staticDemand != null) {
                int currentCount = countMatchingInventory(villager, staticDemand.match());
                int shortfall = Math.max(staticDemand.desiredMinCount() - currentCount, 0);
                int desiredCount = Math.max(shortfall, signal.desiredCount());
                activeByMatch.put(signal.match(), ActiveDemand.builder()
                        .match(signal.match())
                        .desiredCount(desiredCount)
                        .priority(staticDemand.basePriority() + signal.priorityBoost())
                        .basePricePerUnit(signal.pricePerUnitOverride() != null ? signal.pricePerUnitOverride() : staticDemand.basePricePerUnit())
                        .origin(ActiveDemand.Origin.STATIC_BOOSTED_BY_SIGNAL)
                        .build());
                continue;
            }

            activeByMatch.put(signal.match(), ActiveDemand.builder()
                    .match(signal.match())
                    .desiredCount(signal.desiredCount())
                    .priority(signal.priorityBoost())
                    .basePricePerUnit(signal.pricePerUnitOverride() != null ? signal.pricePerUnitOverride() : DEFAULT_BASE_PRICE_PER_UNIT)
                    .origin(ActiveDemand.Origin.SIGNAL)
                    .build());
        }

        return activeByMatch.values().stream()
                .sorted(Comparator.comparingInt(ActiveDemand::priority).reversed())
                .toList();
    }

    private static int countMatchingInventory(@Nonnull BaseVillager villager, @Nonnull ItemMatch match) {
        int count = 0;
        for (ItemStack stack : villager.getSettlementsInventory().getBackpack().getItems()) {
            if (ItemMatches.test(match, stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

}
