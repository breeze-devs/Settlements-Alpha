package dev.breezes.settlements.domain.world.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class AabbBlockScan {

    public static List<BlockPos> scanSortedByDistance(@Nonnull BlockPos center,
                                                      @Nonnull BlockScanBox box,
                                                      @Nonnull BlockMatcher matcher,
                                                      @Nonnull Level level) {
        List<BlockPos> matches = scanAxisAligned(center, box, matcher, level);
        matches.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return matches;
    }

    public static List<BlockPos> scanAxisAligned(@Nonnull BlockPos center,
                                                 @Nonnull BlockScanBox box,
                                                 @Nonnull BlockMatcher matcher,
                                                 @Nonnull Level level) {
        return scan(center, box, matcher, level, AabbBlockScan::normalPositionsMutable);
    }

    public static List<List<BlockPos>> scanMatchingEach(@Nonnull BlockPos center,
                                                        @Nonnull BlockScanBox box,
                                                        @Nonnull List<BlockMatcher> matchers,
                                                        @Nonnull Level level) {
        List<List<BlockPos>> matchesByMatcher = new ArrayList<>(matchers.size());
        for (int i = 0; i < matchers.size(); i++) {
            matchesByMatcher.add(new ArrayList<>());
        }

        for (BlockPos.MutableBlockPos pos : normalPositionsMutable(center, box)) {
            BlockState state = level.getBlockState(pos);
            for (int i = 0; i < matchers.size(); i++) {
                if (matchers.get(i).matches(pos, state, level)) {
                    matchesByMatcher.get(i).add(pos.immutable());
                }
            }
        }

        return matchesByMatcher;
    }

    public static List<BlockPos> scanNearestFirst(@Nonnull BlockPos center,
                                                  @Nonnull BlockScanBox box,
                                                  @Nonnull BlockMatcher matcher,
                                                  @Nonnull Level level) {
        return scan(center, box, matcher, level, AabbBlockScan::nearestFirstPositionsMutable);
    }

    private static List<BlockPos> scan(@Nonnull BlockPos center,
                                       @Nonnull BlockScanBox box,
                                       @Nonnull BlockMatcher matcher,
                                       @Nonnull Level level,
                                       @Nonnull PositionOrder positionOrder) {
        List<BlockPos> matches = new ArrayList<>();
        for (BlockPos.MutableBlockPos pos : positionOrder.positions(center, box)) {
            if (matcher.matches(pos, level)) {
                matches.add(pos.immutable());
            }
        }
        return matches;
    }

    public static Optional<BlockPos> findFirst(@Nonnull BlockPos center,
                                               @Nonnull BlockScanBox box,
                                               @Nonnull BlockMatcher matcher,
                                               @Nonnull Level level) {
        for (BlockPos.MutableBlockPos pos : nearestFirstPositionsMutable(center, box)) {
            if (matcher.matches(pos, level)) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    private static Iterable<BlockPos.MutableBlockPos> normalPositionsMutable(@Nonnull BlockPos center, @Nonnull BlockScanBox box) {
        return () -> new Iterator<>() {

            private final BlockPos.MutableBlockPos cursor = center.mutable();

            private int x = -box.horizontalRadius();
            private int y = -box.verticalRadius();
            private int z = -box.horizontalRadius();

            @Override
            public boolean hasNext() {
                return this.x <= box.horizontalRadius();
            }

            @Override
            public BlockPos.MutableBlockPos next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }

                this.cursor.set(center.getX() + this.x, center.getY() + this.y, center.getZ() + this.z);
                this.advance();
                return this.cursor;
            }

            private void advance() {
                this.z++;
                if (this.z <= box.horizontalRadius()) {
                    return;
                }

                this.z = -box.horizontalRadius();
                this.y++;
                if (this.y <= box.verticalRadius()) {
                    return;
                }

                this.y = -box.verticalRadius();
                this.x++;
            }
        };
    }

    private static Iterable<BlockPos.MutableBlockPos> nearestFirstPositionsMutable(@Nonnull BlockPos center, @Nonnull BlockScanBox box) {
        return () -> new Iterator<>() {

            private final int maxRadius = Math.max(box.horizontalRadius(), box.verticalRadius());
            private final BlockPos.MutableBlockPos cursor = center.mutable();

            private int radius = 0;
            private int x = 0;
            private int y = 0;
            private int z = 0;

            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return this.hasNext;
            }

            @Override
            public BlockPos.MutableBlockPos next() {
                if (!this.hasNext) {
                    throw new NoSuchElementException();
                }

                this.cursor.set(center.getX() + this.x, center.getY() + this.y, center.getZ() + this.z);
                this.advance();
                return this.cursor;
            }

            private void advance() {
                if (this.radius == 0) {
                    this.radius = 1;
                    this.x = -1;
                    this.y = -1;
                    this.z = -1;
                } else {
                    this.advanceCursor();
                }

                this.hasNext = false;
                while (this.radius <= this.maxRadius) {
                    if (this.isWithinBox() && this.isOnCurrentShell()) {
                        this.hasNext = true;
                        return;
                    }
                    this.advanceCursor();
                }
            }

            private void advanceCursor() {
                this.z++;
                if (this.z <= this.radius) {
                    return;
                }

                this.z = -this.radius;
                this.y++;
                if (this.y <= this.radius) {
                    return;
                }

                this.y = -this.radius;
                this.x++;
                if (this.x <= this.radius) {
                    return;
                }

                this.radius++;
                this.x = -this.radius;
                this.y = -this.radius;
                this.z = -this.radius;
            }

            private boolean isWithinBox() {
                return Math.abs(this.x) <= box.horizontalRadius()
                        && Math.abs(this.y) <= box.verticalRadius()
                        && Math.abs(this.z) <= box.horizontalRadius();
            }

            private boolean isOnCurrentShell() {
                return Math.max(Math.max(Math.abs(this.x), Math.abs(this.y)), Math.abs(this.z)) == this.radius;
            }
        };
    }

    @FunctionalInterface
    private interface PositionOrder {

        Iterable<BlockPos.MutableBlockPos> positions(@Nonnull BlockPos center, @Nonnull BlockScanBox box);

    }

}
