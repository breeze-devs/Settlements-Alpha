package dev.breezes.settlements.models.memory;

import net.minecraft.nbt.Tag;

public interface IMemorySerializer<T> {

    // TODO: determine if we should use NBT tags or some other serialization method
    Tag serialize(T memory);

    T deserialize(Tag nbt);

}
