package dev.breezes.settlements.application.ai.behavior.usecases.villager.animals;

import lombok.Builder;
import lombok.Value;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;

@Value
@Builder
public class BreedSpecies {

    EntityType<? extends Animal> type;
    TagKey<Item> foodTag;
    Item canonicalFood;

}
