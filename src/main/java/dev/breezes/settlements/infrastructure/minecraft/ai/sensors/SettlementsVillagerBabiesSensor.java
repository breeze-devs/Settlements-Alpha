package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

public class SettlementsVillagerBabiesSensor extends Sensor<LivingEntity> {

    @Override
    protected void doTick(@Nonnull ServerLevel level, @Nonnull LivingEntity entity) {
        entity.getBrain().setMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES, getNearestVillagerBabies(entity));
    }

    private List<LivingEntity> getNearestVillagerBabies(@Nonnull LivingEntity entity) {
        return StreamSupport.stream(getVisibleEntities(entity).findAll(this::isVillagerBaby).spliterator(), false)
                .toList();
    }

    private boolean isVillagerBaby(@Nonnull LivingEntity entity) {
        return entity instanceof Villager villager && villager.isBaby();
    }

    private NearestVisibleLivingEntities getVisibleEntities(@Nonnull LivingEntity entity) {
        return entity.getBrain()
                .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty());
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
    }

}
