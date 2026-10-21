package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.query.BuildingContext;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.world.ChunkPosition;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds settlements and buildings by the claimed chunks in the settlement store.
 */
@CustomLog(topic = "settlement")
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class StoreBackedSettlementQueryService implements SettlementQueryService {

    private final SettlementStore settlementStore;
    private final BuildingRegistry buildingRegistry;
    private final SettlementStructureLocator settlementStructureLocator;
    private final SettlementPieceIdentityResolver settlementPieceIdentityResolver;

    @Override
    public Optional<Settlement> getSettlementAt(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        ChunkPosition chunk = new ChunkPosition(SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ()));
        return this.settlementStore.findClaiming(level, chunk);
    }

    @Override
    public Optional<BuildingContext> getBuildingAt(@Nonnull ServerLevel level, @Nonnull BlockPos position) {
        Optional<Settlement> settlement = this.getSettlementAt(level, position)
                .filter(claiming -> claiming.origin() == SettlementOrigin.GENERATED);
        if (settlement.isEmpty()) {
            return Optional.empty();
        }

        UUID settlementId = settlement.get().id();
        return this.settlementStructureLocator.locate(level, position)
                .flatMap(start -> start.getPieces().stream()
                        .filter(this.settlementPieceIdentityResolver::isBuildingPiece)
                        // A claim clipped against a neighbor's leaves some of its structure's pieces in chunks the
                        // neighbor claims, so a piece counts only for the settlement claiming the position
                        .filter(piece -> settlementId.equals(this.settlementPieceIdentityResolver.settlementId(piece)))
                        .filter(piece -> piece.getBoundingBox().isInside(position))
                        // Where pieces overlap, the smallest is the most specific match
                        .min(Comparator.comparingInt(piece -> boundingBoxVolume(piece.getBoundingBox()))))
                .flatMap(this::buildingDefinitionId)
                .map(definitionId -> new BuildingContext(definitionId, this.displayName(definitionId),
                        settlement.get()));
    }

    private Optional<String> buildingDefinitionId(@Nonnull StructurePiece buildingPiece) {
        return Optional.ofNullable(this.settlementPieceIdentityResolver.buildingDefinitionId(buildingPiece));
    }

    private String displayName(@Nonnull String buildingDefinitionId) {
        if (this.buildingRegistry.byId(buildingDefinitionId).isEmpty()) {
            log.error("Missing building definition '{}'; using raw id as display name", buildingDefinitionId);
        }
        return this.buildingRegistry.displayNameFor(buildingDefinitionId);
    }

    private static int boundingBoxVolume(@Nonnull BoundingBox boundingBox) {
        return boundingBox.getXSpan() * boundingBox.getYSpan() * boundingBox.getZSpan();
    }

}
