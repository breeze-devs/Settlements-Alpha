package dev.breezes.settlements.infrastructure.config.annotations;

import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;

public class GeneralConfig {

    @BooleanConfig(type = ConfigurationType.GENERAL,
            identifier = "enable_client",
            description = "Governs whether client functionalities are enabled",
            defaultValue = true)
    public static boolean clientEnabled;

    @StringConfig(type = ConfigurationType.GENERAL,
            identifier = "global_lock_key",
            description = "The key that can be used to unlock all villager-related containers",
            defaultValue = "settlements-lock-key")
    public static String globalLockKey;

    @BooleanConfig(type = ConfigurationType.GENERAL,
            identifier = "disable_natural_experience_gain",
            description = "Disables villager experience gained naturally from successful behaviors",
            defaultValue = false)
    public static boolean disableNaturalExperienceGain;

    @BooleanConfig(type = ConfigurationType.GENERAL,
            identifier = "bypass_inventory_requirements",
            description = "When true, behaviors run without requiring or emitting demand signals for their input items. " +
                    "If the villager happens to have the item, it is still consumed.",
            defaultValue = true)
    public static boolean bypassInventoryRequirements;

}
