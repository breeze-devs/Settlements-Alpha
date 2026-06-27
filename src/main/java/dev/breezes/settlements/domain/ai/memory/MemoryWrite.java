package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.ai.brain.IBrain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record MemoryWrite<T>(@Nonnull MemoryType<T> type, @Nullable T value) implements IMemoryWrite {

    public static <T> MemoryWrite<T> of(@Nonnull MemoryType<T> type, @Nonnull T value) {
        return new MemoryWrite<>(type, value);
    }

    public static <T> MemoryWrite<T> clear(@Nonnull MemoryType<T> type) {
        return new MemoryWrite<>(type, null);
    }

    @Override
    public void applyTo(@Nonnull IBrain brain) {
        if (this.value == null) {
            brain.clearMemory(this.type);
            return;
        }

        brain.setMemory(this.type, this.value);
    }

}
