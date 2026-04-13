package dev.breezes.settlements.infrastructure.minecraft.data.fishing;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.fishing.FishCatchEntry;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class FishCatchDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/fishing/catches";

    private static final Gson GSON = new GsonBuilder().create();

    private List<FishCatchEntry> catches = List.of();
    private double[] cumulativeWeights = new double[0];
    private double totalWeight = 0.0;

    @Inject
    public FishCatchDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, FishCatchEntry> parsed = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                FishCatchEntry data = GSON.fromJson(json, FishCatchEntry.class);
                if (data == null || data.getEntityId() == null || data.getItemId() == null) {
                    log.warn("Invalid fish catch entry in '{}': missing required fields", fileId);
                    errorCount++;
                    continue;
                }
                if (data.getWeight() <= 0) {
                    log.warn("Invalid fish catch entry in '{}': weight must be > 0", fileId);
                    errorCount++;
                    continue;
                }

                if (parsed.containsKey(data.getEntityId())) {
                    log.warn("Duplicate fish catch entity '{}' from file '{}', overwriting",
                            data.getEntityId(), fileId);
                }
                parsed.put(data.getEntityId(), data);
            } catch (Exception e) {
                log.warn("Failed to parse fish catch from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        List<FishCatchEntry> loadedCatches = List.copyOf(parsed.values());
        double[] loadedCumulativeWeights = new double[loadedCatches.size()];
        double runningWeight = 0.0;

        for (int i = 0; i < loadedCatches.size(); i++) {
            runningWeight += loadedCatches.get(i).getWeight();
            loadedCumulativeWeights[i] = runningWeight;
        }

        this.catches = loadedCatches;
        this.cumulativeWeights = loadedCumulativeWeights;
        this.totalWeight = runningWeight;
        log.info("Loaded {} fish catch entries ({} errors)", parsed.size(), errorCount);
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
