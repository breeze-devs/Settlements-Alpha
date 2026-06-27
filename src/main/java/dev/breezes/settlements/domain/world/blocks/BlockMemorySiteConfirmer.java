package dev.breezes.settlements.domain.world.blocks;

import dev.breezes.settlements.domain.ai.navigation.ReachabilityChecker;
import dev.breezes.settlements.domain.world.location.Location;
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
                                                     @Nonnull LiveBlockSiteMatcher matcher,
                                                     @Nonnull BlockScanBox confirmBox,
                                                     int maxSitesToConfirm,
                                                     @Nonnull ReachabilityChecker reachabilityChecker,
                                                     int completionRange) {
        if (maxSitesToConfirm < 1) {
            throw new IllegalArgumentException("Max sites to confirm must be at least 1");
        }
        if (completionRange < 1) {
            throw new IllegalArgumentException("Completion range must be at least 1");
        }

        return rememberedSites.stream()
                .filter(site -> site.dimension().equals(level.dimension()))
                .sorted(Comparator.comparingDouble(site -> site.pos().distSqr(center)))
                .map(site -> AabbBlockScan.findFirst(site.pos(), confirmBox, matcher, level)
                        .map(pos -> GlobalPos.of(level.dimension(), pos)))
                .flatMap(Optional::stream)
                // Cap the budget on live (still-matching) sites rather than nearest remembered entries, so a
                // reachable site is not abandoned just because nearer entries are stale or unreachable (e.g.
                // buried or fenced off). Reachability is the expensive check, so it is the one worth bounding.
                .limit(maxSitesToConfirm)
                .filter(site -> reachabilityChecker.canReach(Location.of(site.pos(), level), completionRange))
                .findFirst();
    }

}
