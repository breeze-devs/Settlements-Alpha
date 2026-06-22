package dev.breezes.settlements.domain.ai.navigation;

import dev.breezes.settlements.domain.world.location.Location;

import javax.annotation.Nonnull;

/**
 * Domain seam for querying whether a location is pathfinder-reachable within a given completion range
 */
@FunctionalInterface
public interface ReachabilityChecker {

    boolean canReach(@Nonnull Location target, int completionRange);

}
