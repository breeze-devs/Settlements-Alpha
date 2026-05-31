package dev.breezes.settlements.domain.inventory;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class StackLedger implements IVillagerBackpack {

    /*
     * Keys are always immutable count-1 copies — TYPE_AND_TAG ignores count during hashing and
     * equality, so the real total lives in the int value rather than the key's stack count.
     * Never hand a live key reference out; always copy before returning.
     */
    private final Object2IntOpenCustomHashMap<ItemStack> counts =
            new Object2IntOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);

    @Override
    public void add(@Nonnull ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) {
            return;
        }

        this.counts.addTo(stack.copyWithCount(1), stack.getCount());
    }

    @Override
    public int count(@Nonnull Item item) {
        int total = 0;
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            if (entry.getKey().is(item)) {
                total += entry.getIntValue();
            }
        }
        return total;
    }

    @Override
    public int countMatching(@Nonnull ItemMatch match) {
        int total = 0;
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            if (ItemMatches.test(match, entry.getKey())) {
                total += entry.getIntValue();
            }
        }
        return total;
    }

    @Override
    public boolean contains(@Nonnull Item item) {
        for (ItemStack key : this.counts.keySet()) {
            if (key.is(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean anyMatch(@Nonnull Predicate<ItemStack> predicate) {
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            if (entry.getKey().isEmpty() || entry.getIntValue() <= 0) {
                continue;
            }
            if (predicate.test(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<ItemStack> findFirst(@Nonnull Predicate<ItemStack> predicate) {
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            if (entry.getKey().isEmpty() || entry.getIntValue() <= 0) {
                continue;
            }
            if (predicate.test(entry.getKey())) {
                return Optional.of(entry.getKey().copyWithCount(1));
            }
        }
        return Optional.empty();
    }

    @Override
    public int consume(@Nonnull Item item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        // Collect matching keys first to avoid mutating the map during iteration.
        List<ItemStack> matchingKeys = new ArrayList<>();
        for (ItemStack key : this.counts.keySet()) {
            if (key.is(item)) {
                matchingKeys.add(key);
            }
        }

        return this.drainFromKeys(matchingKeys, amount);
    }

    @Override
    public int consume(@Nonnull ItemStack variant, int amount) {
        if (variant.isEmpty() || amount <= 0) {
            return 0;
        }

        // Build a count-1 key to match the stored key via TYPE_AND_TAG.
        return this.drainFromKeys(List.of(variant.copyWithCount(1)), amount);
    }

    @Override
    public List<BackpackEntry> entries() {
        List<BackpackEntry> result = new ArrayList<>(this.counts.size());
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            if (entry.getKey().isEmpty() || entry.getIntValue() <= 0) {
                continue;
            }
            // Copy the stored count-1 key so callers cannot mutate the ledger's internal key.
            result.add(new BackpackEntry(entry.getKey().copyWithCount(1), entry.getIntValue()));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public int totalItemCount() {
        int total = 0;
        for (Object2IntMap.Entry<ItemStack> entry : this.counts.object2IntEntrySet()) {
            total += entry.getIntValue();
        }
        return total;
    }

    @Override
    public int distinctKinds() {
        return this.counts.size();
    }

    private int drainFromKeys(List<ItemStack> keys, int amount) {
        int remaining = amount;
        for (ItemStack key : keys) {
            if (remaining <= 0) {
                break;
            }

            int available = this.counts.getInt(key);
            if (available <= 0) {
                continue;
            }

            int toConsume = Math.min(available, remaining);
            remaining -= toConsume;
            int newCount = available - toConsume;
            if (newCount <= 0) {
                this.counts.removeInt(key);
            } else {
                this.counts.put(key, newCount);
            }
        }
        return amount - remaining;
    }

}
