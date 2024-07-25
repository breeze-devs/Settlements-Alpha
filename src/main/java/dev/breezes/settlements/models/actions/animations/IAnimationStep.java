package dev.breezes.settlements.models.actions.animations;

import dev.breezes.settlements.models.misc.ITickable;

public interface IAnimationStep {

    // TODO: ?
    // There should be an enum of animation steps, such as WALK_TO_GOLEM, TAKE_OUT_IRON_INGOT, REPAIR_IRON_GOLEM, etc.

    /**
     * Perform the animation step
     */
    void tick(int delta);

    /**
     * The remaining duration of the animation step, can be set to instant/permanent
     */
    ITickable getDurationRemaining();

}
