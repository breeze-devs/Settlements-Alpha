package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Resolves where a held Cultivation Lily would land.
 * <p>
 * Minecraft.hitResult cannot answer this: its pick never reports a fluid, so it resolves the block
 * behind the water rather than the water itself. This runs its own fluid-aware clip instead.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationLilyPlacementAim {

    /**
     * The position a placement would occupy, or empty when the aim resolves nowhere placeable.
     */
    public static Optional<BlockPos> resolve(@Nonnull Level level,
                                             @Nonnull Player player,
                                             @Nonnull InteractionHand hand,
                                             @Nonnull ItemStack heldStack,
                                             float partialTick) {
        Vec3 eyePos = player.getEyePosition(partialTick);
        Vec3 viewVector = player.getViewVector(partialTick);
        double reach = player.blockInteractionRange();

        BlockHitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(viewVector.scale(reach)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        FluidState fluidHit = level.getBlockState(hit.getBlockPos()).getFluidState();
        if (!fluidHit.is(Fluids.WATER) || !fluidHit.isSource()) {
            return Optional.empty();
        }

        // The clicked position becomes the block above the source. BlockPlaceContext then decides,
        // from that position, whether it is replaceable — and when it is not, placement falls
        // back to a position relative to the original hit's face, which is the case this rejects.
        BlockPos abovePos = hit.getBlockPos().above();
        BlockPlaceContext context = new BlockPlaceContext(player, hand, heldStack, hit.withPosition(abovePos));
        if (!context.replacingClickedOnBlock()) {
            return Optional.empty();
        }

        return Optional.of(context.getClickedPos());
    }

}
