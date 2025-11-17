package dev.breezes.settlements.entities.wolves.goals;

import dev.breezes.settlements.entities.wolves.SettlementsWolf;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;

public class WolfSitWhenOrderedToGoal extends SitWhenOrderedToGoal {

    private final SettlementsWolf wolf;

    public WolfSitWhenOrderedToGoal(SettlementsWolf wolf) {
        super(wolf);
        this.wolf = wolf;
    }

    /**
     * Simplify condition as we don't care about owners
     * - mostly copied from super class
     */
    @Override
    public boolean canUse() {
        if (this.wolf.isInWaterOrBubble() || !this.wolf.onGround())
            return false;
        return this.wolf.isOrderedToSit();
    }

}