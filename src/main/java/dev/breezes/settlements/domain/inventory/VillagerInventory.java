package dev.breezes.settlements.domain.inventory;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Getter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class VillagerInventory implements IVillagerEquipment {

    private final IVillagerBackpack backpack;
    private final EnumMap<EquipmentSlot, ItemStack> equipped;

    @Getter
    private int inventoryVersion;

    public VillagerInventory() {
        this.backpack = new StackLedger();
        this.equipped = new EnumMap<>(EquipmentSlot.class);
        this.inventoryVersion = 0;
    }

    public int count(@Nonnull Item item) {
        return this.backpack.count(item);
    }

    public boolean contains(@Nonnull Item item) {
        return this.backpack.contains(item);
    }

    public boolean anyMatch(@Nonnull Predicate<ItemStack> predicate) {
        return this.backpack.anyMatch(predicate);
    }

    public Optional<ItemStack> findFirst(@Nonnull Predicate<ItemStack> predicate) {
        return this.backpack.findFirst(predicate);
    }

    public int countMatching(@Nonnull ItemMatch match) {
        return this.backpack.countMatching(match);
    }

    public int consume(@Nonnull Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int consumed = this.backpack.consume(item, amount);
        if (consumed > 0) {
            this.inventoryVersion++;
        }
        return consumed;
    }

    public int consume(@Nonnull ItemStack variant, int amount) {
        int consumed = this.backpack.consume(variant, amount);
        if (consumed > 0) {
            this.inventoryVersion++;
        }
        return consumed;
    }

    public void add(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        this.backpack.add(stack);
        this.inventoryVersion++;
    }

    public List<BackpackEntry> entries() {
        return this.backpack.entries();
    }

    public int totalItemCount() {
        return this.backpack.totalItemCount();
    }

    public int distinctKinds() {
        return this.backpack.distinctKinds();
    }

    // --- Bypass helpers (config concern, not the ledger's job) ---

    public boolean containsOrBypassed(@Nonnull Item item, boolean bypass) {
        return bypass || this.contains(item);
    }

    public boolean consumeIfRequired(@Nonnull Item item, int amount, boolean bypass) {
        if (bypass) {
            return true;
        }
        return this.consume(item, amount) == amount;
    }

    // --- Equipment ---

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

    public Optional<ItemStack> setMainHand(ItemStack newItem) {
        return this.setEquipped(EquipmentSlot.MAIN_HAND, newItem);
    }

    public Optional<ItemStack> setOffHand(ItemStack newItem) {
        return this.setEquipped(EquipmentSlot.OFF_HAND, newItem);
    }

}
