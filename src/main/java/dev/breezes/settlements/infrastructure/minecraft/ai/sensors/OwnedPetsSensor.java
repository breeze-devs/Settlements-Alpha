package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;

import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OwnedPetsSensor extends Sensor<Villager> {

    public OwnedPetsSensor() {
        super(ClockTicks.seconds(60).getTicksAsInt());
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(MemoryTypeRegistry.OWNED_WOLVES.getModuleType());
    }

    @Override
    protected void doTick(@Nonnull ServerLevel level, @Nonnull Villager villager) {
        if (!(villager instanceof BaseVillager baseVillager)) {
            return;
        }

        List<UUID> ownedWolfIds = baseVillager.getBrain()
                .getMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType())
                .orElse(List.of());
        if (ownedWolfIds.isEmpty()) {
            return;
        }

        List<UUID> validWolfIds = new ArrayList<>();
        for (UUID wolfId : ownedWolfIds) {
            Entity entity = level.getEntity(wolfId);
            if (!(entity instanceof SettlementsWolf wolf) || !wolf.isAlive() || wolf.isRemoved()) {
                continue;
            }
            if (!baseVillager.getUUID().equals(wolf.getOwnerUUID())) {
                continue;
            }
            validWolfIds.add(wolfId);
        }

        if (validWolfIds.isEmpty()) {
            baseVillager.getBrain().eraseMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType());
            return;
        }

        baseVillager.getBrain().setMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType(), List.copyOf(validWolfIds));
    }

}
