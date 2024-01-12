package dev.breezes.settlements.entities.villager.animation.conditions;

import dev.breezes.settlements.util.SyncedDataWrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.syncher.SynchedEntityData;

@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public class BooleanAnimationCondition extends AnimationCondition {

    private final SyncedDataWrapper<Boolean> condition;

    public static BooleanAnimationCondition of(SyncedDataWrapper<Boolean> condition) {
        return new BooleanAnimationCondition(condition);
    }

    @Override
    public boolean apply(SynchedEntityData entityData) {
        return this.condition.get(entityData);
    }

}
