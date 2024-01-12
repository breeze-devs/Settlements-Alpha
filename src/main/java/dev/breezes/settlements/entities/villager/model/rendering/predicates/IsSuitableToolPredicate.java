package dev.breezes.settlements.entities.villager.model.rendering.predicates;

import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;

import java.util.function.Predicate;

public class IsSuitableToolPredicate implements Predicate<Item> {

    @Override
    public boolean test(Item item) {
        // TieredItem: Sword, Axe, Pickaxe, Shovel, Hoe
        return item instanceof TieredItem || item instanceof FishingRodItem;
    }

}
