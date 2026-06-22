package dev.breezes.settlements.infrastructure.minecraft.blocks;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;

/**
 * Weights (which ore regenerates and how often relative to others) live in the datapack table
 */
@BehaviorConfig(name = "ore_regeneration", type = ConfigurationType.FEATURE)
public record OreRegenConfig(

        @BooleanConfig(
                type = ConfigurationType.FEATURE,
                identifier = "enabled",
                description = "Master switch: should the ores mined by villagers regenerate into random ores?",
                defaultValue = true)
        boolean enabled,

        @DoubleConfig(
                type = ConfigurationType.FEATURE,
                identifier = "regen_chance_per_random_tick",
                description = "Probability that a dormant ore recharges on each random tick. At randomTickSpeed=3 (vanilla default) " +
                        "and regenChancePerRandomTick=0.03, each recharge takes 37 mins on average",
                defaultValue = 0.03,
                min = 0.0,
                max = 1.0)
        double regenChancePerRandomTick

) {

}
