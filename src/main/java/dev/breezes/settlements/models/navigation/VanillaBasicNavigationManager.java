package dev.breezes.settlements.models.navigation;

import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.PathfinderMob;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor
public class VanillaBasicNavigationManager<T extends PathfinderMob> implements INavigationManager<T> {

    private final T entity;

    @Override
    public Optional<Location> getNavigationTarget() {
        return Optional.ofNullable(this.entity.getNavigation().getTargetPos())
                .map(blockPos -> Location.of(blockPos, this.entity.level()));
    }

    @Override
    public boolean isNavigating() {
        return entity.getNavigation().isInProgress();
    }

    @Override
    public void stop() {
        this.entity.getNavigation().stop();
    }

    @Override
    public void navigateTo(@Nonnull Location target, float speed, int completionRange) {
        if (!this.canReach(target, completionRange)) {
            return;
        }
        this.entity.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speed);
    }

    @Override
    public void walkTo(@Nonnull Location target, int completionRange) {
        this.navigateTo(target, 0.5F, completionRange);
    }

    @Override
    public boolean canReach(@Nonnull Location target, double distance) {
        // TODO: implement this
        return true;
    }

    @Override
    public float getWalkSpeed() {
        return 0.4f;
    }

    @Override
    public float getRunSpeed() {
        return 0.7f;
    }

}
