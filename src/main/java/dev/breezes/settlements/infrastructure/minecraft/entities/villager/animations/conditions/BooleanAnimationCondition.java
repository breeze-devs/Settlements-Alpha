package dev.breezes.settlements.infrastructure.minecraft.entities.villager.animations.conditions;

import dev.breezes.settlements.shared.util.SyncedDataWrapper;
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
