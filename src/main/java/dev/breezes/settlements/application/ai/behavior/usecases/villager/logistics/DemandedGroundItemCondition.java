package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.conditions.IEntityCondition;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DemandedGroundItemCondition implements IEntityCondition<BaseVillager> {

    private static final double SCAN_RANGE_HORIZONTAL = 12.0D;
    private static final double SCAN_RANGE_VERTICAL = 4.0D;

    private final DemandEvaluator demandEvaluator;

    @Nullable
    private Resolution resolution;

    public DemandedGroundItemCondition(@Nonnull DemandEvaluator demandEvaluator) {
        this.demandEvaluator = demandEvaluator;
    }

    @Override
    public boolean test(@Nullable BaseVillager villager) {
        // Always null the previous result so callers never see stale resolutions
        // even when the precondition check is throttled (matches ChestWithDemandedItemCondition's contract).
        this.resolution = null;
        if (villager == null) {
            return false;
        }

        // Demands are cheap to resolve; short-circuit before touching world state.
        List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
        if (demands.isEmpty()) {
            return false;
        }

        AABB box = villager.getBoundingBox().inflate(SCAN_RANGE_HORIZONTAL, SCAN_RANGE_VERTICAL, SCAN_RANGE_HORIZONTAL);
        List<ItemEntity> items = villager.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive);
        if (items.isEmpty()) {
            return false;
        }

        // Copy to a mutable list and sort nearest-first so the inner loop prefers closer drops
        // when multiple items satisfy the same demand.
        List<ItemEntity> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparingDouble(villager::distanceToSqr));

        // Demands-outer / items-inner so that the highest-priority demand always wins over a
        // closer item that only satisfies a lower-priority demand. This mirrors the chest
        // condition's rationale: a distant egg the villager urgently wants beats a nearby
        // wheat the villager does not care about as much.
        for (ActiveDemand demand : demands) {
            for (ItemEntity item : sortedItems) {
                if (ItemMatches.test(demand.match(), item.getItem())) {
                    this.resolution = new Resolution(item, demand);
                    return true;
                }
            }
        }

        return false;
    }

    public Optional<Resolution> getResolution() {
        return Optional.ofNullable(this.resolution);
    }

    public record Resolution(@Nonnull ItemEntity item, @Nonnull ActiveDemand demand) {
    }

}
