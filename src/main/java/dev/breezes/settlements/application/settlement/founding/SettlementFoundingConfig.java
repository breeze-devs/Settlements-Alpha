package dev.breezes.settlements.application.settlement.founding;

import dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim;
import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

/**
 * What a bell needs around it to found a settlement, and how many residents keep a settlement healthy.
 *
 * @param minimumFoundingChunks the fewest chunks a founding must be able to claim
 * @param minimumResidents      the fewest unassigned Settlements villagers who must hold a bell as their meeting point
 *                              to found a settlement there, and the fewest living members a healthy settlement has
 */
@BehaviorConfig(name = "settlement_founding", type = ConfigurationType.FEATURE)
public record SettlementFoundingConfig(
        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "minimum_founding_chunks",
                description = "Fewest chunks a settlement founded at a bell must be able to claim. " +
                        "Chunks already claimed by other settlements are excluded.",
                defaultValue = 9,
                min = 1,
                max = SettlementFoundingClaim.MIN_SQUARE_CHUNKS)
        int minimumFoundingChunks,

        // At most a bell's 32 meeting tickets: every founder must hold the bell as its meeting point, so a larger
        // minimum could never found
        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "minimum_residents",
                description = "Minimum Settlements villagers belonging to no settlement who must share a bell " +
                        "to found a settlement at it. " +
                        "A settlement with fewer living members than this is considered unhealthy and fades.",
                defaultValue = 2,
                min = 1,
                max = 32)
        int minimumResidents
) {

}
