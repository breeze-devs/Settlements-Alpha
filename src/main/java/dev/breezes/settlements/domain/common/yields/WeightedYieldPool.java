package dev.breezes.settlements.domain.common.yields;

import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A set of weighted item candidates, rolled independently {@code rolls} times per invocation.
 */
@Builder
public record WeightedYieldPool(
        int rolls,
        @Nonnull List<WeightedYieldItem> items
) {

    public WeightedYieldPool {
        rolls = rolls <= 0 ? 1 : rolls;
        items = List.copyOf(items);

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Weighted yield pool must declare at least one item");
        }

        if (items.stream().noneMatch(item -> item.weight() > 0)) {
            throw new IllegalArgumentException("Weighted yield pool must contain at least one item with weight > 0");
        }
    }

}
