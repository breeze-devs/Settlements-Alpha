package dev.breezes.settlements.infrastructure.minecraft.worldgen.structures;

import com.mojang.serialization.MapCodec;
import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder.GeneratedSettlement;
import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.generation.building.TemplateResolutionContext;
import dev.breezes.settlements.domain.generation.building.TemplateResolver;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.building.BuildingAssignment;
import dev.breezes.settlements.domain.generation.model.building.ResolvedTemplate;
import dev.breezes.settlements.domain.generation.model.layout.RoadSegment;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.BlockPositionConverter;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.SettlementsStructure;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.TerrainGridFactory;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRecordPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import dev.breezes.settlements.shared.util.CoordinateHashUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@CustomLog(topic = "worldgen")
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
        UUID settlementId = settlementIdFor(context.seed(), chunkPos);

        SurveyBounds bounds = SurveyBounds.aroundSettlementAt(chunkPos.getMiddleBlockX(), chunkPos.getMiddleBlockZ());
        TerrainGrid terrainGrid = TerrainGridFactory.fromGenerationContext(context, bounds);
        GenerationPipeline pipeline = SettlementsDagger.component().generationPipeline();

        GenerationResult result = pipeline.generate(terrainGrid, bounds, seed);

        TemplateResolver templateResolver = SettlementsDagger.component().nbtTemplateResolver();
        Random random = new Random(seed);
        Set<String> biomeTags = pipeline.resolveTemplateTags(result.siteReport());
        TemplateResolutionContext resolutionContext = new TemplateResolutionContext(biomeTags, result.history().visualMarkers());

        log.debug("Settlement template resolution context: biomeTags={}", resolutionContext.biomeTags());

        for (BuildingAssignment assignment : result.layout().assignments()) {
            Optional<ResolvedTemplate> resolvedTemplate = templateResolver.resolve(assignment.building(), random, resolutionContext);
            if (resolvedTemplate.isEmpty()) {
                log.debug("Could not resolve template for building {} with footprint {}",
                        assignment.building().id(), assignment.building().footprint());
                continue;
            }

            ResourceLocation templateId = ResourceLocation.parse(resolvedTemplate.get().templatePath());
            BlockPos plotAnchor = BlockPositionConverter.toMinecraft(assignment.plot().bounds().min().withY(assignment.plot().targetY()));
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

        if (builder.isEmpty()) {
            // Nothing was placed, so there is no settlement to record, and an empty builder yields no structure start
            log.warn("Generated settlement {} placed no buildings or roads; skipping it", settlementId);
            return;
        }

        BlockPos center = BlockPositionConverter.toMinecraft(result.layout().planningCenter());
        // Added last: a structure start takes its placement reference from its first piece
        builder.addPiece(new SettlementRecordPiece(GeneratedSettlement.builder()
                .id(settlementId)
                .bounds(boundsOf(center, builder.getBoundingBox()))
                .profile(result.profile())
                .history(result.history().events())
                .build(), center));
    }

    private static SettlementBounds boundsOf(@Nonnull BlockPos center, @Nonnull BoundingBox generated) {
        // The claim covers every chunk under the generated pieces, so no building or road stands outside its own
        // settlement. A rectangle widened to take in the center chunk stays one region.
        ChunkPos centerChunk = new ChunkPos(center);
        int minChunkX = Math.min(SectionPos.blockToSectionCoord(generated.minX()), centerChunk.x);
        int minChunkZ = Math.min(SectionPos.blockToSectionCoord(generated.minZ()), centerChunk.z);
        int maxChunkX = Math.max(SectionPos.blockToSectionCoord(generated.maxX()), centerChunk.x);
        int maxChunkZ = Math.max(SectionPos.blockToSectionCoord(generated.maxZ()), centerChunk.z);

        Set<ChunkPosition> chunks = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPosition(chunkX, chunkZ));
            }
        }

        return new SettlementBounds(new ChunkPosition(centerChunk.x, centerChunk.z), chunks);
    }

    private static UUID settlementIdFor(long levelSeed, @Nonnull ChunkPos chunkPos) {
        // Derived from the start chunk rather than random, so generating the same start again yields the same id
        byte[] identity = ByteBuffer.allocate(Long.BYTES + 2 * Integer.BYTES)
                .putLong(levelSeed)
                .putInt(chunkPos.x)
                .putInt(chunkPos.z)
                .array();
        return UUID.nameUUIDFromBytes(identity);
    }

}
