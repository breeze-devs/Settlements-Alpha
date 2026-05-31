package dev.breezes.settlements.infrastructure.minecraft.navigation;

import dev.breezes.settlements.domain.ai.navigation.AgilitySpeedResolver;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor
public class VanillaMemoryNavigationManager<T extends BaseVillager> implements INavigationManager<T> {

    private final T villager;

    @Override
    public Optional<Location> getNavigationTarget() {
        return villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .map(WalkTarget::getTarget)
                .map(PositionTracker::currentBlockPosition)
                .map(blockPos -> Location.of(blockPos, villager.level()));
    }

    @Override
    public boolean isNavigating() {
        return this.villager.getNavigation().isInProgress();
    }

    @Override
    public void stop() {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getNavigation().stop();
    }

    @Override
    public void navigateTo(@Nonnull Location target, @Nonnull NavigationType type, int completionRange) {
        if (!this.canReach(target, completionRange)) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(target.toBlockPos(), this.speedFor(type), completionRange));
    }

    @Override
    public void walkTo(@Nonnull Location target, int completionRange) {
        this.navigateTo(target, NavigationType.WALK, completionRange);
    }

    @Override
    public float speedFor(@Nonnull NavigationType type) {
        double agility = this.villager.getGenetics().getGeneValue(GeneType.AGILITY);
        return AgilitySpeedResolver.resolve(type, agility);
    }

    @Override
    public boolean canReach(@Nonnull Location target, double distance) {
        // TODO: implement this
        return true;
    }

}
