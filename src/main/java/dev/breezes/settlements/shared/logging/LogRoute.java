package dev.breezes.settlements.shared.logging;

import dev.breezes.settlements.shared.logging.levels.LogLevel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Immutable routing state for log calls. A route retains no game state.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class LogRoute {

    // Above every level's ordinal, so a threshold holding it admits no level.
    private static final int NOTHING_ADMITTED = Integer.MAX_VALUE;

    // Cache the least destination threshold so admission needs only one integer comparison.
    private final int minimumAdmittedOrdinal;

    private final int normalLogMinimumOrdinal;

    /**
     * Returns the route that follows the given policy.
     */
    static LogRoute of(@Nonnull LoggingPolicy policy) {
        if (!policy.enabled()) {
            return new LogRoute(NOTHING_ADMITTED, NOTHING_ADMITTED);
        }
        int normalLogMinimumOrdinal = policy.standardThreshold().getMinimumLevel()
                .map(LogLevel::ordinal)
                .orElse(NOTHING_ADMITTED);
        return new LogRoute(normalLogMinimumOrdinal, normalLogMinimumOrdinal);
    }

    /**
     * Returns whether the level meets any destination's threshold.
     */
    boolean admits(@Nonnull LogLevel level) {
        return level.ordinal() >= this.minimumAdmittedOrdinal;
    }

    /**
     * Forwards the call to each destination whose threshold accepts its level, leaving the format and arguments unchanged.
     */
    void dispatch(@Nonnull LoggerHandle source,
                  @Nonnull LogLevel level,
                  @Nonnull String format,
                  @Nonnull Object[] args) {
        if (level.ordinal() >= this.normalLogMinimumOrdinal) {
            source.getNormalLog().forward(level, format, args);
        }
    }

}
