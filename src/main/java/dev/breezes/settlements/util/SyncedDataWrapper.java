package dev.breezes.settlements.util;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;

/**
 * Synchronizes entity data between server and client.
 * Allows registering up to 254 parameters per entity class.
 * Effective upper limit is reduced by superclass registrations.
 */
@Getter
public final class SyncedDataWrapper<T> {

    private final EntityDataAccessor<T> dataAccessor;
    private final T defaultValue;

    @Builder
    private SyncedDataWrapper(Class<? extends Entity> entityClass, EntityDataSerializer<T> serializer, T defaultValue) {
        this.dataAccessor = SynchedEntityData.defineId(entityClass, serializer);
        this.defaultValue = defaultValue;
    }

    public void define(@Nonnull SynchedEntityData.Builder entityData) {
        entityData.define(this.dataAccessor, this.defaultValue);
    }

    public T get(SynchedEntityData entityData) {
        return entityData.get(this.dataAccessor);
    }

    public void set(SynchedEntityData entityData, T value) {
        entityData.set(this.dataAccessor, value);
    }

}

