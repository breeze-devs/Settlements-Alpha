package dev.breezes.settlements.application.ai.brain;

import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.entry.IMemoryEntry;
import dev.breezes.settlements.domain.ai.schedule.IScheduleProvider;
import dev.breezes.settlements.domain.time.Ticks;
import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.Optional;

public class DefaultBrain implements IBrain {

    // @Inject
    protected final IScheduleProvider scheduleProvider;

    @Builder
    private DefaultBrain() {
        this.scheduleProvider = null; // TODO: placeholder
    }

    @Override
    public void tick(int delta) {

    }

    @Override
    public <T> Optional<IMemoryEntry<T>> getMemory(@Nonnull MemoryType<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull IMemoryEntry<T> memory) {

    }

    @Override
    public <T> void setMemory(@Nonnull MemoryType<T> type, @Nonnull IMemoryEntry<T> memory, @Nonnull Ticks expiration) {

    }

    @Override
    public void clearMemory(@Nonnull MemoryType<?> type) {

    }

    @Override
    public void checkAndExpireMemories() {

    }

}
