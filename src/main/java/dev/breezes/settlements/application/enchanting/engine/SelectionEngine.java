package dev.breezes.settlements.application.enchanting.engine;

import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SelectionEngine {

    public static EnchantmentCostData selectWeighted(@Nonnull Map<EnchantmentCostData, Double> weightedPool) {
        return RandomUtil.weightedChoice(weightedPool);
    }

    public static int calculateLevel(@Nonnull EnchantmentCostData cost, int remainingBudget) {
        int maxAffordable = findMaxAffordableLevel(cost, remainingBudget);
        int capped = Math.min(maxAffordable, cost.maxLevel());
        if (capped <= 1) {
            return 1;
        }

        // Roll twice and take the higher result, expected value is 0.667 * maxLevel
        double roll1 = RandomUtil.randomDouble(1, capped);
        double roll2 = RandomUtil.randomDouble(1, capped);
        int result = (int) Math.round(Math.max(roll1, roll2));

        // Ensure we don't return a 0 level
        return Math.max(1, result);
    }

    private static int findMaxAffordableLevel(@Nonnull EnchantmentCostData cost, int remainingBudget) {
        // Single-level enchantments
        if (cost.levelMultiplier() == 0) {
            return cost.baseCost() <= remainingBudget ? 1 : 0;
        }

        int maxLevel = ((remainingBudget - cost.baseCost()) / cost.levelMultiplier()) + 1;
        return Math.max(0, maxLevel);
    }

}
