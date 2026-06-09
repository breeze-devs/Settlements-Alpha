package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The single point a villager should gaze at while a behavior runs, intentionally separate from the
 * navigation target. A behavior only sets this when it wants to look somewhere other than where it
 * walks; otherwise the framework falls back to the navigation target. See {@link LookQueries}.
 */
public class LookState implements BehaviorState {

    @Nullable
    private Entity entity;
    @Nullable
    private Location location;

    private LookState(@Nullable Entity entity, @Nullable Location location) {
        this.entity = entity;
        this.location = location;
    }

    public static LookState ofEntity(@Nonnull Entity entity) {
        return new LookState(entity, null);
    }

    public static LookState ofLocation(@Nonnull Location location) {
        return new LookState(null, location);
    }

    /**
     * Resolves the world position to look at this tick. For an entity target the position is re-read
     * every call so a moving target is tracked rather than snapshotted once at assignment.
     */
    public Optional<Location> resolveLocation() {
        if (this.entity != null) {
            return Optional.of(Location.fromEntity(this.entity, false));
        }
        return Optional.ofNullable(this.location);
    }

    @Override
    public void reset() {
        this.entity = null;
        this.location = null;
    }

}
