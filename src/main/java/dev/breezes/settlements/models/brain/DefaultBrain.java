package dev.breezes.settlements.models.brain;

import dev.breezes.settlements.models.memory.MemoryType;
import dev.breezes.settlements.models.memory.entry.IMemoryEntry;
import dev.breezes.settlements.models.schedule.IScheduleProvider;
import dev.breezes.settlements.util.Ticks;
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
