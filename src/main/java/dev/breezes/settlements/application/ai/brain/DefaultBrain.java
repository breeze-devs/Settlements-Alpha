package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.SensedSiteReport;
import dev.breezes.settlements.domain.ai.schedule.IScheduleProvider;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class DefaultBrain implements IBrain {

    protected final IScheduleProvider scheduleProvider;

    @Builder
    private DefaultBrain() {
        this.scheduleProvider = null; // TODO: placeholder
    }

    @Override
    public void initialize() {
        // No server-scoped state to wire up
    }

    @Override
    public void tick(int delta) {

    }

    @Override
    public <T> Optional<T> getMemory(@Nonnull MemoryType<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value) {

    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull T value, @Nonnull ClockTicks expiration) {

    }

    @Override
    public void clearMemory(@Nonnull MemoryType<?> type) {

    }

    @Override
    public void updateSites(@Nonnull MemoryType.DecayingSpatialMemoryType type,
                            @Nonnull SensedSiteReport report,
                            long nowTick) {
        // DefaultBrain is a placeholder; site updates are no-ops here.
    }

}
