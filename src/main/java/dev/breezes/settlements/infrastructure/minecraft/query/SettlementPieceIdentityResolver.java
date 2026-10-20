package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRecordPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class SettlementPieceIdentityResolver {

    public boolean isBuildingPiece(@Nonnull StructurePiece piece) {
        return piece instanceof SettlementBuildingPiece;
    }

    /**
     * Returns the id of the settlement a piece belongs to, or null when the piece is not a settlement piece or carries
     * no settlement id.
     */
    @Nullable
    public UUID settlementId(@Nonnull StructurePiece piece) {
        return switch (piece) {
            case SettlementBuildingPiece buildingPiece -> buildingPiece.getSettlementId();
            case SettlementRoadPiece roadPiece -> roadPiece.getSettlementId();
            case SettlementRecordPiece recordPiece -> recordPiece.getSettlement().id();
            default -> null;
        };
    }

    @Nullable
    public String buildingDefinitionId(@Nonnull StructurePiece piece) {
        if (piece instanceof SettlementBuildingPiece buildingPiece) {
            return buildingPiece.getBuildingDefinitionId();
        }
        return null;
    }

}
