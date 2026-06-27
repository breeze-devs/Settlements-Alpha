package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.domain.world.blocks.BlockStateView;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

import javax.annotation.Nonnull;

/**
 * A {@link BlockStateView} backed entirely by pre-copied {@link PalettedContainer} snapshots.
 * <p>
 * This is the thread boundary. A worker thread can hold and read this object safely because:
 * <ul>
 *   <li>Every container was copied on the main thread via {@code PalettedContainer.copy()} before
 *       the task was submitted — the copy is independent and has no concurrent writer.</li>
 *   <li>The view holds only the target section plus its axis-aligned face-neighbor sections, giving
 *       it full coverage for any neighbor read a matcher can issue (≤ 2 blocks away).</li>
 * </ul>
 * Reads to positions outside any snapshotted section return {@code Blocks.AIR} — this matches
 * what happens at the loaded world edge and is safe because matchers never require a non-air
 * neighbor to produce a false positive.
 */
@AllArgsConstructor
final class SnapshotBlockStateView implements BlockStateView {

    /**
     * Keyed by packed {@code SectionPos.asLong()} → copied section container.
     * Built on the main thread, read on the worker thread.
     */
    private final Long2ObjectMap<PalettedContainer<BlockState>> sectionsByKey;

    @Nonnull
    @Override
    public BlockState getBlockState(@Nonnull BlockPos pos) {
        int sectionX = SectionPos.blockToSectionCoord(pos.getX());
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        int sectionZ = SectionPos.blockToSectionCoord(pos.getZ());
        long key = SectionPos.asLong(sectionX, sectionY, sectionZ);

        PalettedContainer<BlockState> container = this.sectionsByKey.get(key);
        if (container == null) {
            // Section not snapshotted (unloaded neighbor or out of level bounds) → air.
            return Blocks.AIR.defaultBlockState();
        }

        // Local coordinates within the 16³ section (0–15 on each axis).
        int lx = pos.getX() & 15;
        int ly = pos.getY() & 15;
        int lz = pos.getZ() & 15;
        return container.get(lx, ly, lz);
    }

}
