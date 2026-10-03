package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.override.CollectDemandedItemOverridePolicy;
import dev.breezes.settlements.application.ai.override.CombatOverridePolicy;
import dev.breezes.settlements.application.ai.override.CourtshipAcceptOverridePolicy;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.TradeAcceptOverridePolicy;

import java.util.Set;

/**
 * Registers all {@link OverridePolicy} implementations as a Dagger multibinding set.
 * {@link dev.breezes.settlements.application.ai.override.OverrideArbiter} evaluates the set on its
 * throttled cadence and uses {@link OverridePolicy#precedence()} for tier and in-tier ordering.
 */
@Module
public abstract class OverridePolicyModule {

    @Multibinds
    abstract Set<OverridePolicy> overridePolicies();

    @Binds
    @IntoSet
    abstract OverridePolicy combatPolicy(CombatOverridePolicy impl);

    @Binds
    @IntoSet
    abstract OverridePolicy courtshipAcceptPolicy(CourtshipAcceptOverridePolicy impl);

    @Binds
    @IntoSet
    abstract OverridePolicy tradeAcceptPolicy(TradeAcceptOverridePolicy impl);

    @Binds
    @IntoSet
    abstract OverridePolicy collectDemandedItemPolicy(CollectDemandedItemOverridePolicy impl);

}
