package dev.breezes.settlements.logging;

import dev.breezes.settlements.logging.levels.ApacheLogLevelAdapter;
import dev.breezes.settlements.logging.levels.LogLevel;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

@Getter
public class ApacheLogger implements ILogger {

    private final Logger logger;

    public ApacheLogger(@Nonnull Class<?> boundClass) {
        this.logger = LogManager.getLogger(boundClass);
    }

    public ApacheLogger(@Nonnull String name) {
        this.logger = LogManager.getLogger(name);
    }

    @Override
    public void log(@Nonnull LogLevel level, @Nonnull String format, @Nonnull Object... args) {
        this.logger.log(ApacheLogLevelAdapter.adapt(level), format.formatted(args));
    }

    @Override
    public void log(@Nonnull String format, @Nonnull Object... args) {
        this.logger.info(format.formatted(args));
    }

    @Override
    public void behaviorStatus(@Nonnull String format, @Nonnull Object... args) {
        this.info(format.formatted(args));
    }

    @Override
    public void behaviorTrace(@Nonnull String format, @Nonnull Object... args) {
        this.trace(format, args);
    }

    @Override
    public void trace(@Nonnull String format, @Nonnull Object... args) {
        // TODO: re-enable trace logging
        this.logger.trace(format.formatted(args));
    }

    @Override
    public void debug(@Nonnull String format, @Nonnull Object... args) {
        this.logger.debug(format.formatted(args));
    }

    @Override
    public void info(@Nonnull String format, @Nonnull Object... args) {
        this.logger.info(format.formatted(args));
    }

    @Override
    public void warn(@Nonnull String format, @Nonnull Object... args) {
        this.logger.warn(format.formatted(args));
    }

    @Override
    public void error(@Nonnull String format, @Nonnull Object... args) {
        this.logger.error(format.formatted(args));
    }

    @Override
    public void error(@Nonnull Throwable e, @Nonnull String format, @Nonnull Object... args) {
        this.logger.error(format.formatted(args), e);
    }

}
