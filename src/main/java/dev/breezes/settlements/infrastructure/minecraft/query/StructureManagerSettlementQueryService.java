package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import dev.breezes.settlements.domain.settlement.query.SettlementContext;
import dev.breezes.settlements.domain.settlement.query.SettlementPositionContext;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.persistence.SettlementSavedData;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@CustomLog
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class StructureManagerSettlementQueryService implements SettlementQueryService {

    private final BuildingRegistry buildingRegistry;
    private final SettlementStructureLocator settlementStructureLocator;
    private final SettlementPieceIdentityResolver settlementPieceIdentityResolver;

    @Override
    public SettlementPositionContext getContextAt(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        Optional<StructureStart> structureStart = this.settlementStructureLocator.locate(level, position);
        if (structureStart.isEmpty()) {
            return SettlementPositionContext.empty();
        }

        List<StructurePiece> pieces = structureStart.get().getPieces();
        Optional<SettlementContext> settlementContext = extractSettlementId(pieces)
                .flatMap(settlementId -> findSettlementMetadata(level, settlementId)
                        .filter(metadata -> metadata.containsPosition(position.getX(), position.getZ()))
                        .map(SettlementContext::new));
        Optional<BuildingContext> buildingContext = findContainingBuildingPiece(pieces, position)
                .flatMap(piece -> toBuildingContext(level, piece, settlementContext));

        return new SettlementPositionContext(
                settlementContext.orElse(null),
                buildingContext.orElse(null)
        );
    }

    @Override
    public Optional<SettlementContext> getSettlementAt(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        return getContextAt(level, position).settlement();
    }

    @Override
    public Optional<SettlementContext> getSettlementById(@Nonnull ServerLevel level, @Nonnull String settlementId) {
        return toSettlementContext(level, settlementId);
    }

    @Override
    public Optional<BuildingContext> getBuildingAt(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        return getContextAt(level, position).building();
    }

    private Optional<SettlementContext> toSettlementContext(@Nonnull ServerLevel level, @Nonnull String settlementId) {
        return findSettlementMetadata(level, settlementId)
                .map(SettlementContext::new);
    }

    private Optional<SettlementMetadata> findSettlementMetadata(@Nonnull ServerLevel level, @Nonnull String settlementId) {
        return SettlementSavedData.get(level).getBySettlementId(settlementId);
    }

    private Optional<String> extractSettlementId(@Nonnull List<StructurePiece> pieces) {
        return pieces.stream()
                .map(this.settlementPieceIdentityResolver::settlementId)
                .filter(settlementId -> settlementId != null && !settlementId.isBlank())
                .findFirst();
    }

    private Optional<StructurePiece> findContainingBuildingPiece(@Nonnull List<StructurePiece> pieces,
                                                                 @Nonnull BlockPos position) {
        return pieces.stream()
                .filter(this.settlementPieceIdentityResolver::isBuildingPiece)
                .filter(piece -> piece.getBoundingBox().isInside(position))
                .filter(piece -> this.settlementPieceIdentityResolver.settlementId(piece) != null)
                .min(Comparator.comparingInt(piece -> boundingBoxVolume(piece.getBoundingBox())));
    }

    private Optional<BuildingContext> toBuildingContext(@Nonnull ServerLevel level,
                                                        @Nonnull StructurePiece buildingPiece,
                                                        @Nonnull Optional<SettlementContext> resolvedSettlementContext) {
        String settlementId = this.settlementPieceIdentityResolver.settlementId(buildingPiece);
        if (settlementId == null) {
            return Optional.empty();
        }

        String buildingDefinitionId = this.settlementPieceIdentityResolver.buildingDefinitionId(buildingPiece);
        if (buildingDefinitionId == null) {
            return Optional.empty();
        }

        Optional<SettlementContext> settlementContext = resolvedSettlementContext.isPresent()
                ? resolvedSettlementContext
                : toSettlementContext(level, settlementId);

        return settlementContext.map(context -> new BuildingContext(
                buildingDefinitionId,
                resolveBuildingDisplayName(buildingDefinitionId),
                context
        ));
    }

    private String resolveBuildingDisplayName(@Nonnull String buildingDefinitionId) {
        String displayName = this.buildingRegistry.displayNameFor(buildingDefinitionId);
        if (displayName.equals(buildingDefinitionId) && this.buildingRegistry.byId(buildingDefinitionId).isEmpty()) {
            log.warn("Missing building definition '{}'; using raw id as display name", buildingDefinitionId);
            return displayName;
        }

        return displayName;
    }

    private static int boundingBoxVolume(@Nonnull BoundingBox boundingBox) {
        // When pieces overlap we choose the smallest containing building because it is the most specific match.
        return (boundingBox.getXSpan() + 1) * (boundingBox.getYSpan() + 1) * (boundingBox.getZSpan() + 1);
    }

}
