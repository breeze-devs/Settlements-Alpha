package dev.breezes.settlements.entities.villager.animation.conditions;

import net.minecraft.network.syncher.SynchedEntityData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AndAnimationCondition extends AnimationCondition {

    private final List<AnimationCondition> conditions;

    public AndAnimationCondition(AnimationCondition condition1, AnimationCondition condition2, AnimationCondition... conditions) {
        this.conditions = new ArrayList<>();
        this.conditions.add(condition1);
        this.conditions.add(condition2);

        if (conditions != null) {
            this.conditions.addAll(Arrays.asList(conditions));
        }
    }

    @Override
    public boolean apply(SynchedEntityData entityData) {
        return this.conditions.stream().allMatch(condition -> condition.apply(entityData));
    }

}
