package dev.breezes.settlements.di;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dagger qualifier for the post-processing set of {@code PreparableReloadListener}s: validators and
 * other consumers that read the datapack state populated by the {@link DataReloadListeners} set. Callers
 * must drain the producer set before this one so every read observes a fully populated snapshot.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface PostReloadListeners {
}
