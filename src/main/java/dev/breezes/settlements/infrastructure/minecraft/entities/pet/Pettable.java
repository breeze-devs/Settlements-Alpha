package dev.breezes.settlements.infrastructure.minecraft.entities.pet;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

/**
 * Marks a Settlements animal as eligible for the shared "petting" gesture.
 */
public interface Pettable {

    /**
     * Triggers the shared pet reaction (hearts, sound, squish) from the given actor.
     *
     * @return whether the pet landed (false while on cooldown, or when called client-side)
     */
    boolean pet(@Nonnull LivingEntity actor);

}
