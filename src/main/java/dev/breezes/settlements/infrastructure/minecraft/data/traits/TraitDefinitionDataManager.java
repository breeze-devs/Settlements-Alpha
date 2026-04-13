package dev.breezes.settlements.infrastructure.minecraft.data.traits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.generation.model.building.DisplayInfo;
import dev.breezes.settlements.domain.generation.model.profile.TraitDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@CustomLog
public class TraitDefinitionDataManager extends SimpleJsonResourceReloadListener implements TraitRegistry {

    private static final String DIRECTORY_PATH = "settlements/traits/definitions";
    private static final Gson GSON = new GsonBuilder().create();

    private Map<TraitId, TraitDefinition> definitionsById = Map.of();

    @Inject
    public TraitDefinitionDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        Map<TraitId, TraitDefinition> parsed = new LinkedHashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList()) {
            ResourceLocation fileId = entry.getKey();

            try {
                TraitDefinitionEntry definitionEntry = GSON.fromJson(entry.getValue(), TraitDefinitionEntry.class);
                TraitDefinition definition = toDefinition(definitionEntry, fileId.toString());
                TraitDefinition previous = parsed.put(definition.id(), definition);
                if (previous != null) {
                    log.error("Duplicate trait definition id '{}', keeping later entry", definition.id());
                }
            } catch (Exception e) {
                log.warn("Failed to parse trait definition from file '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.definitionsById = Map.copyOf(parsed);
        log.info("Loaded {} trait definitions ({} errors)", this.definitionsById.size(), errorCount);
    }

    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Override
    public Set<TraitId> allTraitIds() {
        return this.definitionsById.keySet();
    }

    @Override
    public Optional<TraitDefinition> byId(TraitId id) {
        return Optional.ofNullable(this.definitionsById.get(id));
    }

    private static TraitDefinition toDefinition(TraitDefinitionEntry entry, String fileId) {
        if (entry == null) {
            throw new IllegalArgumentException("parsed entry was null");
        }
        if (entry.getId() == null || entry.getId().isBlank()) {
            throw new IllegalArgumentException("missing id");
        }

        DisplayInfo displayInfo = null;
        if (entry.getDisplayInfo() != null) {
            DisplayInfoEntry displayInfoEntry = entry.getDisplayInfo();
            if (displayInfoEntry.getDisplayName() == null || displayInfoEntry.getDisplayName().isBlank()) {
                throw new IllegalArgumentException("missing display_info.display_name");
            }
            if (displayInfoEntry.getDescription() == null || displayInfoEntry.getDescription().isBlank()) {
                throw new IllegalArgumentException("missing display_info.description");
            }
            if (displayInfoEntry.getIconItemId() == null || displayInfoEntry.getIconItemId().isBlank()) {
                throw new IllegalArgumentException("missing display_info.icon_item_id");
            }
            displayInfo = new DisplayInfo(
                    displayInfoEntry.getDisplayName(),
                    displayInfoEntry.getDescription(),
                    displayInfoEntry.getCustomName(),
                    displayInfoEntry.getIconItemId()
            );
        }

        try {
            return new TraitDefinition(TraitId.of(entry.getId()), displayInfo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid trait id in '%s': %s".formatted(fileId, e.getMessage()), e);
        }
    }

}
