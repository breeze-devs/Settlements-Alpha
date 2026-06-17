package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.SocialAcceptOverridePolicy;
import dev.breezes.settlements.application.ai.override.UrgentInvestigateOverridePolicy;

import java.util.Set;

/**
 * Registers all {@link OverridePolicy} implementations as a Dagger multibinding set.
 * {@link dev.breezes.settlements.application.ai.planning.PlanRunner} evaluates the set
 * every tick and uses {@link OverridePolicy#priority()} for cross-policy precedence.
 */
@Module
public abstract class OverridePolicyModule {

    @Multibinds
    abstract Set<OverridePolicy> overridePolicies();

    @Binds
    @IntoSet
    abstract OverridePolicy socialAcceptPolicy(SocialAcceptOverridePolicy impl);

    @Binds
    @IntoSet
    abstract OverridePolicy urgentInvestigatePolicy(UrgentInvestigateOverridePolicy impl);

}
