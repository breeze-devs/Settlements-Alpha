package dev.breezes.settlements.domain.common.yields;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A full weighted drop table for one source block: the block it applies to, the weight used when
 * a villager chooses between several known block sources, and its per-expertise pools.
 */
@Builder
public record WeightedYieldTable(
        @Nonnull String block,
        double selectionWeight,
        @Nonnull Map<String, WeightedYieldPool> pools
) {

    public WeightedYieldTable {
        if (StringUtils.isBlank(block)) {
            throw new IllegalArgumentException("Weighted yield table must declare a non-blank block id");
        }

        pools = Map.copyOf(pools);
        if (pools.isEmpty()) {
            throw new IllegalArgumentException("Weighted yield table '" + block + "' must declare at least one expertise pool");
        }

        if (selectionWeight <= 0) {
            throw new IllegalArgumentException("Weighted yield table '" + block + "' must have a positive selection weight");
        }
    }

}
