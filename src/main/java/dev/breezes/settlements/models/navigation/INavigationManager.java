package dev.breezes.settlements.models.navigation;

import dev.breezes.settlements.models.location.Location;
import net.minecraft.core.GlobalPos;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface INavigationManager<T> {

    /**
     * Get the target position if currently navigating
     */
    Optional<GlobalPos> getNavigationTarget();

    boolean isNavigating();

    void stop();

    /**
     * Navigate to the target position with the given speed and completion range
     * <p>
     * Note that if the target is not reachable, the navigation will be cancelled
     */
    void navigateTo(@Nonnull Location target, float speed, int completionRange);

    /**
     * Same as {@link #navigateTo(Location, float, int)} but with the default walking speed
     */
    void walkTo(@Nonnull Location target, int completionRange);

    /**
     * Whether the position is reachable within the given maximum distance
     */
    boolean isReachable(@Nonnull Location target, double distance);

    float getWalkSpeed();

    float getRunSpeed();

}
