package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.application.ai.catalog.BehaviorCatalogImpl;
import dev.breezes.settlements.application.ai.planning.HeuristicPlanGenerator;
import dev.breezes.settlements.application.ai.planning.WeekCycleProvider;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;

@Module
public abstract class PlanningModule {

    @Binds
    abstract IBehaviorCatalog behaviorCatalog(BehaviorCatalogImpl implementation);

    @Binds
    abstract IPlanGenerator planGenerator(HeuristicPlanGenerator implementation);

    @Binds
    abstract IWeekCycleProvider weekCycleProvider(WeekCycleProvider implementation);

}
