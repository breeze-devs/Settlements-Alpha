package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.perception.SensedEntity;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;

public final class EntityPerceptionSensor extends AbstractSensor<BaseVillager> {

    private final EntityPerceptionSensorConfig config;

    public EntityPerceptionSensor(@Nonnull EntityPerceptionSensorConfig config, @Nonnull BaseVillager villager) {
        super(List.of(), createStaggeredCooldown(config, villager));
        this.config = config;
    }

    @Override
    public List<MemoryWrite<?>> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        AABB scanBox = entity.getBoundingBox().inflate(
                this.config.scanRangeHorizontal(),
                this.config.scanRangeVertical(),
                this.config.scanRangeHorizontal());

        List<SensedEntity> sensedEntities = world.getEntitiesOfClass(LivingEntity.class, scanBox, sensed -> this.canSense(entity, sensed))
                .stream()
                .map(sensed -> new SensedEntity(sensed, entity.distanceToSqr(sensed)))
                .sorted(Comparator.comparingDouble(SensedEntity::distanceSquared))
                .toList();

        return List.of(MemoryWrite.of(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES, new PerceivedEntities(sensedEntities)));
    }

    private boolean canSense(@Nonnull BaseVillager self, @Nonnull LivingEntity sensed) {
        return sensed != self
                && sensed.isAlive()
                && !sensed.isRemoved();
    }

    private static Tickable createStaggeredCooldown(@Nonnull EntityPerceptionSensorConfig config, @Nonnull BaseVillager villager) {
        ClockTicks scanInterval = ClockTicks.seconds(config.scanIntervalSeconds());
        int intervalTicks = Math.max(1, scanInterval.getTicksAsInt());
        int initialDelay = Math.floorMod(villager.getUUID().hashCode(), intervalTicks);

        // Shared perception is intentionally universal, so deterministic staggering prevents large villages
        // from concentrating every entity-section scan onto the same server tick.
        return new Tickable(intervalTicks, initialDelay);
    }

}
