package dev.breezes.settlements.infrastructure.config.annotations;

import dev.breezes.settlements.SettlementsMod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@AllArgsConstructor
@Getter
public enum ConfigurationType {

    GENERAL("general"),
    BEHAVIOR("behaviors"),
    SENSOR("sensors"),
    FEATURE("features"),
    INFERENCE("inference"),
    ;

    private final String filePath;

    /**
     * Each configuration type resolves to a single TOML file, so every config of a given type
     * shares one {@link net.neoforged.neoforge.common.ModConfigSpec}.
     */
    public String getFilePath() {
        return "%s/%s.toml".formatted(SettlementsMod.MOD_NAME.toLowerCase(Locale.ROOT), this.filePath);
    }

}
