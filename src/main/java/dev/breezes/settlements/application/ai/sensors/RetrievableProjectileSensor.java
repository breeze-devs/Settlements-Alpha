package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.AbstractArrowInvoker;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Throttled scan that remembers, for a fletcher, up to a few stuck projectiles nearby that it can walk to and retrieve.
 * Other villagers never scan, and the memory stays clear for them.
 */
public final class RetrievableProjectileSensor extends AbstractSensor<BaseVillager> {

    private static final double SCAN_RANGE_HORIZONTAL = 32.0D;
    private static final double SCAN_RANGE_VERTICAL = 8.0D;

    // Matches RetrieveProjectilesBehavior's navigation completion distance
    private static final int COMPLETION_RANGE = 1;
    private static final int MAX_PROJECTILES = 5;

    // Bounds the path searches one scan spends when many projectiles are out of reach, such as on roofs after a raid
    private static final int MAX_PATH_SEARCHES = 2 * MAX_PROJECTILES;

    public RetrievableProjectileSensor(@Nonnull RetrievableProjectileSensorConfig config) {
        super(List.of(), Tickable.staggered(ClockTicks.seconds(config.scanIntervalSeconds())));
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        if (!VillagerProfessionKey.FLETCHER.equals(entity.getProfession())) {
            return List.of(MemoryWrite.clear(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES));
        }

        List<AbstractArrow> reachable = findReachable(entity);
        return List.of(reachable.isEmpty()
                ? MemoryWrite.clear(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES)
                : MemoryWrite.of(MemoryTypeRegistry.RETRIEVABLE_PROJECTILES, reachable));
    }

    private static List<AbstractArrow> findReachable(@Nonnull BaseVillager villager) {
        AABB box = villager.getBoundingBox().inflate(SCAN_RANGE_HORIZONTAL, SCAN_RANGE_VERTICAL, SCAN_RANGE_HORIZONTAL);
        List<AbstractArrow> projectiles = villager.level().getEntitiesOfClass(AbstractArrow.class, box,
                RetrievableProjectileSensor::isRetrievable);

        List<AbstractArrow> reachable = new ArrayList<>();
        int searches = 0;
        for (AbstractArrow projectile : projectiles) {
            if (reachable.size() >= MAX_PROJECTILES || searches >= MAX_PATH_SEARCHES) {
                break;
            }
            searches++;
            if (villager.getNavigationManager().canReach(Location.fromEntity(projectile, false), COMPLETION_RANGE)) {
                reachable.add(projectile);
            }
        }
        return List.copyOf(reachable);
    }

    private static boolean isRetrievable(@Nonnull AbstractArrow projectile) {
        return projectile.getType().is(SettlementsEntityTypeTags.RETRIEVABLE_PROJECTILES)
                && projectile.isAlive()
                && ((AbstractArrowInvoker) projectile).accessInGround();
    }

}
