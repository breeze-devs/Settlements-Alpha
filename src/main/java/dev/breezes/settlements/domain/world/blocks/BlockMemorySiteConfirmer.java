package dev.breezes.settlements.domain.world.blocks;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockMemorySiteConfirmer {

    public static final int DEFAULT_MAX_CONFIRMS = 8;

    public static Optional<GlobalPos> confirmNearest(@Nonnull List<GlobalPos> rememberedSites,
                                                     @Nonnull BlockPos center,
                                                     @Nonnull Level level,
                                                     @Nonnull BlockMatcher matcher,
                                                     @Nonnull BlockScanBox confirmBox,
                                                     int maxSitesToConfirm) {
        if (maxSitesToConfirm < 1) {
            throw new IllegalArgumentException("Max sites to confirm must be at least 1");
        }

        return rememberedSites.stream()
                .filter(site -> site.dimension().equals(level.dimension()))
                .sorted(Comparator.comparingDouble(site -> site.pos().distSqr(center)))
                .limit(maxSitesToConfirm)
                .map(site -> AabbBlockScan.findFirst(site.pos(), confirmBox, matcher, level)
                        .map(pos -> GlobalPos.of(level.dimension(), pos)))
                .flatMap(Optional::stream)
                .findFirst();
    }

}
