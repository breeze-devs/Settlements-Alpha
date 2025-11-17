package dev.breezes.settlements.entities.wolves.goals;

import dev.breezes.settlements.entities.wolves.SettlementsWolf;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class WolfFollowOwnerGoal extends FollowOwnerGoal {

    private final SettlementsWolf wolf;

    public WolfFollowOwnerGoal(SettlementsWolf wolf, double speed, float minDistance, float maxDistance) {
        super(wolf, speed, minDistance, maxDistance);
        this.wolf = wolf;
    }

    @Override
    public boolean canUse() {
        // Don't follow owner if fetching
        if (this.wolf.isStopFollowOwner()) {
            return false;
        }

        // Otherwise, use super's decision
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Don't follow owner if fetching
        if (this.wolf.isStopFollowOwner()) {
            return false;
        }

        // Otherwise, use super's decision
        return super.canContinueToUse();
    }
}