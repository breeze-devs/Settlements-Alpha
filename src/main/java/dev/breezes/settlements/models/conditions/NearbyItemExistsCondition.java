package dev.breezes.settlements.models.conditions;

import dev.breezes.settlements.entities.villager.BaseVillager;
import lombok.Builder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;

public class NearbyItemExistsCondition<T extends BaseVillager> extends NearbyEntityExistsCondition<T, ItemEntity> {

    @Builder
    private NearbyItemExistsCondition(double rangeHorizontal,
                                      double rangeVertical,
                                      @Nonnull IEntityCondition<ItemEntity> itemCondition,
                                      int minimumTargetCount) {
        super(rangeHorizontal, rangeVertical, EntityType.ITEM, itemCondition, minimumTargetCount);
    }

}
