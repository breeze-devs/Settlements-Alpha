package dev.breezes.settlements.infrastructure.minecraft.data.framework;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base for datapack managers whose files are keyed by profession and carry a list of
 * profession-scoped values (craft recipes, stock policies, etc). Handles the id-merge across files
 * that all such catalogs need; subclasses only supply the value codec and an id extractor.
 */
public abstract class ProfessionCatalogDataManager<T> extends CodecJsonDataManager<ProfessionCatalogFile<T>> {

    private Map<VillagerProfessionKey, List<T>> byProfession = Map.of();

    protected ProfessionCatalogDataManager(@Nonnull String directory, @Nonnull Codec<T> valueCodec, @Nonnull String valuesField) {
        super(directory, ProfessionCatalogFile.codec(valueCodec, valuesField));
    }

    @Override
    protected final void onReloaded(@Nonnull Map<ResourceLocation, ProfessionCatalogFile<T>> values) {
        // Keyed by value id -- a later file overrides an earlier value by re-declaring the same id,
        // while otherwise preserving first-seen ordering within a profession.
        Map<VillagerProfessionKey, LinkedHashMap<String, T>> merged = new LinkedHashMap<>();

        for (ProfessionCatalogFile<T> file : values.values()) {
            LinkedHashMap<String, T> professionValues = merged.computeIfAbsent(file.profession(), ignored -> new LinkedHashMap<>());
            for (T value : file.values()) {
                professionValues.put(this.idOf(value), value);
            }
        }

        Map<VillagerProfessionKey, List<T>> immutable = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, T>> entry : merged.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        this.byProfession = Collections.unmodifiableMap(immutable);
        this.onCatalogReloaded(this.byProfession);
    }

    /**
     * Extracts the stable id used to de-duplicate values within a profession across files.
     */
    protected abstract String idOf(T value);

    /**
     * Hook for subclasses that build derived projections (e.g. offer/demand/supply views) on top of
     * the merged catalog. No-op by default.
     */
    protected void onCatalogReloaded(@Nonnull Map<VillagerProfessionKey, List<T>> byProfession) {
    }

    protected final List<T> valuesFor(@Nonnull VillagerProfessionKey profession) {
        return this.byProfession.getOrDefault(profession, List.of());
    }

    protected final Map<VillagerProfessionKey, List<T>> byProfession() {
        return this.byProfession;
    }

}
