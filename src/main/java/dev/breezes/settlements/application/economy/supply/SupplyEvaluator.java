package dev.breezes.settlements.application.economy.supply;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.inventory.BackpackEntry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SupplyEvaluator {

    private final TradeCatalogRegistry tradeCatalogRegistry;

    public List<ActiveSupply> resolve(@Nonnull BaseVillager villager) {
        VillagerProfessionKey profession = villager.getProfession();
        List<SupplyEntry> staticSupply = this.tradeCatalogRegistry.supplyFor(profession);
        if (staticSupply.isEmpty()) {
            return List.of();
        }

        List<BackpackEntry> inventoryEntries = villager.getSettlementsInventory().entries();
        List<ActiveSupply> activeSupply = new ArrayList<>();

        for (SupplyEntry supply : staticSupply) {
            int held = villager.getSettlementsInventory().countMatching(supply.match());
            int remainingDumpable = Math.max(held - supply.dumpAbove(), 0);
            if (remainingDumpable <= 0) {
                continue;
            }

            for (BackpackEntry inventoryEntry : inventoryEntries) {
                if (remainingDumpable <= 0) {
                    break;
                }
                if (!ItemMatches.test(supply.match(), inventoryEntry.representative())) {
                    continue;
                }

                int dumpableFromEntry = Math.min(inventoryEntry.count(), remainingDumpable);
                if (dumpableFromEntry <= 0) {
                    continue;
                }

                activeSupply.add(ActiveSupply.builder()
                        .match(supply.match())
                        .representative(inventoryEntry.representative().copyWithCount(1))
                        .dumpableCount(dumpableFromEntry)
                        .build());
                remainingDumpable -= dumpableFromEntry;
            }
        }

        return List.copyOf(activeSupply);
    }

}
