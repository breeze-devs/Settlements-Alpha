package dev.breezes.settlements.configurations.annotations;

import dev.breezes.settlements.configurations.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.configurations.annotations.strings.StringConfig;

public class GeneralConfig {

    @BooleanConfig(type = ConfigurationType.GENERAL,
            identifier = "enable_client",
            description = "Governs whether client functionalities are enabled",
            defaultValue = false)
    public static boolean clientEnabled;

    @StringConfig(type = ConfigurationType.GENERAL,
            identifier = "global_lock_key",
            description = "The key that can be used to unlock all villager-related containers",
            defaultValue = "settlements-lock-key")
    public static String globalLockKey;


}
