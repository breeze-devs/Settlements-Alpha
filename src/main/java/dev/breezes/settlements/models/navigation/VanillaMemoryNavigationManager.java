package dev.breezes.settlements.models.navigation;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

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
    public void navigateTo(@Nonnull Location target, float speed, int completionRange) {
        if (!this.isReachable(target, completionRange)) {
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target.toBlockPos(), speed, completionRange));
    }

    @Override
    public void walkTo(@Nonnull Location target, int completionRange) {
        this.navigateTo(target, 0.5F, completionRange);
    }

    @Override
    public boolean isReachable(@Nonnull Location target, double distance) {
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
