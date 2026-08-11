package dev.breezes.settlements.domain.farming;

import dev.breezes.settlements.domain.world.blocks.BlockStateView;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The "is this a working farm zone" rule: a still water source directly beneath the lily position,
 * and at least one zone cell {@link CultivationZoneCategorizer#counts}.
 * <p>
 * Answering costs a read of every cell in the zone, so this is a throttled question rather than one
 * worth asking per frame or per interaction.
 * <p>
 * The foundation and the cells are read from one {@link CultivationZone}, whose own position is the
 * only lily position this rule will consider — there is no separate position parameter through which
 * one lily's water base could be checked against another lily's cells.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationLilyValidity {

    public static boolean isValid(@Nonnull BlockStateView view,
                                  @Nonnull CultivationZone zone,
                                  @Nullable ResourceLocation cropFilter) {
        if (!hasWaterSourceFoundation(view, zone.lilyPos())) {
            return false;
        }

        return zone.streamCells()
                .anyMatch(cellPos -> CultivationZoneCategorizer.counts(
                        CultivationZoneCategorizer.categorizeCell(view, cellPos, cropFilter)));
    }

    /**
     * Whether lilyPos sits on the only foundation a lily can survive on: a still water source directly below.
     */
    private static boolean hasWaterSourceFoundation(@Nonnull BlockStateView view, @Nonnull BlockPos lilyPos) {
        FluidState fluidBelow = view.getBlockState(lilyPos.below()).getFluidState();
        return fluidBelow.is(Fluids.WATER) && fluidBelow.isSource();
    }

}
