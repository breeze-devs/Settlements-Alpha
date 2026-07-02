package dev.breezes.settlements.domain.enchanting;

import dev.breezes.settlements.domain.entities.Expertise;
import lombok.Builder;

@Builder
public record EnchantmentCostData(
        String enchantmentId,
        int baseCost,
        int levelMultiplier,
        int maxLevel,
        Expertise minTier
) {

    public int costForLevel(int level) {
        return this.baseCost + ((level - 1) * this.levelMultiplier);
    }

    public int minTierOrdinal() {
        return this.minTier.ordinal();
    }

}
