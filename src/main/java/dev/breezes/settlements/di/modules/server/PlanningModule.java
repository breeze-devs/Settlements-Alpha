package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.catalog.BehaviorCatalogImpl;
import dev.breezes.settlements.application.ai.planning.DefaultMealAnchorTable;
import dev.breezes.settlements.application.ai.planning.HeuristicAsyncPlanGenerator;
import dev.breezes.settlements.application.ai.planning.HeuristicPlanGenerator;
import dev.breezes.settlements.application.ai.planning.MealAnchorRule;
import dev.breezes.settlements.application.ai.planning.WakeTickResolver;
import dev.breezes.settlements.application.ai.planning.WeekCycleProvider;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.planning.IAsyncPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;

import java.util.List;

@Module
public abstract class PlanningModule {

    @Binds
    abstract IBehaviorCatalog behaviorCatalog(BehaviorCatalogImpl implementation);

    /**
     * The mod's default meal anchor table (breakfast/lunch/dinner) — the only table today, but a
     * distinct provider seam so a future data-driven or config-sourced table swaps in without
     * touching {@code DayPlanComposer}'s constructor.
     */
    @Provides
    static List<MealAnchorRule> mealAnchorTable() {
        return DefaultMealAnchorTable.rows();
    }

    @Binds
    abstract IPlanGenerator planGenerator(HeuristicPlanGenerator implementation);

    @Binds
    abstract IAsyncPlanGenerator asyncPlanGenerator(HeuristicAsyncPlanGenerator implementation);

    @Binds
    abstract IWeekCycleProvider weekCycleProvider(WeekCycleProvider implementation);

    @Binds
    abstract IWakeTickResolver wakeTickResolver(WakeTickResolver implementation);

}
