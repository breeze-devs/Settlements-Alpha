package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;

/**
 * Runtime policy for replacing vanilla villagers produced by village world generation.
 */
@BehaviorConfig(name = "worldgen_villager_replacement", type = ConfigurationType.FEATURE)
public record WorldgenVillagerReplacementConfig(
        @DoubleConfig(
                type = ConfigurationType.FEATURE,
                identifier = "replacement_chance",
                description = "Chance for each naturally spawned villager to be replaced by a Settlements villager. Set to 0 to disable",
                defaultValue = 0.75,
                min = 0.0,
                max = 1.0)
        double replacementChance
) {

}
