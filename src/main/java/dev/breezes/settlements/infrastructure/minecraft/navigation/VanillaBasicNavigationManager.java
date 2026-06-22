package dev.breezes.settlements.infrastructure.minecraft.navigation;

import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.AllArgsConstructor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.pathfinder.Path;

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
    public void navigateTo(@Nonnull Location target, @Nonnull NavigationType type, int completionRange) {
        this.entity.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), this.speedFor(type));
    }

    @Override
    public void walkTo(@Nonnull Location target, int completionRange) {
        this.navigateTo(target, NavigationType.WALK, completionRange);
    }

    @Override
    public float speedFor(@Nonnull NavigationType type) {
        return type.getBaseModifier();
    }

    @Override
    public boolean canReach(@Nonnull Location target, double distance) {
        // Use vanilla's own pathfinder to produce a trial path
        Path path = this.entity.getNavigation().createPath(target.toBlockPos(), Mth.floor(distance));
        return path != null && path.canReach();
    }

}
