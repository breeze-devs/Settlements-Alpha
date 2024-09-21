package dev.breezes.settlements.models.navigation;

import dev.breezes.settlements.entities.ISettlementsEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface INavigationManager<T extends ISettlementsEntity> {

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
    void navigateTo(@Nonnull Vec3 target, float speed, int completionRange);

    /**
     * Whether the position is reachable within the given maximum distance
     */
    boolean isReachable(@Nonnull Vec3 target, double distance);


}
