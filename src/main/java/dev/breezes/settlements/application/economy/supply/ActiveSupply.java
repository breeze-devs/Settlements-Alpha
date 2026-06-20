package dev.breezes.settlements.application.economy.supply;

import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import lombok.Builder;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

@Builder
public record ActiveSupply(
        @Nonnull ItemMatch match,
        @Nonnull ItemStack representative,
        int dumpableCount
) {
}
