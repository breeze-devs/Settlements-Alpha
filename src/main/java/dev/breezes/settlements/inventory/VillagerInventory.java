package dev.breezes.settlements.inventory;

import dev.breezes.settlements.util.NbtTags;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class VillagerInventory {

    private static final String NBT_TAG = NbtTags.of("villager_inventory");

    @Getter
    private final int backpackSize;
    @Getter
    private final SimpleContainer backpack;

    private ItemStack mainHand;
    private ItemStack offHand;

    @Builder
    public VillagerInventory(int backpackSize) {
        this.backpackSize = backpackSize;
        this.backpack = new SimpleContainer(backpackSize);

        this.mainHand = ItemStack.EMPTY;
        this.offHand = ItemStack.EMPTY;
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
        return remainder.isEmpty() ? Optional.empty() : Optional.of(remainder);
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

        return amount - remaining;
    }

    /**
     * Sets main-hand item, returning the previous item if present
     */
    public Optional<ItemStack> setMainHand(ItemStack newItem) {
        ItemStack previous = this.mainHand;
        this.mainHand = (newItem == null || newItem.isEmpty()) ? ItemStack.EMPTY : newItem.copy();
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    /**
     * Sets off-hand item, returning the previous item if present
     */
    public Optional<ItemStack> setOffHand(ItemStack newItem) {
        ItemStack previous = this.offHand;
        this.offHand = (newItem == null || newItem.isEmpty()) ? ItemStack.EMPTY : newItem.copy();
        return previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    /*
     * Serialization & deserialization to NBT tags
     */
    public void writeInventoryToTag(CompoundTag nbtTag, HolderLookup.Provider levelRegistry) {
        nbtTag.put(NBT_TAG, this.backpack.createTag(levelRegistry));
    }

    public void readInventoryFromTag(CompoundTag tag, HolderLookup.Provider levelRegistry) {
        if (tag.contains(NBT_TAG, Tag.TAG_LIST)) {
            this.backpack.fromTag(tag.getList(NBT_TAG, Tag.TAG_COMPOUND), levelRegistry);
        }
    }

}
