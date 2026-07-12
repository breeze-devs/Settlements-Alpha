package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import dev.breezes.settlements.infrastructure.config.annotations.BehaviorConfig;
import dev.breezes.settlements.infrastructure.config.annotations.ConfigurationType;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;

@BehaviorConfig(name = "drop_inventory_on_death", type = ConfigurationType.FEATURE)
public record DropInventoryOnDeathConfig(

        @BooleanConfig(
                type = ConfigurationType.FEATURE,
                identifier = "enabled",
                description = "When a villager dies, drop inventory onto the ground.",
                defaultValue = true)
        boolean enabled

) {

}
