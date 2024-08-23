package dev.breezes.settlements.entities.villager.animations.animator;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.animations.definitions.VillagerAnimationDefinition;
import lombok.CustomLog;

import java.util.List;

/**
 * Animator that only plays once and stops after the animation is done
 */
@CustomLog
public class OneShotAnimator extends BaseAnimator {

    private final String animatorName;
    private boolean shouldAnimate;

    public OneShotAnimator(String animatorName, BaseVillager villager, List<VillagerAnimationDefinition> animations) {
        super(log, animatorName, villager, animations, false);
        this.animatorName = animatorName;
        this.shouldAnimate = false;
    }

    @Override
    protected boolean shouldAnimate() {
        if (this.shouldAnimate) {
            this.shouldAnimate = false;
            return true;
        }
        return false;
    }

    public void playOnce() {
        log.debug("[%s] Playing one-shot animation", this.animatorName);
        this.shouldAnimate = true;
    }

}
