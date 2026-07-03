package dev.breezes.settlements.domain.crafting.catalog;

/**
 * Recipe source for the generic crafting-grid station, distinct from {@link dev.breezes.settlements.domain.forge.catalog.ForgeCatalogRegistry}
 * so Dagger can disambiguate the two catalogs by type — no qualifier needed even though both are backed by a
 * {@code ProfessionCatalogDataManager<CraftRecipe>}.
 */
public interface CraftCatalogRegistry extends ProfessionRecipeCatalog {
}
