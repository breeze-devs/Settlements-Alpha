package dev.breezes.settlements.application.economy.supply;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Builder;
import net.minecraft.world.item.ItemStack;

@Builder
public record ActiveSupply(
        ItemMatch match,
        ItemStack representative,
        int dumpableCount
) {
}
