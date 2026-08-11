package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * A held Cultivation Lily and where it would land, bundled so a consumer never has to assume which
 * hand the resolution came from.
 */
public record CultivationLilyPlacementCandidate(@Nonnull InteractionHand hand,
                                                @Nonnull ItemStack stack,
                                                @Nonnull BlockPos placementPos) {
}
