package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingFootprint;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import dev.breezes.settlements.domain.generation.model.layout.ZoneTier;
import lombok.CustomLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

@CustomLog
public class RoadInfillPlacer extends BuildingPlacer {

    /**
     * Places buildings from {@code remainingBuildings} (highest-priority first) into plots
     * along both sides of every road segment.
     * <p>
     * For each road segment, buildings are walked outward from the start of the segment in
     * steps equal to the building footprint size plus spacing jitter. Each candidate center
     * is placed perpendicularly off the road edge at a lateral distance derived from the
     * road's half-width plus the building's own half-footprint, so buildings sit flush
     * against the road without overlapping it. Facing is set toward the road so the
     * building's entrance aligns with the street.
     * <p>
     * Roads are processed in priority order -- MAIN first, then SECONDARY, then SIDE -- so
     * high-priority buildings land on the most prominent streets. A building is consumed
     * from the queue only when it is successfully placed; otherwise it remains for the next
     * road or falls through to scatter.
     * <p>
     * Zone assignment uses the road-graph distance from the planning center to the placement point
     * (via {@link #computeRoadDistances}), not straight-line distance, so buildings tucked
     * behind a long winding road are correctly classified as further from the center.
     */
    @Override
    public PlacementPhaseResult executePlacement(PlacementContext context) {
        Queue<BuildingDefinition> buildingQueue = new ArrayDeque<>(context.getBuildings().stream()
                .sorted(Comparator.comparingInt(BuildingDefinition::placementPriority).reversed())
                .toList());
        List<BuildingAssignment> assignments = new ArrayList<>();
        int nextPlotId = context.getStartingPlotId();

        Map<XZKey, Double> roadDistances = computeRoadDistances(context.getPlanningCenter(), context.getRoads());
        List<RoadSegment> orderedRoads = context.getRoads().stream()
                .sorted(Comparator.comparingInt(segment -> roadOrder(segment.type())))
                .toList();

        for (RoadSegment road : orderedRoads) {
            double roadLength = Math.max(1.0d, LayoutSupport.segmentLengthXZ(road.start(), road.end()));
            double tangentX = (road.end().x() - road.start().x()) / roadLength;
            double tangentZ = (road.end().z() - road.start().z()) / roadLength;
            double perpendicularX = -tangentZ;
            double perpendicularZ = tangentX;

            for (int side : List.of(1, -1)) {
                double usedRoadLength = 0.0d;

                while (!buildingQueue.isEmpty() && usedRoadLength <= roadLength) {
                    BuildingDefinition building = buildingQueue.peek();
                    BuildingFootprint footprintShape = building.footprint();

                    double ratio = usedRoadLength / roadLength;
                    BlockPosition roadPoint = LayoutSupport.interpolateOnSegment(road.start(), road.end(), ratio, context.terrainGrid());
                    int maxBuildingRadius = Math.max(footprintShape.width(), footprintShape.depth());
                    double lateralDistance = (LayoutSupport.roadWidth(road.type()) / 2.0d) + 1.0d + (maxBuildingRadius / 2.0d);
                    int candidateX = (int) Math.round(roadPoint.x() + perpendicularX * lateralDistance * side);
                    int candidateZ = (int) Math.round(roadPoint.z() + perpendicularZ * lateralDistance * side);

                    BlockPosition center = new BlockPosition(candidateX, context.terrainGrid().getHeightAtWorld(candidateX, candidateZ), candidateZ);
                    Direction facing = LayoutSupport.directionToward(center, roadPoint);
                    LayoutSupport.CandidateFootprint footprint = LayoutSupport.evaluateFootprint(context.terrainGrid(), center, facing, footprintShape);
                    PlacementResult result = context.getValidator().evaluate(building, footprint);

                    if (result.valid()) {
                        double distanceAlongRoad = computePlotRoadDistance(roadDistances, road, ratio);
                        ZoneTier zone = LayoutSupport.zoneForDistance(context.scaleTier(), distanceAlongRoad);
                        assignments.add(this.createAssignment(
                                nextPlotId++,
                                building,
                                null,
                                footprint,
                                facing,
                                zone,
                                true,
                                result.localResources()
                        ));
                        context.getGrid().occupy(footprint.bounds());
                        buildingQueue.poll();
                        log.worldgenStatus("RoadInfill: placed {} at ({},{},{}) zone={}",
                                building.id(), center.x(), center.y(), center.z(), zone);
                        // Placement succeeded: advance by the full building footprint + spacing so
                        // the next candidate starts just past this building.
                        usedRoadLength += Math.max(4, maxBuildingRadius + context.getGrid().minimumSpacing() + context.getRandom().nextInt(-2, 4));
                    } else {
                        // Placement failed (cliff, conflict, out of bounds): advance by a small
                        // increment so we don't waste the road frontage right past the obstacle.
                        usedRoadLength += 2;
                    }
                }

                if (!buildingQueue.isEmpty() && usedRoadLength > roadLength) {
                    log.worldgenWarn("RoadInfill: exhausted road {} side={} with {} buildings still queued",
                            road.type(), side, buildingQueue.size());
                }
            }
        }

        return new PlacementPhaseResult(assignments, List.copyOf(buildingQueue));
    }

