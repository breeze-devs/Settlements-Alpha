package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dev.breezes.settlements.application.ai.gossip.GossipSessionRegistry;
import dev.breezes.settlements.bootstrap.event.GossipSessionReaperServerEvents;

/**
 * Declares gossip-related server-scope singletons for Dagger auto-provisioning
 * <p>
 * {@link GossipSessionRegistry} and {@link GossipSessionReaperServerEvents} each carry
 * {@code @ServerScope} and {@code @Inject} constructors, so no explicit {@code @Provides}
 * methods are needed — Dagger discovers them automatically once this module is included
 * in the server subcomponent.
 */
@Module
public abstract class GossipModule {

    // All bindings are auto-provisioned via @ServerScope + @Inject on the impl classes.

}
