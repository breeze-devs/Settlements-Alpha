package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.DemandedGroundItemCondition;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Throttled per-villager scan that raises a cheap trigger flag when a demanded item is lying on
 * the ground nearby, so {@code CollectDemandedItemOverridePolicy} can react during an idle gap.
 * <p>
 * Reuses {@link DemandedGroundItemCondition} — the same reachability/demand-resolution logic the
 * behavior uses for its own fresh re-resolve — so the sensor's "nearby" and the behavior's "still
 * there" agree by construction.
 */
public final class DemandedGroundItemSensor extends AbstractSensor<BaseVillager> {

    /**
     * Matches CollectDemandedItemBehavior's NAVIGATION_COMPLETION_DISTANCE: 1 block is "close
     * enough to walk up and grab it" for the purposes of the sensor's reachability check.
     */
    private static final int COMPLETION_RANGE = 1;

    private final DemandedGroundItemCondition itemCondition;

    public DemandedGroundItemSensor(@Nonnull DemandedGroundItemSensorConfig config,
                                    @Nonnull DemandEvaluator demandEvaluator,
                                    @Nonnull BaseVillager villager) {
        super(List.of(), createStaggeredCooldown(config, villager));
        this.itemCondition = new DemandedGroundItemCondition(demandEvaluator, COMPLETION_RANGE);
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        boolean resolved = this.itemCondition.test(entity);
        return List.of(resolved
                ? MemoryWrite.of(MemoryTypeRegistry.DEMANDED_GROUND_ITEM_NEARBY, true)
                : MemoryWrite.clear(MemoryTypeRegistry.DEMANDED_GROUND_ITEM_NEARBY));
    }

    private static Tickable createStaggeredCooldown(@Nonnull DemandedGroundItemSensorConfig config, @Nonnull BaseVillager villager) {
        ClockTicks scanInterval = ClockTicks.seconds(config.scanIntervalSeconds());
        int intervalTicks = Math.max(1, scanInterval.getTicksAsInt());
        int initialDelay = Math.floorMod(villager.getUUID().hashCode(), intervalTicks);

        // Every adult villager runs this sensor, so deterministic staggering prevents large
        // villages from concentrating every item-demand scan onto the same server tick.
        return new Tickable(intervalTicks, initialDelay);
    }

}
