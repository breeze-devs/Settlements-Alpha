package dev.breezes.settlements.domain.generation.layout;

import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.survey.BiomeSurveyLookup;
import lombok.CustomLog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@CustomLog
public class SettlementLayoutEngine {

    private final HubPlacer hubPlacer;
    private final ConstrainedPlacer constrainedPlacer;
    private final RandomSearchPlacer signaturePlacer;
    private final RandomSearchPlacer uniformPlacer;
    private final RoadNetworkGenerator roadGenerator;
    private final RoadInfillPlacer infillPlacer;

    public SettlementLayoutEngine() {
        this(
                new HubPlacer(),
                new ConstrainedPlacer(),
                RandomSearchPlacer.radial(10, 25, 16),
                RandomSearchPlacer.uniform(20),
                new RoadNetworkGenerator(),
                new RoadInfillPlacer()
        );
    }

    public SettlementLayoutEngine(HubPlacer hubPlacer,
                                  ConstrainedPlacer constrainedPlacer,
                                  RandomSearchPlacer signaturePlacer,
                                  RandomSearchPlacer uniformPlacer,
                                  RoadNetworkGenerator roadGenerator,
                                  RoadInfillPlacer infillPlacer) {
        this.hubPlacer = hubPlacer;
        this.constrainedPlacer = constrainedPlacer;
        this.signaturePlacer = signaturePlacer;
        this.uniformPlacer = uniformPlacer;
        this.roadGenerator = roadGenerator;
        this.infillPlacer = infillPlacer;
    }

