package dev.breezes.settlements.infrastructure.minecraft.data.trading;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.economy.catalog.DemandEntry;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class TradeCatalogDataManager extends SimpleJsonResourceReloadListener implements TradeCatalogRegistry {

    private static final String DIRECTORY_PATH = "settlements/trade_catalog";
    private static final Gson GSON = new GsonBuilder().create();

    private Map<VillagerProfessionKey, List<OfferEntry>> offersByProfession = Map.of();
    private Map<VillagerProfessionKey, List<DemandEntry>> demandsByProfession = Map.of();
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
        this.offersByProfession = snapshot.offersByProfession();
        this.demandsByProfession = snapshot.demandsByProfession();
        this.catalogVersion++;

        log.info("Loaded trade catalog for {} professions ({} errors)",
                snapshot.professionCount(), errorCount);
    }

    @VisibleForTesting
    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
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
    public int catalogVersion() {
        return this.catalogVersion;
    }

    private static TradeCatalogSnapshot buildSnapshot(@Nonnull List<TradeCatalogDefinition> definitions) {
        Map<VillagerProfessionKey, LinkedHashMap<String, OfferEntry>> offers = new LinkedHashMap<>();
        Map<VillagerProfessionKey, LinkedHashMap<String, DemandEntry>> demands = new LinkedHashMap<>();

        for (TradeCatalogDefinition definition : definitions) {
            // Last-writer-wins per entry id within a profession
            LinkedHashMap<String, OfferEntry> professionOffers = offers.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            for (OfferEntry offer : definition.offers()) {
                professionOffers.put(offer.id(), offer);
            }

            LinkedHashMap<String, DemandEntry> professionDemands = demands.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            for (DemandEntry demand : definition.demands()) {
                professionDemands.put(demand.id(), demand);
            }
        }

        Map<VillagerProfessionKey, List<OfferEntry>> immutableOffers = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, OfferEntry>> entry : offers.entrySet()) {
            immutableOffers.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        Map<VillagerProfessionKey, List<DemandEntry>> immutableDemands = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, DemandEntry>> entry : demands.entrySet()) {
            immutableDemands.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        return new TradeCatalogSnapshot(immutableOffers, immutableDemands);
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
        List<OfferEntry> offers = file.offers == null ? List.of() : file.offers.stream().map(TradeCatalogDataManager::parseOffer).toList();
        List<DemandEntry> demands = file.demands == null ? List.of() : file.demands.stream().map(TradeCatalogDataManager::parseDemand).toList();
        return new TradeCatalogDefinition(profession, offers, demands);
    }

    private static OfferEntry parseOffer(@Nonnull OffersFile file) {
        return OfferEntry.builder()
                .id(requireNonBlank(file.id, "offer.id"))
                .match(parseMatch(file.match))
                .bundleSize(requirePositive(file.bundleSize, "offer.bundleSize"))
                .basePrice(requirePositive(file.basePrice, "offer.basePrice"))
                .priceJitter(requireNonNegative(file.priceJitter, "offer.priceJitter"))
                .surplusThreshold(requireNonNegative(file.surplusThreshold, "offer.surplusThreshold"))
                .build();
    }

    private static DemandEntry parseDemand(@Nonnull DemandsFile file) {
        return DemandEntry.builder()
                .id(requireNonBlank(file.id, "demand.id"))
                .match(parseMatch(file.match))
                .desiredMinCount(requirePositive(file.desiredMinCount, "demand.desiredMinCount"))
                .basePricePerUnit(requirePositive(file.basePricePerUnit, "demand.basePricePerUnit"))
                .basePriority(requireNonNegative(file.basePriority, "demand.basePriority"))
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

    private static int requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static int requireNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }

    private record TradeCatalogSnapshot(
            Map<VillagerProfessionKey, List<OfferEntry>> offersByProfession,
            Map<VillagerProfessionKey, List<DemandEntry>> demandsByProfession
    ) {
        // Union of both key sets to avoid double-counting professions that have both offers and demands.
        int professionCount() {
            HashSet<VillagerProfessionKey> all = new HashSet<>(offersByProfession.keySet());
            all.addAll(demandsByProfession.keySet());
            return all.size();
        }
    }

    private static final class TradeCatalogFile {
        private String profession;
        private List<OffersFile> offers;
        private List<DemandsFile> demands;
    }

    private static final class OffersFile {
        private String id;
        private MatchFile match;
        private Integer bundleSize;
        private Integer basePrice;
        private Integer priceJitter;
        private Integer surplusThreshold;
    }

    private static final class DemandsFile {
        private String id;
        private MatchFile match;
        private Integer desiredMinCount;
        private Integer basePricePerUnit;
        private Integer basePriority;
    }

    private static final class MatchFile {
        private String item;
        private String tag;
    }

}
