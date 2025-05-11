package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;

import javax.annotation.Nullable;

public class NearbyShearableSheepExistsCondition<T extends BaseVillager> extends NearbyEntityExistsCondition<T, Sheep> {

    @Builder
    private NearbyShearableSheepExistsCondition(double rangeHorizontal, double rangeVertical) {
        super(rangeHorizontal, rangeVertical, EntityType.SHEEP, NearbyShearableSheepExistsCondition::isShearable, 1);
    }

    private static boolean isShearable(@Nullable Sheep sheep) {
        return sheep != null && sheep.readyForShearing();
    }

}
