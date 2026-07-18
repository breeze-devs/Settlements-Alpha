package dev.breezes.settlements.infrastructure.minecraft.data.fishing;

import dev.breezes.settlements.domain.fishing.FishCatchEntry;
import dev.breezes.settlements.domain.fishing.FishCatchEntryCodec;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import dev.breezes.settlements.shared.util.RandomUtil;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class FishCatchDataManager extends CodecJsonDataManager<FishCatchEntry> {

    private static final String DIRECTORY_PATH = "settlements/fishing/catches";

    private List<FishCatchEntry> catches = List.of();
    private double[] cumulativeWeights = new double[0];
    private double totalWeight = 0.0;

    @Inject
    public FishCatchDataManager() {
        super(DIRECTORY_PATH, FishCatchEntryCodec.CODEC);
    }

    @Override
    protected String label() {
        return "fish catch entry";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, FishCatchEntry> values) {
        // A later file overrides an earlier one that declares the same catch entity
        Map<ResourceLocation, FishCatchEntry> deduplicatedByEntity = new LinkedHashMap<>();
        for (FishCatchEntry entry : values.values()) {
            deduplicatedByEntity.put(entry.getEntityId(), entry);
        }

        List<FishCatchEntry> loadedCatches = List.copyOf(deduplicatedByEntity.values());
        double[] loadedCumulativeWeights = new double[loadedCatches.size()];
        double runningWeight = 0.0;

        for (int i = 0; i < loadedCatches.size(); i++) {
            runningWeight += loadedCatches.get(i).getWeight();
            loadedCumulativeWeights[i] = runningWeight;
        }

        this.catches = loadedCatches;
        this.cumulativeWeights = loadedCumulativeWeights;
        this.totalWeight = runningWeight;
    }

    public List<FishCatchEntry> getAllEntries() {
        return this.catches;
    }

    public Optional<FishCatchEntry> rollRandomCatch() {
        List<FishCatchEntry> entries = this.catches;
        double[] weights = this.cumulativeWeights;
        double maxWeight = this.totalWeight;

        if (entries.isEmpty() || weights.length == 0 || maxWeight <= 0.0) {
            log.error("Unable to determine random fish catch because all entries are empty");
            return Optional.empty();
        }

        double roll = RandomUtil.RANDOM.nextDouble() * maxWeight;
        int index = Arrays.binarySearch(weights, roll);
        if (index < 0) {
            index = -index - 1;
        }

        if (index >= entries.size()) {
            index = entries.size() - 1;
        }

        return Optional.of(entries.get(index));
    }

}
