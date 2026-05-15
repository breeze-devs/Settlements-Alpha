package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dev.breezes.settlements.application.ai.catalog.BehaviorCatalogImpl;
import dev.breezes.settlements.application.ai.planning.HeuristicAsyncPlanGenerator;
import dev.breezes.settlements.application.ai.planning.HeuristicPlanGenerator;
import dev.breezes.settlements.application.ai.planning.WakeTickResolver;
import dev.breezes.settlements.application.ai.planning.WeekCycleProvider;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.planning.IAsyncPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IPlanGenerator;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import lombok.CustomLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Module
@CustomLog
public abstract class PlanningModule {

    @Binds
    abstract IBehaviorCatalog behaviorCatalog(BehaviorCatalogImpl implementation);

    @Binds
    abstract IPlanGenerator planGenerator(HeuristicPlanGenerator implementation);

    @Binds
    abstract IAsyncPlanGenerator asyncPlanGenerator(HeuristicAsyncPlanGenerator implementation);

    @Binds
    abstract IWeekCycleProvider weekCycleProvider(WeekCycleProvider implementation);

    @Binds
    abstract IWakeTickResolver wakeTickResolver(WakeTickResolver implementation);

    @Provides
    @ServerScope
    static ExecutorService planGenerationExecutor() {
        AtomicInteger threadId = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "settlements-plan-gen-" + threadId.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, throwable) ->
                    log.error("Uncaught error on plan-gen thread {}", failedThread.getName(), throwable));
            return thread;
        };
        // TODO: evaluate if we need additional threads
        return Executors.newFixedThreadPool(2, threadFactory);
    }

}
