package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.StockPolicy;
import dev.breezes.settlements.domain.economy.catalog.StockPolicyCodec;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.ProfessionCatalogDataManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TradeCatalogDataManager extends ProfessionCatalogDataManager<StockPolicy> implements TradeCatalogRegistry {

    private static final String DIRECTORY_PATH = "settlements/trade_catalog";

    private Map<VillagerProfessionKey, List<OfferEntry>> offersByProfession = Map.of();
    private Map<VillagerProfessionKey, List<DemandEntry>> demandsByProfession = Map.of();
    private Map<VillagerProfessionKey, List<SupplyEntry>> supplyByProfession = Map.of();

    @Inject
    public TradeCatalogDataManager() {
        super(DIRECTORY_PATH, StockPolicyCodec.CODEC, "stock");
    }

    @Override
    protected String label() {
        return "trade catalog";
    }

    @Override
    protected String idOf(StockPolicy value) {
        return value.id();
    }

    @Override
    protected void onCatalogReloaded(@Nonnull Map<VillagerProfessionKey, List<StockPolicy>> byProfession) {
        // Stock policies are already id-deduped per profession by the base class, so each of these
        // projections can be derived straight from the list without re-merging by id.
        Map<VillagerProfessionKey, List<OfferEntry>> offers = new LinkedHashMap<>();
        Map<VillagerProfessionKey, List<DemandEntry>> demands = new LinkedHashMap<>();
        Map<VillagerProfessionKey, List<SupplyEntry>> supply = new LinkedHashMap<>();

        for (Map.Entry<VillagerProfessionKey, List<StockPolicy>> entry : byProfession.entrySet()) {
            List<OfferEntry> professionOffers = new ArrayList<>();
            List<DemandEntry> professionDemands = new ArrayList<>();
            List<SupplyEntry> professionSupply = new ArrayList<>();

            for (StockPolicy policy : entry.getValue()) {
                if (policy.offer() != null) {
                    professionOffers.add(policy.toOfferEntry());
                }
                if (policy.restock() != null) {
                    professionDemands.add(policy.toDemandEntry());
                }
                if (policy.dump() != null) {
                    professionSupply.add(policy.toSupplyEntry());
                }
            }

            offers.put(entry.getKey(), List.copyOf(professionOffers));
            demands.put(entry.getKey(), List.copyOf(professionDemands));
            supply.put(entry.getKey(), List.copyOf(professionSupply));
        }

        this.offersByProfession = Map.copyOf(offers);
        this.demandsByProfession = Map.copyOf(demands);
        this.supplyByProfession = Map.copyOf(supply);
    }

    @Override
    public List<StockPolicy> stockPoliciesFor(@Nonnull VillagerProfessionKey profession) {
        return this.valuesFor(profession);
    }

    @Nonnull
    @Override
    public List<OfferEntry> findOffers(@Nonnull VillagerProfessionKey sellerProfession, @Nonnull ItemMatch want) {
        return this.offersFor(sellerProfession).stream()
                .filter(entry -> matchPriority(entry.match(), want) >= 0)
                .sorted((left, right) -> Integer.compare(matchPriority(right.match(), want), matchPriority(left.match(), want)))
                .toList();
    }

    @Nonnull
    @Override
    public List<DemandEntry> findDemands(@Nonnull VillagerProfessionKey buyerProfession, @Nonnull ItemMatch want) {
        return this.demandsByProfession.getOrDefault(buyerProfession, List.of()).stream()
                .filter(entry -> matchPriority(entry.match(), want) >= 0)
                .sorted((left, right) -> Integer.compare(matchPriority(right.match(), want), matchPriority(left.match(), want)))
                .toList();
    }

    @Override
    public List<OfferEntry> offersFor(@Nonnull VillagerProfessionKey profession) {
        return this.offersByProfession.getOrDefault(profession, List.of());
    }

    @Nonnull
    @Override
    public List<DemandEntry> demandsFor(@Nonnull VillagerProfessionKey profession) {
        return this.demandsByProfession.getOrDefault(profession, List.of());
    }

    @Override
    public List<SupplyEntry> supplyFor(@Nonnull VillagerProfessionKey profession) {
        return this.supplyByProfession.getOrDefault(profession, List.of());
    }

    private static int matchPriority(@Nonnull ItemMatch catalogMatch, @Nonnull ItemMatch want) {
        return switch (catalogMatch) {
            case ItemMatch.ItemRef itemRef -> switch (want) {
                case ItemMatch.ItemRef wantRef -> itemRef.id().equals(wantRef.id()) ? 3 : -1;
                case ItemMatch.TagRef ignored -> 0; // cross-kind: include as candidate, scanner resolves
            };
            case ItemMatch.TagRef tagRef -> switch (want) {
                case ItemMatch.ItemRef ignored -> 0; // cross-kind: include as candidate, scanner resolves
                case ItemMatch.TagRef wantTagRef -> tagRef.tag().equals(wantTagRef.tag()) ? 3 : -1;
            };
        };
    }

}
