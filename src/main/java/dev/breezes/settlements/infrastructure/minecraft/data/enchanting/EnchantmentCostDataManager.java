package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class EnchantmentCostDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/enchantments/costs";

    private static final Gson GSON = new GsonBuilder().create();

    private Map<String, EnchantmentCostData> costsByEnchantmentId = Map.of();

    @Inject
    public EnchantmentCostDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, EnchantmentCostData> parsed = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                EnchantmentCostData data = GSON.fromJson(json, EnchantmentCostData.class);
                if (data == null || data.getEnchantmentId() == null) {
                    log.warn("Invalid enchantment cost entry in '{}': missing required fields", fileId);
                    errorCount++;
                    continue;
                }

                if (parsed.containsKey(data.getEnchantmentId())) {
                    log.error("Duplicate enchantment cost for '{}' from file '{}', overwriting",
                            data.getEnchantmentId(), fileId);
                }
                parsed.put(data.getEnchantmentId(), data);
            } catch (Exception e) {
                log.warn("Failed to parse enchantment cost from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.costsByEnchantmentId = Map.copyOf(parsed);
        log.info("Loaded {} enchantment cost entries ({} errors)", parsed.size(), errorCount);
    }

    public Optional<EnchantmentCostData> getCost(@Nonnull String enchantmentId) {
        return Optional.ofNullable(this.costsByEnchantmentId.get(enchantmentId));
    }

    public Collection<EnchantmentCostData> getAllCosts() {
        return this.costsByEnchantmentId.values();
    }

}
