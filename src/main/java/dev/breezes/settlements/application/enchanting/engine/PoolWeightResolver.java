package dev.breezes.settlements.application.enchanting.engine;

import dev.breezes.settlements.domain.enchanting.EnchantmentCostData;
import dev.breezes.settlements.domain.enchanting.SpecializationProfile;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoolWeightResolver {

    public static Map<EnchantmentCostData, Double> resolve(@Nonnull List<EnchantmentCostData> filteredPool,
                                                           @Nullable SpecializationProfile profile) {
        Map<EnchantmentCostData, Double> weightedPool = new LinkedHashMap<>();
        for (EnchantmentCostData entry : filteredPool) {
            double weight = profile != null ? profile.getWeight(entry.enchantmentId()) : 1.0;
            if (weight > 0.0) {
                weightedPool.put(entry, weight);
            }
        }
        return weightedPool;
    }

}
