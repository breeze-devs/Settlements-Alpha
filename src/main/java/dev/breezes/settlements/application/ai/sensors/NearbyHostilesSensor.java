package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.perception.SensedEntity;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Senses the hostiles within a villager's awareness radius, nearest first, which of the nearest few
 * are currently in sight and which are not, and the nearest one as vanilla's nearest-hostile memory.
 */
public final class NearbyHostilesSensor extends AbstractSensor<BaseVillager> {

    private static final ClockTicks SCAN_INTERVAL = ClockTicks.of(5);

    private static final double AWARENESS_RADIUS_BLOCKS = 16.0;
    private static final double AWARENESS_RADIUS_SQUARED = AWARENESS_RADIUS_BLOCKS * AWARENESS_RADIUS_BLOCKS;

    /**
     * At most this many of the nearest hostiles get a line-of-sight check per scan.
     */
    private static final int SIGHT_CHECK_BUDGET = 3;

    private static final List<IMemoryWrite> NO_HOSTILES = List.of(
            MemoryWrite.of(MemoryTypeRegistry.NEARBY_HOSTILES, PerceivedEntities.empty()),
            MemoryWrite.of(MemoryTypeRegistry.SIGHTED_HOSTILES, PerceivedEntities.empty()),
            MemoryWrite.of(MemoryTypeRegistry.UNSEEN_HOSTILES, PerceivedEntities.empty()),
            MemoryWrite.clear(MemoryTypeRegistry.NEAREST_HOSTILE));

    public NearbyHostilesSensor() {
        super(List.of(), Tickable.staggered(SCAN_INTERVAL));
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        // TODO: replace this source with a per-level hostile index fed by entity join/leave events;
        //   the vanilla living-entity scan caps awareness at its own 16-block reach.
        List<LivingEntity> scan = entity.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());

        List<SensedEntity> hostiles = new ArrayList<>();
        for (LivingEntity candidate : scan) {
            if (!candidate.isAlive() || !candidate.getType().is(SettlementsEntityTypeTags.VILLAGER_ENEMIES)) {
                continue;
            }
            double distanceSquared = entity.distanceToSqr(candidate);
            if (distanceSquared > AWARENESS_RADIUS_SQUARED) {
                continue;
            }
            hostiles.add(new SensedEntity(candidate, distanceSquared));
        }

        if (hostiles.isEmpty()) {
            return NO_HOSTILES;
        }

        hostiles.sort(Comparator.comparingDouble(SensedEntity::distanceSquared));

        // TODO: vanilla's visibility test stops at 16 blocks; awareness beyond that needs a budgeted direct line-of-sight probe.
        NearestVisibleLivingEntities visible = entity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty());
        int checkCount = Math.min(SIGHT_CHECK_BUDGET, hostiles.size());
        List<SensedEntity> sighted = new ArrayList<>(checkCount);
        List<SensedEntity> unseen = new ArrayList<>(checkCount);
        for (SensedEntity candidate : hostiles.subList(0, checkCount)) {
            if (visible.contains(candidate.entity())) {
                sighted.add(candidate);
            } else {
                unseen.add(candidate);
            }
        }

        // Vanilla's panic flee reads the nearest-hostile memory; a villager can panic anywhere in awareness, so the
        // flee target must come from the same awareness
        return List.of(
                MemoryWrite.of(MemoryTypeRegistry.NEARBY_HOSTILES, new PerceivedEntities(hostiles)),
                MemoryWrite.of(MemoryTypeRegistry.SIGHTED_HOSTILES, new PerceivedEntities(sighted)),
                MemoryWrite.of(MemoryTypeRegistry.UNSEEN_HOSTILES, new PerceivedEntities(unseen)),
                MemoryWrite.of(MemoryTypeRegistry.NEAREST_HOSTILE, hostiles.getFirst().entity()));
    }

}
