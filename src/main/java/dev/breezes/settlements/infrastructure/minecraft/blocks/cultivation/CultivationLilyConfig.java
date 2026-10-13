package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;

@BehaviorConfig(name = "cultivation_lily", type = ConfigurationType.FEATURE)
public record CultivationLilyConfig(

        @IntegerConfig(
                type = ConfigurationType.FEATURE,
                identifier = "crops_grown_per_random_tick",
                description = "How many crops in an active Cultivation Lily's zone receive a growth boost randomly. " +
                        "At randomTickSpeed=3 (vanilla default) this boost activates every 68 seconds on average. " +
                        "0 turns the boost off",
                defaultValue = 3,
                min = 0,
                max = 128)
        int cropsGrownPerRandomTick

) {

}
