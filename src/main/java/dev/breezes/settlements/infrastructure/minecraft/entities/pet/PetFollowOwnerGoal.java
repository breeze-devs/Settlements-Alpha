package dev.breezes.settlements.infrastructure.minecraft.entities.pet;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

import javax.annotation.Nonnull;
import java.util.function.BooleanSupplier;

/**
 * Follows the owner as vanilla does, staying idle while followOwnerLocked reports true.
 */
public class PetFollowOwnerGoal extends FollowOwnerGoal {

    private final BooleanSupplier followOwnerLocked;

    public PetFollowOwnerGoal(@Nonnull TamableAnimal pet,
                              @Nonnull BooleanSupplier followOwnerLocked,
                              double speedModifier,
                              float startDistance,
                              float stopDistance) {
        super(pet, speedModifier, startDistance, stopDistance);
        this.followOwnerLocked = followOwnerLocked;
    }

    @Override
    public boolean canUse() {
        return !this.followOwnerLocked.getAsBoolean() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.followOwnerLocked.getAsBoolean() && super.canContinueToUse();
    }

}
