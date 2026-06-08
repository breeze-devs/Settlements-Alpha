package dev.breezes.settlements.application.ai.behavior.teardown;

import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * Obligation to restore a block at a position to a previous block type after a crash.
 * <p>
 * Unlike {@link ResetBlockStateObligation}, which resets a *property* on the same block,
 * this obligation handles full block identity swaps (e.g. WATER_CAULDRON → CAULDRON) that
 * cannot be expressed as a single property change.
 * <p>
 * The {@code ownedBlockId} guard ensures the discharge is a no-op when:
 * - The cauldron was already drained normally during the behavior (the block would be
 * CAULDRON again, not WATER_CAULDRON), making {@link #stillValid} return false.
 * - A player replaced the block with something else, preventing a clobber of their work.
 */
public record RestoreBlockObligation(@Nonnull BlockPos pos,
                                     @Nonnull ResourceLocation restoreBlockId,
                                     @Nonnull ResourceLocation ownedBlockId) implements TeardownObligation {

    @Override
    public BlockPos targetPos() {
        return this.pos;
    }

    @Override
    public boolean stillValid(@Nonnull ServerLevel level) {
        if (!level.isLoaded(this.pos)) {
            return false;
        }

        Block owned = resolveBlock(this.ownedBlockId);
        BlockState current = level.getBlockState(this.pos);
        return current.is(owned);
    }

    @Override
    public void discharge(@Nonnull ServerLevel level) {
        Block owned = resolveBlock(this.ownedBlockId);
        BlockState current = level.getBlockState(this.pos);
        if (!current.is(owned)) {
            return;
        }

        Block restore = resolveBlock(this.restoreBlockId);
        level.setBlock(this.pos, restore.defaultBlockState(), BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

    @Override
    public boolean durable() {
        return true;
    }

    @Override
    public String describe() {
        return "restore block " + this.restoreBlockId + " (owned: " + this.ownedBlockId + ") at " + this.pos.toShortString();
    }

    private static Block resolveBlock(@Nonnull ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id);
    }

}
