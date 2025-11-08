package dev.breezes.settlements.models.memory.serializers;

public interface MemorySerializer<F, T> {

    F serialize(T memory);

    T deserialize(F nbt);

}
