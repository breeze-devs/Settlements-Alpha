package dev.breezes.settlements.application.ai.behavior.usecases.villager.ballista;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;

@BehaviorConfig(name = "ballista_operation", type = ConfigurationType.FEATURE)
public record BallistaOperationConfig(

        @DoubleConfig(
                type = ConfigurationType.FEATURE,
                identifier = "operator_intelligence_threshold",
                description = "Minimum intelligence (INT) gene value required for villagers other than fletchers to " +
                        "operate a ballista. Fletchers ignore this requirement. Active raids lower the threshold. " +
                        "INT values range from 0 to 1. Set above 1 to restrict operation to fletchers, even during raids",
                defaultValue = 0.65,
                min = 0.0,
                max = 2.0)
        double operatorIntelligenceThreshold,

        @DoubleConfig(
                type = ConfigurationType.FEATURE,
                identifier = "max_scatter_degrees",
                description = "Maximum random aim offset in degrees for a novice villager with the lowest " +
                        "intelligence (INT). Higher INT and villager levels reduce scatter. Set to 0 to disable " +
                        "random scatter. Target prediction still depends on operator skill",
                defaultValue = 6.0,
                min = 0.0,
                max = 30.0)
        double maxScatterDegrees

) {

}
