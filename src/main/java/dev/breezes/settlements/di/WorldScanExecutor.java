package dev.breezes.settlements.di;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dagger qualifier for the world-section scan executor
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface WorldScanExecutor {
}
