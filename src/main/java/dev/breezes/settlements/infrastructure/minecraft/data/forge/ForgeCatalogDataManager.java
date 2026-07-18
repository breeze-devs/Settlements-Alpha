package dev.breezes.settlements.infrastructure.minecraft.data.forge;

import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipeCodec;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.forge.catalog.ForgeCatalogRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.ProfessionCatalogDataManager;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public class ForgeCatalogDataManager extends ProfessionCatalogDataManager<CraftRecipe> implements ForgeCatalogRegistry {

    private static final String DIRECTORY_PATH = "settlements/forge_catalog";

    @Inject
    public ForgeCatalogDataManager() {
        super(DIRECTORY_PATH, CraftRecipeCodec.CODEC, "recipes");
    }

    @Override
    protected String label() {
        return "forge catalog";
    }

    @Override
    protected String idOf(CraftRecipe value) {
        return value.id();
    }

    @Override
    public List<CraftRecipe> recipesFor(@Nonnull VillagerProfessionKey profession) {
        return this.valuesFor(profession);
    }

    /**
     * The currently loaded recipes keyed by profession — exposed read-only for boot-time cross-catalog
     * validation (see {@code RecipeCatalogValidationReloadListener}).
     */
    public Map<VillagerProfessionKey, List<CraftRecipe>> loadedRecipes() {
        return this.byProfession();
    }

}
