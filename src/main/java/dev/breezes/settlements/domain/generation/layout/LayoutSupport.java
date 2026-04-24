package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class LayoutSupport {

    static final int FLATNESS_THRESHOLD = 5;

    /**
     * Samples terrain heights across the full XZ footprint centred on {@code center} and
     * oriented by {@code facing}, then derives placement metadata:
     * <ul>
     *   <li><b>bounds</b> — the axis-aligned bounding box of all sampled blocks (min/max Y
     *       spanning the actual terrain, not a flat slab)</li>
     *   <li><b>targetY</b> — median surface height across all sampled columns; used as the
     *       floor level when the building is placed, so it sits at the "middle ground" rather
     *       than sinking into hills or floating over valleys</li>
     *   <li><b>elevationDelta</b> — {@code maxHeight - minHeight} across the footprint;
     *       callers compare this against {@link #FLATNESS_THRESHOLD} to reject placements
     *       on terrain too uneven to build on</li>
     * </ul>
     * Facing controls which footprint dimension maps to X vs Z: NORTH/SOUTH buildings use
     * {@code width} on X and {@code depth} on Z; EAST/WEST buildings swap the axes.
     */
    public static CandidateFootprint evaluateFootprint(TerrainGrid terrainGrid,
                                                       BlockPosition center,
                                                       Direction facing,
                                                       BuildingFootprint footprint) {
        int widthX = usesWidthOnX(facing) ? footprint.width() : footprint.depth();
        int depthZ = usesWidthOnX(facing) ? footprint.depth() : footprint.width();
        int minX = center.x() - ((widthX - 1) / 2);
        int maxX = minX + widthX - 1;
        int minZ = center.z() - ((depthZ - 1) / 2);
        int maxZ = minZ + depthZ - 1;

        List<Integer> heights = new ArrayList<>(widthX * depthZ);
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                int height = terrainGrid.getHeightAtWorld(x, z);
                heights.add(height);
                minHeight = Math.min(minHeight, height);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        Collections.sort(heights);
        int targetY = heights.get(heights.size() / 2);
        BoundingRegion bounds = new BoundingRegion(
                new BlockPosition(minX, minHeight, minZ),
                new BlockPosition(maxX, maxHeight, maxZ)
        );
        return new CandidateFootprint(bounds, targetY, maxHeight - minHeight, footprint, facing, center);
    }

    public static boolean isWithinBuildAreaXZ(BoundingRegion buildArea, BoundingRegion candidate) {
        return candidate.min().x() >= buildArea.min().x()
                && candidate.max().x() <= buildArea.max().x()
                && candidate.min().z() >= buildArea.min().z()
                && candidate.max().z() <= buildArea.max().z();
    }

    public static long distanceSquaredXZ(BlockPosition a, BlockPosition b) {
        long dx = (long) a.x() - b.x();
        long dz = (long) a.z() - b.z();
        return dx * dx + dz * dz;
    }

    public static Direction directionToward(BlockPosition from, BlockPosition to) {
        int dx = to.x() - from.x();
        int dz = to.z() - from.z();
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public static Direction directionFromVector(double dx, double dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0.0d ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0.0d ? Direction.SOUTH : Direction.NORTH;
    }

    public static double segmentLengthXZ(BlockPosition a, BlockPosition b) {
        long dx = (long) b.x() - a.x();
        long dz = (long) b.z() - a.z();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static BlockPosition interpolateOnSegment(BlockPosition start, BlockPosition end, double ratio, TerrainGrid terrainGrid) {
        double clamped = Math.clamp(ratio, 0.0d, 1.0d);
        int x = (int) Math.round(start.x() + (end.x() - start.x()) * clamped);
        int z = (int) Math.round(start.z() + (end.z() - start.z()) * clamped);
        int y = terrainGrid.getHeightAtWorld(x, z);
        return new BlockPosition(x, y, z);
    }

    /**
     * Maps a Euclidean XZ distance from the settlement planning center to a {@link ZoneTier} using
     * per-{@link ScaleTier} radius thresholds.
     * <p>
     * Thresholds (blocks from the planning center):
     * <pre>
     *             CORE   DOWNTOWN  MIDTOWN  OUTER   SUBURB
     * HAMLET      ≤ 5      ≤ 15     ≤ 25    ≤ 32    > 32
     * VILLAGE     ≤ 8      ≤ 25     ≤ 45    ≤ 56    > 56
     * TOWN        ≤ 10     ≤ 30     ≤ 55    ≤ 80    > 80
     * </pre>
     */
    public static ZoneTier zoneForDistance(ScaleTier scaleTier, double distance) {
        return switch (scaleTier) {
            case HAMLET -> {
                if (distance <= 5.0d) {
                    yield ZoneTier.CORE;
                }
                if (distance <= 15.0d) {
                    yield ZoneTier.DOWNTOWN;
                }
                if (distance <= 25.0d) {
                    yield ZoneTier.MIDTOWN;
                }
                if (distance <= 32.0d) {
                    yield ZoneTier.OUTER;
                }
                yield ZoneTier.SUBURB;
            }
            case VILLAGE -> {
                if (distance <= 8.0d) {
                    yield ZoneTier.CORE;
                }
                if (distance <= 25.0d) {
                    yield ZoneTier.DOWNTOWN;
                }
                if (distance <= 45.0d) {
                    yield ZoneTier.MIDTOWN;
                }
                if (distance <= 56.0d) {
                    yield ZoneTier.OUTER;
                }
                yield ZoneTier.SUBURB;
            }
            case TOWN -> {
                if (distance <= 10.0d) {
                    yield ZoneTier.CORE;
                }
                if (distance <= 30.0d) {
                    yield ZoneTier.DOWNTOWN;
                }
                if (distance <= 55.0d) {
                    yield ZoneTier.MIDTOWN;
                }
                if (distance <= 80.0d) {
                    yield ZoneTier.OUTER;
                }
                yield ZoneTier.SUBURB;
            }
        };
    }

    public static int roadWidth(RoadType roadType) {
        return switch (roadType) {
            case MAIN -> 5;
            case SECONDARY -> 3;
            case SIDE -> 2;
        };
    }

    /**
     * Returns a single AABB for a road segment expanded by {@code halfWidth} blocks on each
     * XZ side. Intended for proximity checks (e.g. "is a building near this road?"), not for
     * conflict registration in {@link PlacementGrid}. For conflict registration use
     * {@link #rasterizeRoad(RoadSegment)}.
     */
    public static BoundingRegion roadBounds(RoadSegment segment) {
        int halfWidth = Math.max(1, roadWidth(segment.type()) / 2);
        return new BoundingRegion(
                new BlockPosition(
                        Math.min(segment.start().x(), segment.end().x()) - halfWidth,
                        Math.min(segment.start().y(), segment.end().y()) - 1,
                        Math.min(segment.start().z(), segment.end().z()) - halfWidth
                ),
                new BlockPosition(
                        Math.max(segment.start().x(), segment.end().x()) + halfWidth,
                        Math.max(segment.start().y(), segment.end().y()) + 1,
                        Math.max(segment.start().z(), segment.end().z()) + halfWidth
                )
        );
    }

    /**
     * Rasterizes a road segment into a list of small AABBs that tightly approximate the
     * physical road corridor. Sample points are spaced {@code roadWidth} blocks apart along
     * the segment, and each sample produces a {@code (2·halfWidth) × (2·halfWidth)} AABB.
     * <p>
     * This avoids the inflated dead-zone that a single overarching AABB creates for diagonal
     * roads. A 30-block diagonal road that would otherwise occupy a 30×30 square is instead
     * represented as ~15 small squares of 2–4 blocks each, leaving the flanking space usable
     * for building placement.
     */
    public static List<BoundingRegion> rasterizeRoad(RoadSegment segment) {
        int halfWidth = Math.max(1, roadWidth(segment.type()) / 2);
        int minY = Math.min(segment.start().y(), segment.end().y()) - 1;
        int maxY = Math.max(segment.start().y(), segment.end().y()) + 1;
        double length = segmentLengthXZ(segment.start(), segment.end());

        if (length < 1.0d) {
            int x = segment.start().x();
            int z = segment.start().z();
            return List.of(new BoundingRegion(
                    new BlockPosition(x - halfWidth, minY, z - halfWidth),
                    new BlockPosition(x + halfWidth, maxY, z + halfWidth)
            ));
        }

        double stepSize = Math.max(1.0d, roadWidth(segment.type()));
        List<BoundingRegion> regions = new ArrayList<>();
        for (double t = 0.0d; t < length; t += stepSize) {
            double ratio = t / length;
            int x = (int) Math.round(segment.start().x() + (segment.end().x() - segment.start().x()) * ratio);
            int z = (int) Math.round(segment.start().z() + (segment.end().z() - segment.start().z()) * ratio);
            regions.add(new BoundingRegion(
                    new BlockPosition(x - halfWidth, minY, z - halfWidth),
                    new BlockPosition(x + halfWidth, maxY, z + halfWidth)
            ));
        }
        // Always include the endpoint to avoid gaps at the tail
        int ex = segment.end().x();
        int ez = segment.end().z();
        regions.add(new BoundingRegion(
                new BlockPosition(ex - halfWidth, minY, ez - halfWidth),
                new BlockPosition(ex + halfWidth, maxY, ez + halfWidth)
        ));
        return List.copyOf(regions);
    }

    public static boolean satisfiesResourceConstraints(BuildingDefinition building, Set<ResourceTag> localResources) {
        return hasRequiredResources(building, localResources) && hasNoForbiddenResources(building, localResources);
    }

    public static boolean hasRequiredResources(BuildingDefinition building, Set<ResourceTag> localResources) {
        return building.requiresResources().isEmpty()
                || building.requiresResources().stream().anyMatch(localResources::contains);
    }

    public static boolean hasNoForbiddenResources(BuildingDefinition building, Set<ResourceTag> localResources) {
        return building.forbiddenResources().stream().noneMatch(localResources::contains);
    }

    public static boolean allowsPartialWater(BuildingDefinition building) {
        return building.requiresResources().contains(ResourceTag.FRESHWATER)
                || building.requiresResources().contains(ResourceTag.COASTAL);
    }

    public static boolean isOnWater(TerrainGrid terrainGrid,
                                    LocalResourceScanner resourceScanner,
                                    CandidateFootprint footprint,
                                    boolean allowPartialWater) {
        BlockPosition center = footprint.center();
        if (resourceScanner.isWaterAt(terrainGrid, center.x(), center.z())) {
            return true;
        }
        if (allowPartialWater) {
            return false;
        }

        BoundingRegion bounds = footprint.bounds();
        return resourceScanner.isWaterAt(terrainGrid, bounds.min().x(), bounds.min().z())
                || resourceScanner.isWaterAt(terrainGrid, bounds.min().x(), bounds.max().z())
                || resourceScanner.isWaterAt(terrainGrid, bounds.max().x(), bounds.min().z())
                || resourceScanner.isWaterAt(terrainGrid, bounds.max().x(), bounds.max().z());
    }

    public static List<BuildingDefinition> sortedManifestBuildings(BuildingManifest manifest) {
        return manifest.buildings().stream()
                .sorted(Comparator.comparingInt(BuildingDefinition::placementPriority).reversed())
                .toList();
    }

    @Nullable
    public static TraitId dominantTrait(BuildingDefinition building, SettlementProfile profile) {
        return profile.allTraits().stream()
                .filter(trait -> building.traitAffinities().containsKey(trait))
                .max(Comparator.comparingDouble(trait -> building.traitAffinities().get(trait)))
                .orElse(null);
    }

    private static boolean usesWidthOnX(Direction facing) {
        return facing == Direction.NORTH || facing == Direction.SOUTH;
    }

    public record CandidateFootprint(
            BoundingRegion bounds,
            int targetY,
            int elevationDelta,
            BuildingFootprint footprint,
            Direction facing,
            BlockPosition center
    ) {
    }

}
