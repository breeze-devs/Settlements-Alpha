package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Live-world confirmation contract for remembered block sites
 * <p>
 * Snapshot-pure resource matchers and deliberately live-only block-entity checks can both
 * confirm a remembered site without letting block-entity access leak into resource scans
 */
@FunctionalInterface
public interface LiveBlockSiteMatcher {

    boolean matches(@Nonnull BlockPos pos, @Nonnull Level level);

}
