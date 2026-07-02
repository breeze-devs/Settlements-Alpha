package dev.breezes.settlements.application.enchanting.engine;

import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import dev.breezes.settlements.domain.entities.Expertise;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoolFilter {

    public static List<EnchantmentCostData> filter(@Nonnull Collection<EnchantmentCostData> allCosts,
                                                   @Nonnull ItemStack item,
                                                   @Nonnull List<Holder<Enchantment>> appliedThisSession,
                                                   @Nonnull Expertise villagerTier,
                                                   int remainingBudget,
                                                   @Nonnull RegistryAccess registryAccess) {
        return allCosts.stream()
                .filter(cost -> isItemEligible(cost, item, registryAccess))
                .filter(cost -> isSessionCompatible(cost, appliedThisSession, registryAccess))
                .filter(cost -> isTierEligible(cost, villagerTier))
                .filter(cost -> isAffordable(cost, remainingBudget))
                .toList();
    }

    private static boolean isItemEligible(@Nonnull EnchantmentCostData cost,
                                          @Nonnull ItemStack item,
                                          @Nonnull RegistryAccess registryAccess) {
        if (item.is(Items.BOOK) || item.is(Items.ENCHANTED_BOOK)) {
            return true;
        }

        return resolveEnchantment(cost.enchantmentId(), registryAccess)
                .map(item::supportsEnchantment)
                .orElse(false);
    }

    private static boolean isSessionCompatible(@Nonnull EnchantmentCostData cost,
                                               @Nonnull List<Holder<Enchantment>> appliedThisSession,
                                               @Nonnull RegistryAccess registryAccess) {
        Optional<Holder.Reference<Enchantment>> candidateHolder = resolveEnchantment(cost.enchantmentId(), registryAccess);
        if (candidateHolder.isEmpty()) {
            return false;
        }

        for (Holder<Enchantment> applied : appliedThisSession) {
            if (!Enchantment.areCompatible(candidateHolder.get(), applied)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTierEligible(@Nonnull EnchantmentCostData cost, @Nonnull Expertise villagerTier) {
        return cost.minTierOrdinal() <= villagerTier.ordinal();
    }

    private static boolean isAffordable(@Nonnull EnchantmentCostData cost, int remainingBudget) {
        return cost.baseCost() <= remainingBudget;
    }

    public static Optional<Holder.Reference<Enchantment>> resolveEnchantment(@Nonnull String enchantmentId,
                                                                             @Nonnull RegistryAccess registryAccess) {
        ResourceLocation location = ResourceLocation.parse(enchantmentId);
        return registryAccess.registryOrThrow(Registries.ENCHANTMENT).getHolder(location);
    }

}
