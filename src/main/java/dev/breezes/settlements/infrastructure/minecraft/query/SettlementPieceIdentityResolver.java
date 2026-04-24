package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.levelgen.structure.StructurePiece;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class SettlementPieceIdentityResolver {

    public boolean isBuildingPiece(@Nonnull StructurePiece piece) {
        return piece instanceof SettlementBuildingPiece;
    }

    @Nullable
    public String settlementId(@Nonnull StructurePiece piece) {
        if (piece instanceof SettlementBuildingPiece buildingPiece) {
            return buildingPiece.getSettlementId();
        }
        if (piece instanceof SettlementRoadPiece roadPiece) {
            return roadPiece.getSettlementId();
        }
        return null;
    }

    @Nullable
    public String buildingDefinitionId(@Nonnull StructurePiece piece) {
        if (piece instanceof SettlementBuildingPiece buildingPiece) {
            return buildingPiece.getBuildingDefinitionId();
        }
        return null;
    }

}
