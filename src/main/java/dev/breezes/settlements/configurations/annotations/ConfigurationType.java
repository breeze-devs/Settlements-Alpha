package dev.breezes.settlements.configurations.annotations;

import dev.breezes.settlements.SettlementsMod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Locale;

@AllArgsConstructor
@Getter
public enum ConfigurationType {

    GENERAL("general"),
    BEHAVIOR("behaviors"),
    ;

    private final String filePath;

    public String getFilePath(@Nonnull String className) {
        String filePath = switch (this) {
            case GENERAL -> this.filePath;
            case BEHAVIOR -> "%s/%s".formatted(this.filePath, className);
        };

        return "%s/%s.toml".formatted(SettlementsMod.MOD_NAME.toLowerCase(Locale.ROOT), filePath);
    }

}
