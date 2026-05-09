package dev.breezes.settlements.domain.inventory;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class VillagerInventory {

    @Getter
    private final int backpackSize;
    @Getter
    private final SimpleContainer backpack;

    private ItemStack mainHand;
    private ItemStack offHand;

    @Getter
    private int inventoryVersion;

    @Builder
    public VillagerInventory(int backpackSize) {
        this.backpackSize = backpackSize;
        this.backpack = new SimpleContainer(backpackSize);

        this.mainHand = ItemStack.EMPTY;
        this.offHand = ItemStack.EMPTY;
        this.inventoryVersion = 0;
    }

    public Optional<ItemStack> getMainHand() {
        return this.mainHand.isEmpty() ? Optional.empty() : Optional.of(this.mainHand);
    }

    public Optional<ItemStack> getOffHand() {
        return this.offHand.isEmpty() ? Optional.empty() : Optional.of(this.offHand);
    }

    /**
     * Adds an item stack to the backpack and returns an Optional leftover if not all items could be inserted
     */
    public Optional<ItemStack> addItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }
        // Work on a copy to avoid mutating the caller's stack reference
        ItemStack remainder = this.backpack.addItem(item.copy());
        this.inventoryVersion++;
        return remainder.isEmpty() ? Optional.empty() : Optional.of(remainder);
    }

    /**
     * Adds an item stack to the backpack and returns an Optional leftover dropped item entity if not all items could be inserted
     */
    public Optional<ItemEntity> addOrDropItem(ItemStack item, Level level, double x, double y, double z) {
        Optional<ItemStack> remainder = this.addItem(item);
        return remainder.map(leftover -> {
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, leftover.copy());
            level.addFreshEntity(itemEntity);
            return itemEntity;
        });
    }

    /**
     * Checks if the backpack can fully accept the given stack (delegates to SimpleContainer.canAddItem)
     */
    public boolean canAddItem(ItemStack item) {
        return item != null && !item.isEmpty() && this.backpack.canAddItem(item);
    }

    /**
     * Count number of items in backpack matching the provided item type.
     */
    public int countItem(Item item) {
        return item == null ? 0 : this.backpack.countItem(item);
    }

    /**
     * Checks whether backpack contains at least one of the provided item type.
     */
    public boolean containsItem(Item item) {
        return this.countItem(item) > 0;
    }

    /**
     * Consume up to {@code amount} items matching the provided item type.
     *
     * <p>If requested amount is larger than what is available, this method consumes all
     * available matching items and returns the actual consumed count.</p>
     *
     * @return actual number of consumed items (0..amount)
     */
    public int consume(Item item, int amount) {
        if (item == null || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        for (int i = 0; i < this.backpack.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = this.backpack.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }

            int toConsume = Math.min(stack.getCount(), remaining);
            stack.shrink(toConsume);
            remaining -= toConsume;

            if (stack.isEmpty()) {
                this.backpack.setItem(i, ItemStack.EMPTY);
            }
        }

        int consumed = amount - remaining;
        if (consumed > 0) {
            this.inventoryVersion++;
        }
        return consumed;
    }

    public int consumeFromSlot(int slot, int amount) {
        if (slot < 0 || slot >= this.backpack.getContainerSize() || amount <= 0) {
            return 0;
        }

        ItemStack stack = this.backpack.getItem(slot);
        if (stack.isEmpty()) {
            return 0;
        }

        int consumed = Math.min(stack.getCount(), amount);
        stack.shrink(consumed);
        if (stack.isEmpty()) {
            this.backpack.setItem(slot, ItemStack.EMPTY);
        }

        if (consumed > 0) {
            this.inventoryVersion++;
        }
        return consumed;
    }

    /**
     * Sets main-hand item, returning the previous item if present
     */
    public Optional<ItemStack> setMainHand(ItemStack newItem) {
        ItemStack previous = this.mainHand;
        this.mainHand = (newItem == null || newItem.isEmpty()) ? ItemStack.EMPTY : newItem.copy();
        this.inventoryVersion++;
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    /**
     * Sets off-hand item, returning the previous item if present
     */
    public Optional<ItemStack> setOffHand(ItemStack newItem) {
        ItemStack previous = this.offHand;
        this.offHand = (newItem == null || newItem.isEmpty()) ? ItemStack.EMPTY : newItem.copy();
        this.inventoryVersion++;
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

}
