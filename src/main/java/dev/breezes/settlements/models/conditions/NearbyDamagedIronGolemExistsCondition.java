package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;

import javax.annotation.Nullable;

/**
 * Condition that checks if a damaged golem exists nearby.
 */
public class NearbyDamagedIronGolemExistsCondition<T extends BaseVillager> extends NearbyEntityExistsCondition<T, IronGolem> {

    public NearbyDamagedIronGolemExistsCondition(double rangeHorizontal, double rangeVertical, double maxHealthPercentage) {
        super(rangeHorizontal, rangeVertical, EntityType.IRON_GOLEM, ironGolem -> isSufficientlyDamaged(ironGolem, maxHealthPercentage), 1);
    }

    private static boolean isSufficientlyDamaged(@Nullable IronGolem ironGolem, double maxHealthPercentage) {
        return ironGolem != null && ironGolem.getHealth() < ironGolem.getMaxHealth() * maxHealthPercentage;
    }

}
