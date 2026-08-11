package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

import dev.breezes.settlements.domain.world.blocks.LiveBlockSiteMatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.block.entity.BlockEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationSiteMatchers {

    /**
     * Lily work availability is owned by the block entity, so this matcher intentionally stays
     * live-only rather than pretending it can participate in snapshot-pure resource scans
     */
    public static final LiveBlockSiteMatcher NEEDS_WORK = (pos, level) -> {
        if (!(level.getBlockState(pos).getBlock() instanceof CultivationLilyBlock)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CultivationLilyBlockEntity lily)) {
            return false;
        }

        return lily.isValid() && lily.needsCultivation();
    };

}
