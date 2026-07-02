package dev.breezes.settlements.application.enchanting.engine;

import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.domain.enchanting.EnchantingExpertiseDefinition;
import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.infrastructure.minecraft.data.enchanting.EnchantmentCostDataManager;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class EnchantmentEngine {

    private final EnchantmentCostDataManager costDataManager;

    public EnchantmentEngine(@Nonnull EnchantmentCostDataManager costDataManager) {
        this.costDataManager = costDataManager;
    }

    public ItemStack enchant(@Nonnull ItemStack item,
                             @Nonnull Expertise expertise,
                             double intelligence,
                             int bookshelfCount,
                             @Nullable SpecializationProfile specialization,
                             @Nonnull RegistryAccess registryAccess) {
        EnchantingExpertiseDefinition definition = EnchantingExpertiseDefinition.of(expertise);
        int totalBudget = PowerBudgetCalculator.calculate(definition.getBasePower(), intelligence, bookshelfCount);

        int remainingBudget = totalBudget;
        int rollsPerformed = 0;
        List<Holder<Enchantment>> appliedThisSession = new ArrayList<>();

        ItemStack result = item.copy();
        result.setCount(1);
        if (result.is(Items.BOOK)) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
        }

        List<EnchantmentCostData> pool = PoolFilter.filter(this.costDataManager.getAllCosts(), result,
                appliedThisSession, expertise, remainingBudget, registryAccess);
        log.behaviorStatus("Enchanting session starts for item {}: expertise={}, intelligence={}, bookshelves={}, budget={}, rolls={}, pool={}",
                item, expertise, intelligence, bookshelfCount, totalBudget, definition.getMaxRolls(), pool.size());

        while (rollsPerformed < definition.getMaxRolls()) {
            if (pool.isEmpty()) {
                log.behaviorStatus("Enchanting session ended: pool empty after {} rolls, {} budget remaining",
                        rollsPerformed, remainingBudget);
                break;
            }

            Map<EnchantmentCostData, Double> weightedPool = PoolWeightResolver.resolve(pool, specialization);
            if (weightedPool.isEmpty()) {
                log.behaviorStatus("Enchanting session ended: weighted pool empty after {} rolls", rollsPerformed);
                break;
            }

            EnchantmentCostData selected = SelectionEngine.selectWeighted(weightedPool);
            int level = SelectionEngine.calculateLevel(selected, remainingBudget);
            int cost = selected.costForLevel(level);

            Optional<Holder.Reference<Enchantment>> enchantmentHolder =
                    PoolFilter.resolveEnchantment(selected.enchantmentId(), registryAccess);
            if (enchantmentHolder.isEmpty()) {
                log.behaviorWarn("Could not resolve enchantment '{}', skipping", selected.enchantmentId());
                rollsPerformed++;
                continue;
            }

            result.enchant(enchantmentHolder.get(), level);
            appliedThisSession.add(enchantmentHolder.get());
            remainingBudget -= cost;
            rollsPerformed++;

            // Re-filter from the previous pool (strictly shrinking — budget decreased, conflicts grew)
            pool = PoolFilter.filter(pool, result, appliedThisSession, expertise, remainingBudget, registryAccess);

            log.behaviorStatus("Roll {}: {} lv.{} (cost={}, remaining={}, pool={})",
                    rollsPerformed, selected.enchantmentId(), level, cost, remainingBudget, pool.size());
        }

        result.set(DataComponentRegistry.VILLAGER_ENCHANT_ATTEMPTED.get(), true);
        log.behaviorStatus("Enchanting session complete: {} rolls, {} total budget, {} remaining",
                rollsPerformed, totalBudget, remainingBudget);
        return result;
    }

}
