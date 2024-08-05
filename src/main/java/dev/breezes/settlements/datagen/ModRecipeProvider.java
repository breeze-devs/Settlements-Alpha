package dev.breezes.settlements.datagen;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    private static final List<ItemLike> SAPPHIRE_SMELTABLES = List.of(ItemRegistry.RAW_SAPPHIRE.get());

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(@Nonnull RecipeOutput recipeOutput) {
        oreSmelting(recipeOutput, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 0.7F, 200, "sapphire");
        oreBlasting(recipeOutput, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 0.7F, 100, "sapphire");

        // Sapphire x9 -> Sapphire Block
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.SAPPHIRE_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ItemRegistry.SAPPHIRE.get())
                .unlockedBy(getHasName(ItemRegistry.SAPPHIRE.get()), has(ItemRegistry.SAPPHIRE.get()))
                .save(recipeOutput);

        // Sapphire Block -> Sapphire x9
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 9)
                .requires(BlockRegistry.SAPPHIRE_BLOCK.get())
                .unlockedBy(getHasName(BlockRegistry.SAPPHIRE_BLOCK.get()), has(BlockRegistry.SAPPHIRE_BLOCK.get()))
                .save(recipeOutput);
    }

    protected static void oreSmelting(@Nonnull RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ingredients.toArray(new ItemLike[0])), category, result, experience, cookingTime)
                .group(group)
                .unlockedBy(getHasName(ingredients.get(0)), has(ingredients.get(0)))
                .save(recipeOutput, "%s:%s_from_smelting".formatted(SettlementsMod.MOD_ID, getItemName(result)));
//        oreCooking(recipeOutput, RecipeSerializer.SMELTING_RECIPE, ingredients, category, result, experience, cookingTime, group, "_from_smelting");
    }

    protected static void oreBlasting(@Nonnull RecipeOutput recipeOutput, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ingredients.toArray(new ItemLike[0])), category, result, experience, cookingTime)
                .group(group)
                .unlockedBy(getHasName(ingredients.get(0)), has(ingredients.get(0)))
                .save(recipeOutput, "%s:%s_from_blasting".formatted(SettlementsMod.MOD_ID, getItemName(result)));
//        oreCooking(recipeOutput, RecipeSerializer.BLASTING_RECIPE, ingredients, category, result, experience, cookingTime, group, "_from_blasting");
    }

}
