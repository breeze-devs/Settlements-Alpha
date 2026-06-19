package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;

import java.util.Map;

/**
 * Runtime knobs for {@link VillageAnimalSpawnerServerEvents}: how often it runs, how many Settlements
 * wolves a village may hold, and which farm animals — with their per-species caps — may appear.
 */
@BehaviorConfig(name = "village_animal_spawner", type = ConfigurationType.FEATURE)
public record VillageAnimalSpawnerConfig(
        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "spawn_interval",
                description = "Seconds between spawn attempts near a village",
                defaultValue = 20,
                min = 1,
                max = 1200)
        int spawnIntervalSeconds,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "wolf_village_cap",
                description = "Maximum Settlements wolves within 48 blocks of a village before wolf spawning stops",
                defaultValue = 3,
                min = 0,
                max = 64)
        int wolfVillageCap,

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "minimum_occupied_homes",
                description = "Minimum occupied home POIs nearby for an area to count as a spawnable village",
                defaultValue = 4,
                min = 0,
                max = 256)
        int minimumOccupiedHomes,

        @DoubleConfig(
                type = ConfigurationType.FEATURE,
                identifier = "farm_animal_spawn_chance",
                description = "Chance per spawn cycle to also spawn one configured farm animal near the village",
                defaultValue = 0.12,
                min = 0.0,
                max = 1.0)
        double farmAnimalSpawnChance,

        @MapConfig(
                type = ConfigurationType.FEATURE,
                identifier = "farm_animal_caps",
                description = "Farm animals eligible to spawn in villages, mapped to their per-species cap within 48 blocks",
                deserializer = "StringToInteger",
                defaultValue = {
                        @MapEntry(key = "minecraft:sheep", value = "3"),
                        @MapEntry(key = "minecraft:cow", value = "2"),
                        @MapEntry(key = "minecraft:chicken", value = "2"),
                        @MapEntry(key = "minecraft:pig", value = "2"),
                        @MapEntry(key = "minecraft:rabbit", value = "4")
                })
        Map<String, Integer> farmAnimalCaps
) {

}
