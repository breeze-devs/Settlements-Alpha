package dev.breezes.settlements.domain.ai.override;

import lombok.Builder;

/**
 * An override policy's priority.
 * <p>
 * Two policies must never share the same precedence.
 *
 * @param tier  primary priority; higher-priority tiers take precedence
 * @param order priority within the tier; lower values take precedence
 */
@Builder
public record OverridePrecedence(OverrideTier tier, int order) implements Comparable<OverridePrecedence> {

    @Override
    public int compareTo(OverridePrecedence other) {
        int tierComparison = this.tier.compareTo(other.tier);
        if (tierComparison != 0) {
            return tierComparison;
        }

        return Integer.compare(this.order, other.order);
    }

}
