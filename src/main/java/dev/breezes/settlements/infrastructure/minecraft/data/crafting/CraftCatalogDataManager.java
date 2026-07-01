package dev.breezes.settlements.infrastructure.minecraft.data.crafting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.breezes.settlements.domain.crafting.catalog.CraftCatalogRegistry;
import dev.breezes.settlements.domain.crafting.catalog.CraftIngredient;
import dev.breezes.settlements.domain.crafting.catalog.CraftOutput;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CustomLog
public class CraftCatalogDataManager extends SimpleJsonResourceReloadListener implements CraftCatalogRegistry {

    private static final String DIRECTORY_PATH = "settlements/craft_catalog";
    private static final Gson GSON = new GsonBuilder().create();

    private Map<VillagerProfessionKey, List<CraftRecipe>> recipesByProfession = Map.of();

    @Inject
    public CraftCatalogDataManager() {
        super(GSON, DIRECTORY_PATH);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> entries,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        List<CraftCatalogDefinition> definitions = new ArrayList<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : entries.entrySet()) {
            try {
                CraftCatalogFile file = GSON.fromJson(entry.getValue(), CraftCatalogFile.class);
                if (file == null) {
                    throw new IllegalArgumentException("parsed entry was null");
                }
                definitions.add(parseFile(file));
            } catch (Exception exception) {
                log.warn("Failed to parse craft catalog from file '{}': {}", entry.getKey(), exception.getMessage());
                errorCount++;
            }
        }

        this.recipesByProfession = buildSnapshot(definitions);

        log.info("Loaded craft catalog for {} professions ({} errors)",
                this.recipesByProfession.size(), errorCount);
    }

    @VisibleForTesting
    public void loadForTest(@Nonnull Map<ResourceLocation, JsonElement> entries) {
        this.apply(entries, null, null);
    }

    @Override
    public List<CraftRecipe> recipesFor(@Nonnull VillagerProfessionKey profession) {
        return this.recipesByProfession.getOrDefault(profession, List.of());
    }

    /**
     * The currently loaded recipes keyed by profession — exposed read-only for boot-time cross-catalog
     * validation (see {@code CraftCatalogValidationReloadListener}).
     */
    public Map<VillagerProfessionKey, List<CraftRecipe>> loadedRecipes() {
        return Collections.unmodifiableMap(this.recipesByProfession);
    }

    private static Map<VillagerProfessionKey, List<CraftRecipe>> buildSnapshot(@Nonnull List<CraftCatalogDefinition> definitions) {
        // Keyed by recipe id so that, as with the trade catalog, a later datapack file may replace an
        // earlier recipe by re-declaring the same id rather than duplicating it.
        Map<VillagerProfessionKey, LinkedHashMap<String, CraftRecipe>> recipes = new LinkedHashMap<>();

        for (CraftCatalogDefinition definition : definitions) {
            LinkedHashMap<String, CraftRecipe> professionRecipes = recipes.computeIfAbsent(definition.profession(), ignored -> new LinkedHashMap<>());
            for (CraftRecipe recipe : definition.recipes()) {
                professionRecipes.put(recipe.id(), recipe);
            }
        }

        Map<VillagerProfessionKey, List<CraftRecipe>> immutable = new LinkedHashMap<>();
        for (Map.Entry<VillagerProfessionKey, LinkedHashMap<String, CraftRecipe>> entry : recipes.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }
        return immutable;
    }

    private static CraftCatalogDefinition parseFile(@Nonnull CraftCatalogFile file) {
        if (file.profession == null || file.profession.isBlank()) {
            throw new IllegalArgumentException("missing profession");
        }

        VillagerProfessionKey profession = VillagerProfessionKey.fromResourceLocation(ResourceLocation.parse(file.profession));
        if (file.recipes == null) {
            throw new IllegalArgumentException("missing recipes");
        }

        List<CraftRecipe> recipes = file.recipes.stream().map(CraftCatalogDataManager::parseRecipe).toList();
        return new CraftCatalogDefinition(profession, recipes);
    }

    private static CraftRecipe parseRecipe(@Nonnull RecipeFile file) {
        if (file.inputs == null || file.inputs.isEmpty()) {
            throw new IllegalArgumentException("recipe '" + file.id + "' has no inputs");
        }

        List<CraftIngredient> inputs = file.inputs.stream().map(CraftCatalogDataManager::parseIngredient).toList();
        return CraftRecipe.builder()
                .id(requireNonBlank(file.id, "recipe.id"))
                .inputs(inputs)
                .output(parseOutput(file.output))
                .priority(file.priority == null ? 0 : file.priority)
                .build();
    }

    private static CraftIngredient parseIngredient(@Nonnull IngredientFile file) {
        return new CraftIngredient(parseMatch(file.item, file.tag), requirePresent(file.count, "recipe.input.count"));
    }

    private static CraftOutput parseOutput(OutputFile file) {
        if (file == null) {
            throw new IllegalArgumentException("missing output");
        }
        return new CraftOutput(ResourceLocation.parse(requireNonBlank(file.item, "recipe.output.item")),
                requirePresent(file.count, "recipe.output.count"));
    }

    // Inputs carry the item/tag inline (not nested under a "match" object), so the reusable ItemMatch is
    // built from the flat fields exactly as the trade catalog builds it from its own match block.
    private static ItemMatch parseMatch(String item, String tag) {
        boolean hasItem = item != null && !item.isBlank();
        boolean hasTag = tag != null && !tag.isBlank();
        if (hasItem == hasTag) {
            throw new IllegalArgumentException("input must define exactly one of item or tag");
        }

        if (hasItem) {
            return new ItemMatch.ItemRef(ResourceLocation.parse(item));
        }

        return new ItemMatch.TagRef(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + fieldName);
        }
        return value;
    }

    // Boundary mapping only checks that the field is present; the domain records own the value-range
    // invariants (e.g. count >= 1), so we don't duplicate those rules here.
    private static int requirePresent(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("missing " + fieldName);
        }
        return value;
    }

    private record CraftCatalogDefinition(
            VillagerProfessionKey profession,
            List<CraftRecipe> recipes
    ) {
    }

    private static final class CraftCatalogFile {
        private String profession;
        private List<RecipeFile> recipes;
    }

    private static final class RecipeFile {
        private String id;
        private List<IngredientFile> inputs;
        private OutputFile output;
        private Integer priority;
    }

    private static final class IngredientFile {
        private String item;
        private String tag;
        private Integer count;
    }

    private static final class OutputFile {
        private String item;
        private Integer count;
    }

}
