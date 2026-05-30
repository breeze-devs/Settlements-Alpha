package dev.breezes.settlements.di.catalog;

import dev.breezes.settlements.domain.ai.sensors.ISensor;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

@FunctionalInterface
public interface VillagerSensorFactory {

    ISensor<BaseVillager> create(BaseVillager villager);

}
