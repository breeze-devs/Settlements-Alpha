package dev.breezes.settlements.domain.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * Unified value type for a single kind in the ledger.
 * Reused by entries(), the network snapshot DTO, and the persistence codec so there is no
 * parallel representation to keep in sync.
 */
public record BackpackEntry(ItemStack representative, int count) {
}
