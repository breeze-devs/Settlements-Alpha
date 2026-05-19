package dev.breezes.settlements.domain.inventory;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class VillagerInventory implements IVillagerEquipment {

    @Getter
    private final int backpackSize;
    @Getter
    private final SimpleContainer backpack;

    private final EnumMap<EquipmentSlot, ItemStack> equipped;

    @Getter
    private int inventoryVersion;

    @Builder
    public VillagerInventory(int backpackSize) {
        this.backpackSize = backpackSize;
        this.backpack = new SimpleContainer(backpackSize);

        this.equipped = new EnumMap<>(EquipmentSlot.class);
        this.inventoryVersion = 0;
    }

    public Optional<ItemStack> getMainHand() {
        return this.getEquipped(EquipmentSlot.MAIN_HAND);
    }

    public Optional<ItemStack> getOffHand() {
        return this.getEquipped(EquipmentSlot.OFF_HAND);
    }

    @Override
    public Optional<ItemStack> getEquipped(@Nonnull EquipmentSlot slot) {
        ItemStack equippedStack = this.equipped.get(slot);
        return equippedStack == null || equippedStack.isEmpty() ? Optional.empty() : Optional.of(equippedStack);
    }

    public Optional<ItemStack> setEquipped(@Nonnull EquipmentSlot slot, @Nullable ItemStack newItem) {
        ItemStack previous = this.equipped.get(slot);

        if (newItem == null || newItem.isEmpty()) {
            this.equipped.remove(slot);
        } else {
            // The loadout owns its stored stacks so behavior code cannot mutate equipment by retaining a reference.
            this.equipped.put(slot, newItem.copy());
        }

        this.inventoryVersion++;
        return previous == null || previous.isEmpty() ? Optional.empty() : Optional.of(previous);
    }

    @Override
    public Set<EquipmentSlot> occupiedSlots() {
        EnumSet<EquipmentSlot> occupiedSlots = EnumSet.noneOf(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.equipped.get(slot);
            if (stack != null && !stack.isEmpty()) {
                occupiedSlots.add(slot);
            }
        }

        return Collections.unmodifiableSet(occupiedSlots);
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
     * Count items in backpack matching an {@link ItemMatch} (item ref or tag).
     */
    public int countMatching(@Nonnull ItemMatch match) {
        int total = 0;
        for (ItemStack stack : this.backpack.getItems()) {
            if (ItemMatches.test(match, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Returns true if {@code bypass} is on, or if the item is present. Use to guard containsItem
     * checks in behaviors that respect {@code GeneralConfig.bypassInventoryRequirements}.
     */
    public boolean containsOrBypassed(@Nonnull Item item, boolean bypass) {
        return bypass || this.containsItem(item);
    }

    /**
     * If {@code bypass} is off, consumes {@code amount} of {@code item} and returns true only when
     * exactly that amount was consumed. If {@code bypass} is on, skips the consume and returns true.
     */
    public boolean consumeIfRequired(@Nonnull Item item, int amount, boolean bypass) {
        if (bypass) {
            return true;
        }
        return this.consume(item, amount) == amount;
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
        return this.setEquipped(EquipmentSlot.MAIN_HAND, newItem);
    }

    /**
     * Sets off-hand item, returning the previous item if present
     */
    public Optional<ItemStack> setOffHand(ItemStack newItem) {
        return this.setEquipped(EquipmentSlot.OFF_HAND, newItem);
    }

}
