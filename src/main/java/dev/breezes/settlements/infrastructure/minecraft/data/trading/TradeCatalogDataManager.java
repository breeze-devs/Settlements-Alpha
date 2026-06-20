package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.DumpFacet;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferFacet;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.RestockFacet;
import dev.breezes.settlements.domain.economy.catalog.StockPolicy;
import dev.breezes.settlements.domain.economy.catalog.SupplyEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.CustomLog;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class TradeCatalogDataManager extends SimpleJsonResourceReloadListener implements TradeCatalogRegistry {

    private static final String DIRECTORY_PATH = "settlements/trade_catalog";
    private static final Gson GSON = new GsonBuilder().create();

    private Map<VillagerProfessionKey, List<StockPolicy>> stockPoliciesByProfession = Map.of();
    private Map<VillagerProfessionKey, List<OfferEntry>> offersByProfession = Map.of();
    private Map<VillagerProfessionKey, List<DemandEntry>> demandsByProfession = Map.of();
    private Map<VillagerProfessionKey, List<SupplyEntry>> supplyByProfession = Map.of();
    private int catalogVersion = 0;

    @Inject
    public TradeCatalogDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        List<TradeCatalogDefinition> definitions = new ArrayList<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                TradeCatalogFile file = GSON.fromJson(entry.getValue(), TradeCatalogFile.class);
                if (file == null) {
                    throw new IllegalArgumentException("parsed entry was null");
                }
                definitions.add(parseFile(file));
            } catch (Exception exception) {
                log.warn("Failed to parse trade catalog from file '{}': {}", entry.getKey(), exception.getMessage());
                errorCount++;
            }
        }

        TradeCatalogSnapshot snapshot = buildSnapshot(definitions);
        this.stockPoliciesByProfession = snapshot.stockPoliciesByProfession();
        this.offersByProfession = snapshot.offersByProfession();
        this.demandsByProfession = snapshot.demandsByProfession();
        this.supplyByProfession = snapshot.supplyByProfession();
        this.catalogVersion++;

        log.info("Loaded trade catalog for {} professions ({} errors)",
                snapshot.professionCount(), errorCount);
    }

    @VisibleForTesting
    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Override
    public List<StockPolicy> stockPoliciesFor(@Nonnull VillagerProfessionKey profession) {
        return this.stockPoliciesByProfession.getOrDefault(profession, List.of());
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

    @Override
    public int catalogVersion() {
        return this.catalogVersion;
    }

    private static TradeCatalogSnapshot buildSnapshot(@Nonnull List<TradeCatalogDefinition> definitions) {
        Map<VillagerProfessionKey, LinkedHashMap<String, StockPolicy>> stockPolicies = new LinkedHashMap<>();
        Map<VillagerProfessionKey, LinkedHashMap<String, OfferEntry>> offers = new LinkedHashMap<>();
        Map<VillagerProfessionKey, LinkedHashMap<String, DemandEntry>> demands = new LinkedHashMap<>();
        Map<VillagerProfessionKey, LinkedHashMap<String, SupplyEntry>> supply = new LinkedHashMap<>();

        for (TradeCatalogDefinition definition : definitions) {
            // Data packs intentionally allow later files to replace stock policies by id.
            LinkedHashMap<String, StockPolicy> professionStockPolicies = stockPolicies.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            LinkedHashMap<String, OfferEntry> professionOffers = offers.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            LinkedHashMap<String, DemandEntry> professionDemands = demands.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            LinkedHashMap<String, SupplyEntry> professionSupply = supply.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());

            for (StockPolicy policy : definition.stockPolicies()) {
                professionStockPolicies.put(policy.id(), policy);
                if (policy.offer() != null) {
                    professionOffers.put(policy.id(), policy.toOfferEntry());
                }
                if (policy.restock() != null) {
                    professionDemands.put(policy.id(), policy.toDemandEntry());
                }
                if (policy.dump() != null) {
                    professionSupply.put(policy.id(), policy.toSupplyEntry());
                }
            }
        }

        Map<VillagerProfessionKey, List<StockPolicy>> immutableStockPolicies = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, StockPolicy>> entry : stockPolicies.entrySet()) {
            immutableStockPolicies.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        Map<VillagerProfessionKey, List<OfferEntry>> immutableOffers = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, OfferEntry>> entry : offers.entrySet()) {
            immutableOffers.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        Map<VillagerProfessionKey, List<DemandEntry>> immutableDemands = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, DemandEntry>> entry : demands.entrySet()) {
            immutableDemands.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        Map<VillagerProfessionKey, List<SupplyEntry>> immutableSupply = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, SupplyEntry>> entry : supply.entrySet()) {
            immutableSupply.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        return new TradeCatalogSnapshot(immutableStockPolicies, immutableOffers, immutableDemands, immutableSupply);
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

    private static TradeCatalogDefinition parseFile(@Nonnull TradeCatalogFile file) {
        if (file.profession == null || file.profession.isBlank()) {
            throw new IllegalArgumentException("missing profession");
        }

        VillagerProfessionKey profession = VillagerProfessionKey.fromResourceLocation(ResourceLocation.parse(file.profession));
        if (file.stock == null) {
            throw new IllegalArgumentException("missing stock");
        }

        List<StockPolicy> stockPolicies = file.stock.stream().map(TradeCatalogDataManager::parseStockPolicy).toList();
        return new TradeCatalogDefinition(profession, stockPolicies);
    }

    private static StockPolicy parseStockPolicy(@Nonnull StockFile file) {
        return StockPolicy.builder()
                .id(requireNonBlank(file.id, "stock.id"))
                .match(parseMatch(file.match))
                .restock(file.restock == null ? null : parseRestock(file.restock))
                .offer(file.offer == null ? null : parseOffer(file.offer))
                .dump(file.dump == null ? null : parseDump(file.dump))
                .build();
    }

    private static RestockFacet parseRestock(@Nonnull RestockFile file) {
        return RestockFacet.builder()
                .below(requirePresent(file.below, "stock.restock.below"))
                .buyPricePerUnit(requirePresent(file.buyPricePerUnit, "stock.restock.buyPricePerUnit"))
                .priority(requirePresent(file.priority, "stock.restock.priority"))
                .build();
    }

    private static OfferFacet parseOffer(@Nonnull OfferFile file) {
        return OfferFacet.builder()
                .above(requirePresent(file.above, "stock.offer.above"))
                .basePrice(requirePresent(file.basePrice, "stock.offer.basePrice"))
                .priceJitter(requirePresent(file.priceJitter, "stock.offer.priceJitter"))
                .bundleSize(requirePresent(file.bundleSize, "stock.offer.bundleSize"))
                .build();
    }

    private static DumpFacet parseDump(@Nonnull DumpFile file) {
        return DumpFacet.builder()
                .above(requirePresent(file.above, "stock.dump.above"))
                .build();
    }

    private static ItemMatch parseMatch(MatchFile file) {
        if (file == null) {
            throw new IllegalArgumentException("missing match");
        }

        boolean hasItem = file.item != null && !file.item.isBlank();
        boolean hasTag = file.tag != null && !file.tag.isBlank();
        if (hasItem == hasTag) {
            throw new IllegalArgumentException("match must define exactly one of item or tag");
        }

        if (hasItem) {
            return new ItemMatch.ItemRef(ResourceLocation.parse(file.item));
        }

        return new ItemMatch.TagRef(TagKey.create(Registries.ITEM, ResourceLocation.parse(file.tag)));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + fieldName);
        }
        return value;
    }

    // Boundary mapping only checks that the field is present; the facet records own the value-range
    // invariants, so we don't duplicate the >0 / >=0 rules here.
    private static int requirePresent(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("missing " + fieldName);
        }
        return value;
    }

    private record TradeCatalogSnapshot(
            Map<VillagerProfessionKey, List<StockPolicy>> stockPoliciesByProfession,
            Map<VillagerProfessionKey, List<OfferEntry>> offersByProfession,
            Map<VillagerProfessionKey, List<DemandEntry>> demandsByProfession,
            Map<VillagerProfessionKey, List<SupplyEntry>> supplyByProfession
    ) {
        int professionCount() {
            return stockPoliciesByProfession.size();
        }
    }

    private static final class TradeCatalogFile {
        private String profession;
        private List<StockFile> stock;
    }

    private static final class StockFile {
        private String id;
        private MatchFile match;
        private RestockFile restock;
        private OfferFile offer;
        private DumpFile dump;
    }

    private static final class RestockFile {
        private Integer below;
        private Integer buyPricePerUnit;
        private Integer priority;
    }

    private static final class OfferFile {
        private Integer above;
        private Integer basePrice;
        private Integer priceJitter;
        private Integer bundleSize;
    }

    private static final class DumpFile {
        private Integer above;
    }

    private static final class MatchFile {
        private String item;
        private String tag;
    }

}
