package dev.breezes.settlements.domain.forge.catalog;

import dev.breezes.settlements.domain.crafting.catalog.ProfessionRecipeCatalog;

/**
 * Recipe source for the anvil-forge station, distinct from {@link dev.breezes.settlements.domain.crafting.catalog.CraftCatalogRegistry}
 * so Dagger can disambiguate the two catalogs by type — no qualifier needed.
 */
public interface ForgeCatalogRegistry extends ProfessionRecipeCatalog {
}
