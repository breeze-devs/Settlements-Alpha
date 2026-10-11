package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.override.CollectDemandedItemOverridePolicy;
import dev.breezes.settlements.application.ai.override.CombatOverridePolicy;
import dev.breezes.settlements.application.ai.override.CourtshipAcceptOverridePolicy;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.RetrieveProjectilesOverridePolicy;
import dev.breezes.settlements.application.ai.override.TradeAcceptOverridePolicy;

import java.util.List;

@Module
public abstract class OverridePolicyModule {

    /**
     * Every override policy in ranked order for evaluation, highest {@link OverridePolicy#tier()} first.
     * Within a tier, a policy ahead in the list is preferred over one behind it whenever both request an override.
     */
    @Provides
    static List<OverridePolicy> overridePolicies(CombatOverridePolicy combat,
                                                 CourtshipAcceptOverridePolicy courtshipAccept,
                                                 TradeAcceptOverridePolicy tradeAccept,
                                                 CollectDemandedItemOverridePolicy collectDemandedItem,
                                                 RetrieveProjectilesOverridePolicy retrieveProjectiles) {
        return List.of(
                combat,
                courtshipAccept,
                tradeAccept,
                collectDemandedItem,
                retrieveProjectiles
        );
    }

}
