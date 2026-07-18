package dev.breezes.settlements.di;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dagger qualifier for the producer set of {@code PreparableReloadListener}s: datapack-backed
 * catalogs/managers that populate their own snapshot on reload. These are mutually order-independent
 * and must all finish before any {@link PostReloadListeners} listener runs.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface DataReloadListeners {
}
