package dev.breezes.settlements.di.modules.server;

import dagger.Module;

/**
 * Declares the {@link dev.breezes.settlements.application.ai.perception.PerceptionPipeline}
 * for Dagger auto-provisioning within the server subcomponent.
 * <p>
 * The pipeline carries {@code @ServerScope} and an {@code @Inject} constructor, so no
 * explicit {@code @Provides} methods are required — Dagger discovers it automatically
 * once this module is included in the server subcomponent.
 */
@Module
public abstract class PerceptionModule {

    // All bindings are auto-provisioned via @ServerScope + @Inject on the impl classes.

}
