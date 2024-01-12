package dev.breezes.settlements.entities.villager.animation.animator;

import com.mojang.logging.LogUtils;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.animation.conditions.AnimationCondition;
import org.slf4j.Logger;

public class ConditionAnimator extends BaseAnimator {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AnimationCondition condition;

    public ConditionAnimator(BaseVillager villager, AnimationCondition condition, float animationDurationSeconds, boolean stopImmediately) {
        super(villager, animationDurationSeconds, stopImmediately);
        this.condition = condition;
    }

    @Override
    protected boolean shouldAnimate() {
        return this.condition.apply(this.entityData);
    }

}
