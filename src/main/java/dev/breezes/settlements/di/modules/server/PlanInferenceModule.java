package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.inference.plan.HttpPlanGateway;
import dev.breezes.settlements.application.ai.inference.plan.PlanGateway;
import dev.breezes.settlements.di.ServerScope;

/**
 * Exposes the LLM PLAN-inference sweep's services to the server Dagger graph.
 */
@Module
public final class PlanInferenceModule {

    @Provides
    @ServerScope
    static PlanGateway planGateway(HttpPlanGateway gateway) {
        return gateway;
    }

}
