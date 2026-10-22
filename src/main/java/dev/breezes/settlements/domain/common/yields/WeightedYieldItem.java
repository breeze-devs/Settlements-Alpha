package dev.breezes.settlements.domain.common.yields;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

/**
 * A single weighted drop candidate within a {@link WeightedYieldPool}.
 */
@Builder
public record WeightedYieldItem(
        ResourceLocation item,
        double weight,
        int minCount,
        int maxCount
) {

    public WeightedYieldItem {
        minCount = Math.max(1, minCount);
        maxCount = Math.max(minCount, maxCount);
        if (weight < 0) {
            throw new IllegalArgumentException("Weighted yield item '" + item + "' must not have a negative weight");
        }
    }

}
