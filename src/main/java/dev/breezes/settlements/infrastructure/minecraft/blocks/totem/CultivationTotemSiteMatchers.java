package dev.breezes.settlements.infrastructure.minecraft.blocks.totem;

import dev.breezes.settlements.domain.world.blocks.LiveBlockSiteMatcher;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.entity.BlockEntity;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CultivationTotemSiteMatchers {

    /**
     * Totem work availability is owned by the block entity, so this matcher intentionally stays
     * live-only rather than pretending it can participate in snapshot-pure resource scans
     */
    public static final LiveBlockSiteMatcher NEEDS_WORK = (pos, level) -> {
        if (!(level.getBlockState(pos).getBlock() instanceof TotemOfCultivationBlock)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TotemOfCultivationBlockEntity totem)) {
            return false;
        }

        return totem.isValid() && totem.needsCultivation();
    };

}
