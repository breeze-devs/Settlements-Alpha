package dev.breezes.settlements.infrastructure.minecraft.data.smelting;

import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipe;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipeCodec;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipeRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.KeyedCatalogDataManager;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class BlastOreRecipeDataManager extends KeyedCatalogDataManager<ResourceLocation, BlastOreRecipe> implements BlastOreRecipeRegistry {

    private static final String DIRECTORY_PATH = "settlements/blast_recipes";

    @Inject
    public BlastOreRecipeDataManager() {
        super(DIRECTORY_PATH, BlastOreRecipeCodec.CODEC);
    }

    @Override
    protected String label() {
        return "blast ore recipe";
    }

    @Override
    protected ResourceLocation keyOf(@Nonnull BlastOreRecipe value) {
        return value.input();
    }

    @Override
    public List<BlastOreRecipe> allRecipes() {
        return List.copyOf(this.all().values());
    }

    @Override
    public Optional<BlastOreRecipe> forInput(@Nonnull ResourceLocation input) {
        return this.find(input);
    }

}
