package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Obligation to drop a leash that a villager attached to an animal during a behavior.
 * <p>
 * {@code animalPos} is the animal's position at leash-time, used by Dim 3
 * reconciliation to distinguish "animal gone" from "chunk not loaded yet".
 * <p>
 * The {@code holderId} guard in {@link #stillValid} prevents a crash-recovered
 * obligation from dropping a leash that a player re-attached to a fence post
 * between the crash and the villager's next reload.
 */
public record DropLeashObligation(@Nonnull UUID animalId,
                                  @Nonnull UUID holderId,
                                  @Nonnull BlockPos animalPos) implements TeardownObligation {

    @Override
    public BlockPos targetPos() {
        return this.animalPos;
    }

    @Override
    public boolean stillValid(@Nonnull ServerLevel level) {
        if (!level.isLoaded(this.animalPos)) {
            return false;
        }

        Entity entity = level.getEntity(this.animalId);
        if (!(entity instanceof Animal animal)) {
            return false;
        }

        Entity leashHolder = animal.getLeashHolder();
        return leashHolder != null && this.holderId.equals(leashHolder.getUUID());
    }

    @Override
    public void discharge(@Nonnull ServerLevel level) {
        Entity entity = level.getEntity(this.animalId);
        if (!(entity instanceof Animal animal)) {
            return;
        }

        animal.dropLeash(true, false);
    }

    @Override
    public boolean durable() {
        return true;
    }

    @Override
    public String describe() {
        return "drop leash on animal " + this.animalId + " held by " + this.holderId + " at " + this.animalPos.toShortString();
    }

}
