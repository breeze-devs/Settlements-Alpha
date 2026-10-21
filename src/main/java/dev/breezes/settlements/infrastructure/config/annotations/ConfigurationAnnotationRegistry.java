package dev.breezes.settlements.infrastructure.config.annotations;


import lombok.Getter;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.Map;

@Getter
public class ConfigurationAnnotationRegistry {

    /**
     * Registry for configuration builders, keyed by the configuration type whose file each builds.
     * This allows for multiple config files to be registered and built separately.
     */
    private final Map<ConfigurationType, ModConfigSpec.Builder> fileBuilderMap;

    public ConfigurationAnnotationRegistry() {
        this.fileBuilderMap = new EnumMap<>(ConfigurationType.class);
    }

    public ModConfigSpec.Builder getBuilder(@Nonnull ConfigurationType type) {
        return this.fileBuilderMap.computeIfAbsent(type, k -> new ModConfigSpec.Builder());
    }

}
