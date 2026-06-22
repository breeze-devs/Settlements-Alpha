package dev.breezes.settlements.domain.ai.navigation;

import dev.breezes.settlements.domain.world.location.Location;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface INavigationManager<T> {

    /**
     * Get the target position if currently navigating
     */
    Optional<Location> getNavigationTarget();

    boolean isNavigating();

    void stop();

    /**
     * Navigate to the target position with the given navigation intent and completion range.
     * <p>
     * The manager always attempts the move; reachability decisions belong to the step (e.g.
     * {@code NavigateToTargetStep}) so it can react appropriately rather than silently doing nothing.
     */
    void navigateTo(@Nonnull Location target, @Nonnull NavigationType type, int completionRange);

    /**
     * Same as {@link #navigateTo(Location, NavigationType, int)} but with the default walking intent.
     */
    void walkTo(@Nonnull Location target, int completionRange);

    float speedFor(@Nonnull NavigationType type);

    /**
     * Whether the position is reachable within the given maximum distance
     */
    boolean canReach(@Nonnull Location target, double distance);

}
