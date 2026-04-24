package dev.breezes.settlements.infrastructure.minecraft.worldgen.structures;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.di.ServerComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.generation.building.TemplateResolutionContext;
import dev.breezes.settlements.domain.generation.building.TemplateResolver;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.ResolvedTemplate;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.BlockPositionConverter;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.SettlementsStructure;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.TerrainGridFactory;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import dev.breezes.settlements.shared.util.CoordinateHashUtil;
import lombok.CustomLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@CustomLog
public class SettlementStructure extends SettlementsStructure {

    public static final MapCodec<SettlementStructure> CODEC = simpleCodec(SettlementStructure::new);

    public SettlementStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return StructureRegistry.SETTLEMENT_STRUCTURE_TYPE.get();
    }

    @Override
    protected void generatePieces(StructurePiecesBuilder builder, GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        long seed = context.seed() ^ CoordinateHashUtil.hash(chunkPos.x, chunkPos.z);
        String settlementId = settlementIdFor(chunkPos);

        SurveyBounds bounds = buildSurveyBounds(chunkPos);
        TerrainGrid terrainGrid = TerrainGridFactory.fromGenerationContext(context, bounds, 4);
        GenerationPipeline pipeline = SettlementsDagger.component().generationPipeline();

        GenerationResult result = pipeline.generate(terrainGrid, bounds, seed);

        TemplateResolver templateResolver = SettlementsDagger.component().nbtTemplateResolver();
        Random random = new Random(seed);
        Set<String> biomeTags = pipeline.resolveTemplateTags(result.siteReport());
        TemplateResolutionContext resolutionContext = new TemplateResolutionContext(biomeTags, result.history().visualMarkers());

        log.info("Settlement template resolution context: biomeTags={}, visualMarkers={}",
                resolutionContext.biomeTags(), resolutionContext.visualMarkers().markers());

        for (BuildingAssignment assignment : result.layout().assignments()) {
            Optional<ResolvedTemplate> resolvedTemplate = templateResolver.resolve(assignment.building(), random, resolutionContext);
            if (resolvedTemplate.isEmpty()) {
                log.error("Could not resolve template for building {} with footprint {}",
                        assignment.building().id(), assignment.building().footprint());
                continue;
            }

            log.info("Resolved template for {}: {}", assignment.building().id(), resolvedTemplate.get());
            ResourceLocation templateId = ResourceLocation.parse(resolvedTemplate.get().templatePath());
            var plotAnchor = BlockPositionConverter.toMinecraft(assignment.plot().bounds().min().withY(assignment.plot().targetY()));
            builder.addPiece(new SettlementBuildingPiece(
                    context.structureTemplateManager(),
                    templateId,
                    plotAnchor,
                    SettlementBuildingPiece.fromDirection(assignment.facing()),
                    settlementId,
                    assignment.building().id()
            ));
        }

        for (RoadSegment road : result.layout().roads()) {
            builder.addPiece(new SettlementRoadPiece(
                    BlockPositionConverter.toMinecraft(road.start()),
                    BlockPositionConverter.toMinecraft(road.end()),
                    road.type(),
                    result.profile().wealthLevel(),
                    settlementId
            ));
        }

        enqueueSettlementMetadata(result, settlementId);
    }

    private static void enqueueSettlementMetadata(@Nonnull GenerationResult result,
                                                  @Nonnull String settlementId) {
        ServerComponent serverComponent = SettlementsDagger.serverOrNull();
        if (serverComponent == null) {
            return;
        }

        BlockPosition planningCenter = result.layout().planningCenter();
        BoundingRegion buildArea = result.siteReport().bounds().buildArea();
        SettlementMetadata metadata = SettlementMetadata.builder()
                .settlementId(settlementId)
                .name("Settlement #" + settlementId) // TODO: settlement name generation
                .primaryTrait(result.profile().primary().full())
                .scaleTier(result.profile().scaleTier().name())
                .centerX(planningCenter.x())
                .centerZ(planningCenter.z())
                .boundsMinX(buildArea.min().x())
                .boundsMinZ(buildArea.min().z())
                .boundsMaxX(buildArea.max().x())
                .boundsMaxZ(buildArea.max().z())
                .estimatedPopulation(result.profile().estimatedPopulation())
                .wealthLevel(result.profile().wealthLevel())
                .build();
        serverComponent.settlementMetadataQueueService().enqueue(metadata);
    }

    private static String settlementIdFor(ChunkPos chunkPos) {
        // The start chunk is the stable identity anchor for a generated settlement.
        // Encoding the pair into one long keeps the persisted ID compact and deterministic.
        long packedChunkPos = ((long) chunkPos.x & 0xFFFFFFFFL) << 32 | ((long) chunkPos.z & 0xFFFFFFFFL);
        return Long.toUnsignedString(packedChunkPos);
    }

    private static SurveyBounds buildSurveyBounds(ChunkPos chunkPos) {
        int radius = ScaleTier.VILLAGE.areaRadius();
        BlockPosition center = new BlockPosition(chunkPos.getMiddleBlockX(), 0, chunkPos.getMiddleBlockZ());
        return SurveyBounds.fromBuildArea(new BoundingRegion(
                center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius)
        ), 30);
    }

}
