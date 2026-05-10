package dev.breezes.settlements.infrastructure.minecraft.entities.cats.goals;

import dev.breezes.settlements.infrastructure.minecraft.entities.cats.SettlementsCat;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class CatFollowOwnerGoal extends FollowOwnerGoal {

    private final SettlementsCat cat;

    public CatFollowOwnerGoal(SettlementsCat cat, double speed, float minDistance, float maxDistance) {
        super(cat, speed, minDistance, maxDistance);
        this.cat = cat;
    }

    @Override
    public boolean canUse() {
        if (this.cat.isFollowOwnerLocked()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.cat.isFollowOwnerLocked()) {
            return false;
        }
        return super.canContinueToUse();
    }

}