    public LayoutResult generateLayout(SiteReport report,
                                       SettlementProfile profile,
                                       BuildingManifest manifest,
                                       BiomeSurveyLookup biomeLookup) {
        Random random = new Random(profile.seed());
        LocalResourceScanner resourceScanner = new LocalResourceScanner(biomeLookup);
        PlacementGrid grid = new PlacementGrid(PlacementGrid.DEFAULT_MINIMUM_SPACING);
        BlockPosition anchorXZ = report.bounds().buildArea().centerXZ();
        BlockPosition anchor = anchorXZ.withY(report.terrainGrid().getHeightAtWorld(anchorXZ.x(), anchorXZ.z()));
        PlacementValidator validator = new PlacementValidator(report.bounds().buildArea(), grid, report.terrainGrid(), resourceScanner);

        PlacementContext context = PlacementContext.builder()
                .report(report)
                .profile(profile)
                .resourceScanner(resourceScanner)
                .grid(grid)
                .validator(validator)
                .anchor(anchor)
                .planningCenter(anchor)
                .random(random)
                .buildings(List.of())
                .roads(List.of())
                .startingPlotId(0)
                .build();

        List<BuildingAssignment> allAssignments = new ArrayList<>();
        List<BuildingDefinition> pool = new ArrayList<>(LayoutSupport.sortedManifestBuildings(manifest));

        BuildingDefinition hub = pool.stream()
                .filter(building -> "settlements:building_definitions/town_hall".equals(building.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Manifest must contain settlements:building_definitions/town_hall"));
        pool.remove(hub);
        context.setBuildings(List.of(hub));
        PlacementPhaseResult hubResult = this.hubPlacer.executePlacement(context);
        allAssignments.addAll(hubResult.placed());
        BuildingAssignment hubAssignment = hubResult.placed().getFirst();
        BlockPosition planningCenter = hubAssignment.plot().bounds().centerXZ().withY(hubAssignment.plot().targetY());
        context.setPlanningCenter(planningCenter);
        context.setStartingPlotId(allAssignments.size());
        log.worldgenStatus("Phase A.1 Hub: {} placed", hubResult.placed().size());
        log.worldgenStatus("Planning center set to hub at ({},{},{})", planningCenter.x(), planningCenter.y(), planningCenter.z());

        List<BuildingDefinition> constrained = pool.stream()
                .filter(building -> !building.requiresResources().isEmpty())
                .toList();
        pool.removeAll(constrained);
        context.setBuildings(constrained);
        PlacementPhaseResult constrainedResult = this.constrainedPlacer.executePlacement(context);
        allAssignments.addAll(constrainedResult.placed());
        pool.addAll(constrainedResult.remaining());
        context.setStartingPlotId(allAssignments.size());
        log.worldgenStatus("Phase A.2 Constrained: {} placed, {} demoted",
                constrainedResult.placed().size(), constrainedResult.remaining().size());

        Set<TraitId> satisfiedTraits = constrainedResult.placed().stream()
                .map(assignment -> LayoutSupport.dominantTrait(assignment.building(), profile))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<BuildingDefinition> signatureBuildings = new ArrayList<>();
        Map<String, TraitId> traitOverrides = new HashMap<>();
        for (TraitId trait : profile.allTraits()) {
            if (satisfiedTraits.contains(trait)) {
                continue;
            }
            BuildingDefinition signature = pool.stream()
                    .filter(building -> building.requiresResources().isEmpty())
                    .filter(building -> building.traitAffinities().containsKey(trait))
                    .max(Comparator.comparingDouble(building -> building.traitAffinities().get(trait) * building.placementPriority()))
                    .orElse(null);
            if (signature != null) {
                signatureBuildings.add(signature);
                traitOverrides.put(signature.id(), trait);
                pool.remove(signature);
            }
        }

        context.setBuildings(signatureBuildings);
        context.setTraitSourceOverrides(Map.copyOf(traitOverrides));
        PlacementPhaseResult signatureResult = this.signaturePlacer.executePlacement(context);
        allAssignments.addAll(signatureResult.placed());
        pool.addAll(signatureResult.remaining());
        context.setStartingPlotId(allAssignments.size());
        context.setTraitSourceOverrides(Map.of());
        log.worldgenStatus("Phase A.3 Signature: {} placed", signatureResult.placed().size());

        int bonusCount = Math.min(
                random.nextInt(0, 3),
                (int) pool.stream().filter(building -> building.requiresResources().isEmpty()).count()
        );
        List<BuildingDefinition> bonusBuildings = pool.stream()
                .filter(building -> building.requiresResources().isEmpty())
                .sorted(Comparator.comparingInt(BuildingDefinition::placementPriority).reversed())
                .limit(bonusCount)
                .toList();
        pool.removeAll(bonusBuildings);
        context.setBuildings(bonusBuildings);
        PlacementPhaseResult bonusResult = this.uniformPlacer.executePlacement(context);
        allAssignments.addAll(bonusResult.placed());
        pool.addAll(bonusResult.remaining());
        context.setStartingPlotId(allAssignments.size());
        log.worldgenStatus("Phase A.4 Bonus: {} placed", bonusResult.placed().size());

        List<RoadSegment> roads = this.roadGenerator.generateRoads(context.getPlanningCenter(), allAssignments, report.terrainGrid(), grid, random);
        context.setRoads(roads);
        log.worldgenStatus("Phase B Roads: {} segments", roads.size());

        context.setBuildings(pool);
        PlacementPhaseResult infillResult = this.infillPlacer.executePlacement(context);
        allAssignments.addAll(infillResult.placed());
        context.setStartingPlotId(allAssignments.size());
        log.worldgenStatus("Phase C Infill: {} placed, {} remaining",
                infillResult.placed().size(), infillResult.remaining().size());

        context.setBuildings(infillResult.remaining());
        PlacementPhaseResult scatterResult = this.uniformPlacer.executePlacement(context);
        allAssignments.addAll(scatterResult.placed());
        log.worldgenStatus("Phase D Scatter: {} placed, {} dropped",
                scatterResult.placed().size(), scatterResult.remaining().size());
        log.worldgenStatus("Layout complete: {} buildings, {} roads", allAssignments.size(), roads.size());

        return new LayoutResult(anchor, context.getPlanningCenter(), roads, allAssignments);
    }

}
