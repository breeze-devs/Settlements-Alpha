package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.shared.util.ReputationUtil;

/**
 * Declares credibility-related server-scope singletons for Dagger auto-provisioning.
 * <p>
 * {@link ReputationUtil} carries {@code @ServerScope} and an {@code @Inject} constructor,
 * so no explicit {@code @Provides} methods are needed — Dagger discovers the concrete binding
 * automatically.
 */
@Module
public abstract class CredibilityModule {

    /**
     * Binds the concrete {@link ReputationUtil} as the {@link ReputationQuery} implementation
     * so all DIP-compliant callers (planners, admission paths) can inject the interface.
     */
    @Binds
    abstract ReputationQuery reputationQuery(ReputationUtil reputationUtil);

}
