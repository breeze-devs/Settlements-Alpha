package dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.crafting.catalog.CraftIngredient;
import dev.breezes.settlements.domain.crafting.catalog.CraftRecipe;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The atomic economic transaction shared by every recipe-producing sequence step:
 * recompute the live batch, consume inputs, bank the output, record the deed, and reward XP —
 * all in one commit with no cosmetic side effects in between.
 * <p>
 * Stateless and reusable across callers. The batch is recomputed here rather than trusted from the
 * precondition check because the villager's inventory may have changed between behavior-start
 * selection and arrival at the station.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class RecipeCommit {

    /**
     * Commits the transaction for {@code recipe} against the villager in {@code context}, or signals
     * the caller to bail (empty result) when the recipe is missing or no longer craftable. Callers must
     * commit before playing any cosmetic beats so an interrupted animation can never strand consumed
     * inputs — the transaction is already final by the time this returns.
     */
    public static Optional<Result> commit(@Nonnull BehaviorContext<BaseVillager> context,
                                          @Nullable CraftRecipe recipe,
                                          @Nonnull CraftBatchCalculator batchCalculator,
                                          @Nonnull WorldEventType deedType,
                                          @Nonnull Consumer<BaseVillager> experienceRewarder) {
        if (recipe == null) {
            return Optional.empty();
        }

        BaseVillager villager = context.getInitiator();
        int liveBatch = batchCalculator.computeBatch(villager, recipe);
        if (liveBatch < 1) {
            return Optional.empty();
        }

        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output().itemId());
        int outputTotal = recipe.output().count() * liveBatch;

        VillagerInventory inventory = villager.getSettlementsInventory();
        for (CraftIngredient input : recipe.inputs()) {
            inventory.consumeMatching(input.match(), input.count() * liveBatch);
        }
        inventory.add(new ItemStack(outputItem, outputTotal));

        BehaviorOutcome outcome = BehaviorOutcome.forDeed(deedType, null);
        outcome.putDetailField("item", BuiltInRegistries.ITEM.getKey(outputItem).getPath());
        outcome.putDetailField("count", Integer.toString(outputTotal));
        outcome.markSucceeded();
        context.declarePrimaryDeed(outcome);

        experienceRewarder.accept(villager);

        return Optional.of(new Result(outputItem, outputTotal));
    }

    public record Result(@Nonnull Item outputItem, int outputTotal) {
    }

}
