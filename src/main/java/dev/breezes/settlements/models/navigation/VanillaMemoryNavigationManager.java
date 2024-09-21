package dev.breezes.settlements.models.navigation;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Optional;

@AllArgsConstructor
public class VanillaMemoryNavigationManager<T extends BaseVillager> implements INavigationManager<T> {

    private final T villager;

    @Override
    public Optional<GlobalPos> getNavigationTarget() {
        return Optional.empty();
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
    public void navigateTo(@Nonnull Vec3 target, float speed, int completionRange) {
        if (this.isReachable(target, completionRange)) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, completionRange));
    }

    @Override
    public boolean isReachable(@Nonnull Vec3 target, double distance) {
        // TODO: implement this
        return true;
    }

}
