package dev.breezes.settlements.domain.ai.memory;

public interface MemorySerializer<F, T> {

    F serialize(T memory);

    T deserialize(F nbt);

}
