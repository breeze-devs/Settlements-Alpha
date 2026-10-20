package dev.breezes.settlements.di;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dagger qualifier for the executor that computes founded settlements' characters
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface SettlementFoundingExecutor {
}
