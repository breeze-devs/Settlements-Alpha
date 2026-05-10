package dev.breezes.settlements.infrastructure.minecraft.entities.wolves.goals;

import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class WolfFollowOwnerGoal extends FollowOwnerGoal {

    private final SettlementsWolf wolf;

    public WolfFollowOwnerGoal(SettlementsWolf wolf, double speed, float minDistance, float maxDistance) {
        super(wolf, speed, minDistance, maxDistance);
        this.wolf = wolf;
    }

    @Override
    public boolean canUse() {
        if (this.wolf.isFollowOwnerLocked()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.wolf.isFollowOwnerLocked()) {
            return false;
        }
        return super.canContinueToUse();
    }
}
