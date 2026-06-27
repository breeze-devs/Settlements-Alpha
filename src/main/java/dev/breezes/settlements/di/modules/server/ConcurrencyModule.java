package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.di.PlanGenerationExecutor;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.WorldScanExecutor;
import lombok.CustomLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central home for every server-scoped {@link ExecutorService} pool.
 * <p>
 * Each pool carries a distinct {@link javax.inject.Qualifier}; there is intentionally no
 * unqualified {@code ExecutorService} binding, so a mis-wired injection (e.g. a Lombok field
 * whose qualifier was not copied to the constructor parameter) fails to compile rather than
 * silently resolving to whichever pool happens to be unqualified.
 * <p>
 * Every pool also contributes to the {@code Set<ExecutorService>} managed-executor set, letting
 * {@code ServerLifecycleEvents} shut them all down in one pass — a future pool (e.g. for LLM
 * inference) only needs its provider plus an {@code @IntoSet} forwarder, with no lifecycle edit.
 */
@Module
@CustomLog
public final class ConcurrencyModule {

    @Provides
    @ServerScope
    @PlanGenerationExecutor
    static ExecutorService planGenerationExecutor() {
        // Plan generation is bursty and latency-tolerant
        return fixedDaemonPool("settlements-plan-gen", 2);
    }

    @Provides
    @ServerScope
    @WorldScanExecutor
    static ExecutorService worldScanExecutor() {
        // Throughput knob is the main-thread snapshot budget, not pool size
        return fixedDaemonPool("settlements-world-scan", 3);
    }

    @Provides
    @IntoSet
    static ExecutorService planGenerationManagedExecutor(@PlanGenerationExecutor ExecutorService executor) {
        return executor;
    }

    @Provides
    @IntoSet
    static ExecutorService worldScanManagedExecutor(@WorldScanExecutor ExecutorService executor) {
        return executor;
    }

    /**
     * Builds a fixed pool of named daemon threads with an uncaught-exception logger. Daemon so a
     * missed shutdown can never block JVM exit; named so thread dumps stay legible.
     */
    private static ExecutorService fixedDaemonPool(String threadNamePrefix, int threads) {
        AtomicInteger threadId = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, threadNamePrefix + "-" + threadId.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failedThread, throwable) -> log.error("Uncaught error on thread {}", failedThread.getName(), throwable));
            return thread;
        };

        return Executors.newFixedThreadPool(threads, threadFactory);
    }

}
