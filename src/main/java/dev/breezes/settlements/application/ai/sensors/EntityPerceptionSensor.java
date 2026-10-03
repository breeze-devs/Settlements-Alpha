package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
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

    public EntityPerceptionSensor(@Nonnull EntityPerceptionSensorConfig config) {
        super(List.of(), Tickable.staggered(ClockTicks.seconds(config.scanIntervalSeconds())));
        this.config = config;
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
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

}
