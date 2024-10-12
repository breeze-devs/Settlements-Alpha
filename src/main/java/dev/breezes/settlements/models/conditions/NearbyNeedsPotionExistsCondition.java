package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nullable;

public class NearbyNeedsPotionExistsCondition<T extends BaseVillager, E extends LivingEntity> extends NearbyEntityExistsCondition<T, E> {

    public NearbyNeedsPotionExistsCondition(double rangeHorizontal, double rangeVertical, EntityType entityType) {
        super(rangeHorizontal, rangeVertical, entityType, entity -> needsPotion(entity) , 1);
    }

    private static boolean needsPotion(@Nullable LivingEntity entity) {
        return entity != null && (entity.getHealth() < entity.getMaxHealth() || (entity.isUnderWater() && !entity.hasEffect(MobEffects.WATER_BREATHING)));
    }
}
