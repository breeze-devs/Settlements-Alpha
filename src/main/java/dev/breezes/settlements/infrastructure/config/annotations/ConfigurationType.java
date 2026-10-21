package dev.breezes.settlements.infrastructure.config.annotations;

import dev.breezes.settlements.SettlementsMod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.neoforged.fml.config.ModConfig;

import java.util.Locale;

@AllArgsConstructor
@Getter
public enum ConfigurationType {

    GENERAL("general", ModConfig.Type.COMMON),
    BEHAVIOR("behaviors", ModConfig.Type.COMMON),
    SENSOR("sensors", ModConfig.Type.COMMON),
    FEATURE("features", ModConfig.Type.COMMON),
    INFERENCE("inference", ModConfig.Type.COMMON),
    DEBUGGING("debugging", ModConfig.Type.STARTUP),
    ;

    private final String filePath;

    /**
     * The type this file registers as with NeoForge, which decides when the file loads and its values can be read.
     * A STARTUP file loads the moment it is registered, so its configuration is a record registered during mod
     * construction.
     */
    private final ModConfig.Type modConfigType;

    /**
     * Each configuration type resolves to a single TOML file, so every config of a given type
     * shares one {@link net.neoforged.neoforge.common.ModConfigSpec}.
     */
    public String getFilePath() {
        return "%s/%s.toml".formatted(SettlementsMod.MOD_NAME.toLowerCase(Locale.ROOT), this.filePath);
    }

}
