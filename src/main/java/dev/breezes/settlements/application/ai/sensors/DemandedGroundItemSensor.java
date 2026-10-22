package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.CollectDemandedItemBehavior;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Throttled per-villager scan that remembers a few reachable demanded items nearby.
 */
public final class DemandedGroundItemSensor extends AbstractSensor<BaseVillager> {

    // Narrow opportunistic-pickup radius
    private static final double SCAN_RANGE_HORIZONTAL = 8.0D;
    private static final double SCAN_RANGE_VERTICAL = 2.0D;

    private static final int MAX_ITEMS = 5;

    // Bounds the path searches one scan spends when many wanted items are out of reach
    private static final int MAX_PATH_SEARCHES = 2 * MAX_ITEMS;

    private final DemandEvaluator demandEvaluator;

    public DemandedGroundItemSensor(@Nonnull DemandedGroundItemSensorConfig config,
                                    @Nonnull DemandEvaluator demandEvaluator) {
        super(List.of(), Tickable.staggered(ClockTicks.seconds(config.scanIntervalSeconds())));
        this.demandEvaluator = demandEvaluator;
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        List<ItemEntity> reachable = this.findReachable(entity);
        return List.of(reachable.isEmpty()
                ? MemoryWrite.clear(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS)
                : MemoryWrite.of(MemoryTypeRegistry.DEMANDED_GROUND_ITEMS, reachable));
    }

    private List<ItemEntity> findReachable(@Nonnull BaseVillager villager) {
        List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
        if (demands.isEmpty()) {
            return List.of();
        }

        AABB box = villager.getBoundingBox().inflate(SCAN_RANGE_HORIZONTAL, SCAN_RANGE_VERTICAL, SCAN_RANGE_HORIZONTAL);
        List<ItemEntity> items = new ArrayList<>(villager.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive));
        if (items.isEmpty()) {
            return List.of();
        }
        items.sort(Comparator.comparingDouble(villager::distanceToSqr));

        // Demands outer, items inner, so the highest-priority demand's items come first even when a closer item only
        // satisfies a lower-priority demand: a distant egg the villager urgently wants beats a nearby wheat it wants less
        List<ItemEntity> reachable = new ArrayList<>();
        Set<ItemEntity> searched = new HashSet<>();
        for (ActiveDemand demand : demands) {
            for (ItemEntity item : items) {
                if (reachable.size() >= MAX_ITEMS || searched.size() >= MAX_PATH_SEARCHES) {
                    return List.copyOf(reachable);
                }
                // An item matching several demands is path-searched once
                if (!ItemMatches.test(demand.match(), item.getItem()) || !searched.add(item)) {
                    continue;
                }
                if (villager.getNavigationManager().canReach(Location.fromEntity(item, false),
                        CollectDemandedItemBehavior.NAVIGATION_COMPLETION_DISTANCE)) {
                    reachable.add(item);
                }
            }
        }
        return List.copyOf(reachable);
    }

}
