package dev.breezes.settlements.models.conditions.basic;

import dev.breezes.settlements.models.conditions.IEntityCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemStackCheckCondition implements IEntityCondition<ItemEntity> {

    private final Predicate<ItemStack> predicate;

    public static ItemStackCheckCondition is(@Nonnull Item item) {
        return new ItemStackCheckCondition(check -> check.is(item));
    }

    public static ItemStackCheckCondition is(@Nonnull TagKey<Item> tags) {
        return new ItemStackCheckCondition(item -> item.is(tags));
    }

    @Override
    public boolean test(@Nullable ItemEntity itemEntity) {
        return itemEntity != null
                && itemEntity.isAlive()
                && predicate.test(itemEntity.getItem());
    }

}
