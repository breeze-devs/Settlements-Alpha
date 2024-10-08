package dev.breezes.settlements.entities.villager.animations.animator;

import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.entities.villager.animations.conditions.AnimationCondition;
import dev.breezes.settlements.entities.villager.animations.definitions.VillagerAnimationDefinition;
import lombok.CustomLog;

import java.util.List;

@CustomLog
public class ConditionAnimator extends BaseAnimator {

    private final AnimationCondition condition;

    public ConditionAnimator(String animatorName, BaseVillager villager, AnimationCondition condition, List<VillagerAnimationDefinition> animations, boolean stopImmediately) {
        super(log, animatorName, villager, animations, stopImmediately);
        this.condition = condition;
    }

    @Override
    protected boolean shouldAnimate() {
        return this.condition.apply(this.entityData);
    }

}
