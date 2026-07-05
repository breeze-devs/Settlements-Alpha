package dev.breezes.settlements.domain.world.blocks;

import dev.breezes.settlements.domain.tags.SettlementsBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;

/**
 * Single source of truth for "is this block a barrier a villager can path through and operate" --
 * shared by the pathfinder evaluator (path-able) and the traversal behavior (operable). Both sides
 * MUST agree, or a villager paths into a barrier nothing opens and wedges the day plan forever.
 */
public enum TraversableBarrier {

    DOOR {
        @Override
        boolean matches(BlockState state) {
            return state.is(BlockTags.MOB_INTERACTABLE_DOORS, blockState -> blockState.getBlock() instanceof DoorBlock);
        }

        @Override
        public boolean isOpen(BlockState state) {
            return ((DoorBlock) state.getBlock()).isOpen(state);
        }

        @Override
        public void setOpen(@Nullable Entity actor, Level level, BlockState state, BlockPos pos, boolean open) {
            // DoorBlock#setOpen already handles the open/close sound and BLOCK_OPEN/BLOCK_CLOSE event.
            ((DoorBlock) state.getBlock()).setOpen(actor, level, state, pos, open);
        }
    },
    FENCE_GATE {
        @Override
        boolean matches(BlockState state) {
            return state.is(SettlementsBlockTags.MOB_INTERACTABLE_FENCE_GATES) && state.getBlock() instanceof FenceGateBlock;
        }

        @Override
        public boolean isOpen(BlockState state) {
            return state.getValue(FenceGateBlock.OPEN);
        }

        @Override
        public void setOpen(@Nullable Entity actor, Level level, BlockState state, BlockPos pos, boolean open) {
            // Mirrors FenceGateBlock#useWithoutItem
            level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, open), 10);
            FenceGateBlock gate = (FenceGateBlock) state.getBlock();
            level.playSound(null, pos, open ? gate.openSound : gate.closeSound, SoundSource.BLOCKS,
                    1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.gameEvent(actor, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
    };

    /**
     * Pure. Used only by {@link #resolve}.
     */
    abstract boolean matches(BlockState state);

    /**
     * Pure -- safe to call from the pathfinder evaluator.
     */
    public abstract boolean isOpen(BlockState state);

    /**
     * Effectful (sound + game event). Behavior-only -- never call this from the evaluator/ctor path.
     */
    public abstract void setOpen(@Nullable Entity actor, Level level, BlockState state, BlockPos pos, boolean open);

    private static final TraversableBarrier[] VALUES = values();

    /**
     * Returns the barrier this state matches, or {@code null} if it isn't an operable barrier.
     * Deliberately returns a raw nullable rather than {@code Optional} -- this is called per pathfinder
     * cell at scale, and an {@code Optional} allocation on that hot path is unwarranted.
     */
    @Nullable
    public static TraversableBarrier resolve(BlockState state) {
        for (TraversableBarrier barrier : VALUES) {
            if (barrier.matches(state)) {
                return barrier;
            }
        }
        return null;
    }

}
