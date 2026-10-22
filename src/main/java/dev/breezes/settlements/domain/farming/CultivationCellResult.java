package dev.breezes.settlements.domain.farming;

import net.minecraft.core.BlockPos;

/**
 * The resolved category for one zone cell, paired with its position so the behavior
 * can act on the cell without re-deriving its location from a stream index.
 *
 * @param cellPos  the ground position ({@code lilyPos.below().getY()} plane)
 * @param category what cultivation work (if any) the cell requires
 */
public record CultivationCellResult(
        BlockPos cellPos,
        CultivationCellCategory category
) {
}
