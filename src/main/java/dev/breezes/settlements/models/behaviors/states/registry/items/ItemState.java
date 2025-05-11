package dev.breezes.settlements.models.behaviors.states.registry.items;

import dev.breezes.settlements.models.behaviors.states.BehaviorState;
import dev.breezes.settlements.models.conditions.ICondition;
import lombok.Getter;
import net.minecraft.world.entity.item.ItemEntity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class ItemState implements BehaviorState {

    /**
     * List of targets, possibly ranked by priority
     * <p>
     * Must be mutable to allow for dynamic target selection
     */
    private List<ItemEntity> items;

    public ItemState(@Nonnull List<ItemEntity> items) {
        this.items = new ArrayList<>(items);
    }

    public static ItemState of(@Nonnull List<ItemEntity> items) {
        return new ItemState(items);
    }

    public void addItem(@Nonnull ItemEntity item) {
        this.items.add(item);
    }

    public Stream<ItemEntity> match(@Nonnull ICondition<ItemEntity> condition) {
        return this.items.stream()
                .filter(condition);
    }

    @Override
    public void reset() {
        this.items = new ArrayList<>();
    }

}
