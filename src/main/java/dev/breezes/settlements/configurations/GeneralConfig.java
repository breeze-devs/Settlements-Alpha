package dev.breezes.settlements.configurations;

import dev.breezes.settlements.annotations.configurations.booleans.BooleanConfig;
import dev.breezes.settlements.annotations.configurations.strings.StringConfig;

public class GeneralConfig {

    @BooleanConfig(identifier = "enable_client",
            description = "Governs whether client functionalities are enabled",
            defaultValue = false)
    public static boolean clientEnabled;

    @StringConfig(identifier = "global_lock_key",
            description = "The key that can be used to unlock all villager-related containers",
            defaultValue = "settlements-lock-key")
    public static String globalLockKey;


}
