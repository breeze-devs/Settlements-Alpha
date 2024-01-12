package dev.breezes.settlements.entities.villager.animation.conditions;

import net.minecraft.network.syncher.SynchedEntityData;

public abstract class AnimationCondition {

    public abstract boolean apply(SynchedEntityData entityData);

    public static Builder builder(AnimationCondition baseCondition) {
        return new Builder(baseCondition);
    }

    public static class Builder {

        private AnimationCondition condition;

        private Builder(AnimationCondition baseCondition) {
            this.condition = baseCondition;
        }

        public Builder and(AnimationCondition other, AnimationCondition... others) {
            this.condition = new AndAnimationCondition(this.condition, other, others);
            return this;
        }

        public Builder or(AnimationCondition other, AnimationCondition... others) {
            this.condition = new OrAnimationCondition(this.condition, other, others);
            return this;
        }

        public AnimationCondition build() {
            return this.condition;
        }

    }

}
