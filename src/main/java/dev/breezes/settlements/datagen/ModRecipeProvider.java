package dev.breezes.settlements.datagen;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.registry.BlockRegistry;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    private static final List<ItemLike> SAPPHIRE_SMELTABLES = List.of(ItemRegistry.RAW_SAPPHIRE.get());

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        oreSmelting(writer, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 0.7F, 200, "sapphire");
        oreBlasting(writer, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 0.7F, 100, "sapphire");

        // Sapphire x9 -> Sapphire Block
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, BlockRegistry.SAPPHIRE_BLOCK.get())
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', ItemRegistry.SAPPHIRE.get())
                .unlockedBy(getHasName(ItemRegistry.SAPPHIRE.get()), has(ItemRegistry.SAPPHIRE.get()))
                .save(writer);

        // Sapphire Block -> Sapphire x9
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemRegistry.SAPPHIRE.get(), 9)
                .requires(BlockRegistry.SAPPHIRE_BLOCK.get())
                .unlockedBy(getHasName(BlockRegistry.SAPPHIRE_BLOCK.get()), has(BlockRegistry.SAPPHIRE_BLOCK.get()))
                .save(writer);
    }

    protected static void oreSmelting(Consumer<FinishedRecipe> finishedRecipeConsumer, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        oreCooking(finishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, ingredients, category, result, experience, cookingTime, group, "_from_smelting");
    }

    protected static void oreBlasting(Consumer<FinishedRecipe> finishedRecipeConsumer, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group) {
        oreCooking(finishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, ingredients, category, result, experience, cookingTime, group, "_from_blasting");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> finishedRecipeConsumer, RecipeSerializer<? extends AbstractCookingRecipe> cookingSerializer, List<ItemLike> ingredients, RecipeCategory category, ItemLike result, float experience, int cookingTime, String group, String recipeName) {
        for (ItemLike itemlike : ingredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), category, result, experience, cookingTime, cookingSerializer)
                    .group(group)
                    .unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(finishedRecipeConsumer, "%s:%s%s_%s".formatted(SettlementsMod.MOD_ID, getItemName(result), recipeName, getItemName(itemlike)));
        }

    }


}
