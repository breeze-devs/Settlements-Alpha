package dev.breezes.settlements.configurations.annotations;


import lombok.Getter;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigurationAnnotationRegistry {

    /**
     * Registry for configuration builders, keyed by file name.
     * This allows for multiple config files to be registered and built separately.
     */
    private final Map<String, ModConfigSpec.Builder> fileBuilderMap;

    public ConfigurationAnnotationRegistry() {
        this.fileBuilderMap = new HashMap<>();
    }

    public ModConfigSpec.Builder getBuilder(@Nonnull String fileName) {
        return this.fileBuilderMap.computeIfAbsent(fileName, k -> new ModConfigSpec.Builder());
    }

}
