package dev.breezes.settlements.shared.logging.levels;

import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The least severe level a logging destination admits.
 * A threshold named after a level admits that level and every more severe one.
 * <p>
 * Constant names are the values operators write in configuration files, so renaming one breaks existing files.
 */
@AllArgsConstructor
public enum LogThreshold {

    TRACE(LogLevel.TRACE),
    DEBUG(LogLevel.DEBUG),
    INFO(LogLevel.INFO),
    WARN(LogLevel.WARN),
    ERROR(LogLevel.ERROR),

    /**
     * Admits no level.
     */
    OFF(null);

    @Nullable
    private final LogLevel minimumLevel;

    /**
     * Returns the least severe level this threshold admits, or empty when it admits none.
     */
    public Optional<LogLevel> getMinimumLevel() {
        return Optional.ofNullable(this.minimumLevel);
    }

}
