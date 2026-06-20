package dev.breezes.settlements.infrastructure.minecraft.chest;

import dev.breezes.settlements.infrastructure.minecraft.attachments.ChestWaxAttachment;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Service layer for the wax-chest mechanic
 * <p>
 * Exposes isWaxed / setWaxed without callers needing to know about double-chest topology
 * <p>
 * The attachment lives on each BlockEntity half so that a wax query on either half returns the
 * correct answer — important because the sensor stores the LEFT half but the player may interact
 * with either.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChestWaxService {

    /**
     * Returns true if the chest at {@code pos} (or its partner half, for double-chests) is waxed
     */
    public static boolean isWaxed(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null && ChestWaxAttachment.isWaxed(blockEntity)) {
            return true;
        }

        // A single waxed half is enough to gate the whole double-chest. This covers the case
        // where the sensor always stores the LEFT half but setWaxed was called on the other half.
        return getPartnerPos(level, pos)
                .map(level::getBlockEntity)
                .map(ChestWaxAttachment::isWaxed)
                .orElse(false);
    }

    /**
     * Stamps or un-stamps the wax flag on the chest and its partner half (if any)
     */
    public static void setWaxed(Level level, BlockPos pos, boolean waxed) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            ChestWaxAttachment.setWaxed(blockEntity, waxed);
        }

        // Both halves must be stamped so either half can answer an isWaxed probe correctly.
        getPartnerPos(level, pos)
                .map(level::getBlockEntity)
                .ifPresent(partner -> ChestWaxAttachment.setWaxed(partner, waxed));
    }

    /**
     * Returns the position of the connected double-chest half, or empty for a single chest
     */
    public static Optional<BlockPos> getPartnerPos(@Nonnull Level level, @Nonnull BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(ChestBlock.TYPE) || !state.hasProperty(ChestBlock.FACING)) {
            return Optional.empty();
        }

        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return Optional.empty();
        }

        return Optional.of(pos.relative(ChestBlock.getConnectedDirection(state)));
    }

}
