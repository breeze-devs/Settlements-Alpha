package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.override.ChartRaidersOverridePolicy;
import dev.breezes.settlements.application.ai.override.CollectDemandedItemOverridePolicy;
import dev.breezes.settlements.application.ai.override.CombatOverridePolicy;
import dev.breezes.settlements.application.ai.override.CourtshipAcceptOverridePolicy;
import dev.breezes.settlements.application.ai.override.OverridePolicy;
import dev.breezes.settlements.application.ai.override.RetrieveProjectilesOverridePolicy;
import dev.breezes.settlements.application.ai.override.TradeAcceptOverridePolicy;
import dev.breezes.settlements.application.ai.override.TradeInitiateOverridePolicy;

import java.util.List;

@Module
public abstract class OverridePolicyModule {

    /**
     * Every override policy in ranked order for evaluation, highest {@link OverridePolicy#tier()} first.
     * Within a tier, a policy ahead in the list is preferred over one behind it whenever both request an override.
     */
    @Provides
    static List<OverridePolicy> overridePolicies(CombatOverridePolicy combat,
                                                 ChartRaidersOverridePolicy chartRaiders,
                                                 CourtshipAcceptOverridePolicy courtshipAccept,
                                                 TradeAcceptOverridePolicy tradeAccept,
                                                 CollectDemandedItemOverridePolicy collectDemandedItem,
                                                 RetrieveProjectilesOverridePolicy retrieveProjectiles,
                                                 TradeInitiateOverridePolicy tradeInitiate) {
        return List.of(
                combat,
                chartRaiders,
                courtshipAccept,
                tradeAccept,
                collectDemandedItem,
                retrieveProjectiles,
                tradeInitiate
        );
    }

}