    /**
     * Computes the shortest road-graph distance (in blocks) from {@code planningCenter} to every
     * road segment endpoint using Dijkstra's algorithm.
     * <p>
     * Each road segment contributes two directed edges (start ↔ end) with weight equal to the
     * segment's Euclidean XZ length.
     * <p>
     * The map is keyed by XZ coordinates only (Y is ignored) to avoid misses when road
     * endpoints share the same XZ position but differ in Y due to terrain sampling.
     */
    static Map<XZKey, Double> computeRoadDistances(BlockPosition planningCenter, List<RoadSegment> roads) {
        Map<XZKey, List<RoadEdge>> adjacency = new HashMap<>();
        for (RoadSegment road : roads) {
            double length = LayoutSupport.segmentLengthXZ(road.start(), road.end());
            adjacency.computeIfAbsent(XZKey.of(road.start()), ignored -> new ArrayList<>()).add(new RoadEdge(road.end(), length));
            adjacency.computeIfAbsent(XZKey.of(road.end()), ignored -> new ArrayList<>()).add(new RoadEdge(road.start(), length));
        }
        Map<XZKey, Double> distances = new HashMap<>();
        PriorityQueue<RoadVisit> queue = new PriorityQueue<>(Comparator.comparingDouble(RoadVisit::distance));
        XZKey planningCenterKey = XZKey.of(planningCenter);
        distances.put(planningCenterKey, 0.0d);
        queue.add(new RoadVisit(planningCenterKey, 0.0d));
        while (!queue.isEmpty()) {
            RoadVisit current = queue.poll();
            if (current.distance() > distances.getOrDefault(current.node(), Double.POSITIVE_INFINITY)) {
                continue;
            }
            for (RoadEdge edge : adjacency.getOrDefault(current.node(), List.of())) {
                double nextDistance = current.distance() + edge.length();
                XZKey edgeKey = XZKey.of(edge.node());
                if (nextDistance < distances.getOrDefault(edgeKey, Double.POSITIVE_INFINITY)) {
                    distances.put(edgeKey, nextDistance);
                    queue.add(new RoadVisit(edgeKey, nextDistance));
                }
            }
        }
        return distances;
    }

    private static double computePlotRoadDistance(Map<XZKey, Double> roadDistances,
                                                  RoadSegment road,
                                                  double ratio) {
        double length = LayoutSupport.segmentLengthXZ(road.start(), road.end());
        double fromStart = roadDistances.getOrDefault(XZKey.of(road.start()), Double.POSITIVE_INFINITY) + (length * ratio);
        double fromEnd = roadDistances.getOrDefault(XZKey.of(road.end()), Double.POSITIVE_INFINITY) + (length * (1.0d - ratio));
        return Math.min(fromStart, fromEnd);
    }

    private static int roadOrder(RoadType type) {
        return switch (type) {
            case MAIN -> 0;
            case SECONDARY -> 1;
            case SIDE -> 2;
        };
    }

    private record RoadEdge(BlockPosition node, double length) {
    }

    private record RoadVisit(XZKey node, double distance) {
    }

    private record XZKey(int x, int z) {
        static XZKey of(BlockPosition p) {
            return new XZKey(p.x(), p.z());
        }
    }

}
