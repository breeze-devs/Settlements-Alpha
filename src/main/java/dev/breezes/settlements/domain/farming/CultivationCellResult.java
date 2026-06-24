package dev.breezes.settlements.domain.farming;

import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

/**
 * The resolved category for one zone cell, paired with its position so the behavior
 * can act on the cell without re-deriving its location from a stream index.
 *
 * @param cellPos  the ground position ({@code totemPos.below().getY()} plane)
 * @param category what cultivation work (if any) the cell requires
 */
public record CultivationCellResult(
        @Nonnull BlockPos cellPos,
        @Nonnull CultivationCellCategory category
) {
}
