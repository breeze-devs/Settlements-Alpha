package dev.breezes.settlements.infrastructure.minecraft.data.enchanting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class SpecializationDataManager extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY_PATH = "settlements/specializations";

    private static final Gson GSON = new GsonBuilder().create();

    private Map<String, SpecializationProfile> profilesById = Map.of();

    @Inject
    public SpecializationDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<String, SpecializationProfile> parsed = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                SpecializationProfile profile = GSON.fromJson(json, SpecializationProfile.class);
                if (profile == null || profile.getId() == null) {
                    log.warn("Invalid specialization entry in '{}': missing required fields", fileId);
                    continue;
                }
                if (parsed.containsKey(profile.getId())) {
                    log.warn("Duplicate specialization ID '{}' from file '{}', overwriting",
                            profile.getId(), fileId);
                }
                parsed.put(profile.getId(), profile);
            } catch (Exception e) {
                log.warn("Failed to parse specialization from file '{}': {}", fileId, e.getMessage());
            }
        }

        this.profilesById = Map.copyOf(parsed);
        log.info("Loaded {} specialization profiles", parsed.size());
    }

    public Optional<SpecializationProfile> getProfile(@Nonnull String id) {
        return Optional.ofNullable(this.profilesById.get(id));
    }

}
