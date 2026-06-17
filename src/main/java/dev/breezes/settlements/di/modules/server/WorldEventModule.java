package dev.breezes.settlements.di.modules.server;

import dagger.Module;

/**
 * Declares the {@link dev.breezes.settlements.domain.ai.worldevent.WorldEventBus},
 * {@link dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter},
 * and {@link dev.breezes.settlements.bootstrap.event.WorldEventBusReaperServerEvents}
 * for Dagger auto-provisioning.
 * <p>
 * All three classes carry {@code @ServerScope} and {@code @Inject} constructors, so no
 * explicit {@code @Provides} methods are required — Dagger discovers them automatically
 * once this module is included in the server subcomponent.
 */
@Module
public abstract class WorldEventModule {

    // All bindings are auto-provisioned via @ServerScope + @Inject on the impl classes.

}
