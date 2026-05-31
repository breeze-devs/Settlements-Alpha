package dev.breezes.settlements.domain.inventory;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface IVillagerBackpack {

    /**
     * Aggregate count of all component variants of {@code item}.
     */
    int count(Item item);

    /**
     * Aggregate count of all entries matching the ItemMatch rule.
     */
    int countMatching(ItemMatch match);

    /**
     * True when any component variant of {@code item} is present.
     */
    boolean contains(Item item);

    /**
     * True when any distinct kind matches without materializing a snapshot.
     */
    boolean anyMatch(Predicate<ItemStack> predicate);

    /**
     * Returns a defensive count-1 representative for the first matching kind.
     */
    Optional<ItemStack> findFirst(Predicate<ItemStack> predicate);

    /**
     * Drains up to {@code amount} of any component variant of {@code item}.
     * Returns the actual number consumed (0..amount).
     */
    int consume(Item item, int amount);

    /**
     * Drains up to {@code amount} of the specific component variant identified by {@code variant}.
     * The variant's own count is ignored; {@code amount} is authoritative.
     * Returns the actual number consumed (0..amount).
     */
    int consume(ItemStack variant, int amount);

    /**
     * Adds all items in {@code stack} to the ledger. Always succeeds — capacity is infinite.
     */
    void add(ItemStack stack);

    /**
     * Returns an immutable snapshot of all distinct kinds currently held.
     * Each entry's representative is a count-1 copy; counts can exceed 64 or 99.
     */
    List<BackpackEntry> entries();

    /**
     * Sum of all individual item counts across all kinds.
     */
    int totalItemCount();

    /**
     * Number of distinct kinds (equal to entries().size()).
     */
    int distinctKinds();

}
