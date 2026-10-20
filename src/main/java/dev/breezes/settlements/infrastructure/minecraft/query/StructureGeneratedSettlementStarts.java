package dev.breezes.settlements.infrastructure.minecraft.query;

import dev.breezes.settlements.application.settlement.founding.GeneratedSettlementStarts;
import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder.GeneratedSettlement;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRecordPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.structures.SettlementStructure;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Reads generated settlements from the record pieces of the settlement structure starts a chunk references.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class StructureGeneratedSettlementStarts implements GeneratedSettlementStarts {

    @Override
    public List<GeneratedSettlement> covering(@Nonnull ServerLevel level, @Nonnull ChunkPosition chunk) {
        return level.structureManager()
                .startsForStructure(new ChunkPos(chunk.x(), chunk.z()), SettlementStructure.class::isInstance)
                .stream()
                .flatMap(start -> start.getPieces().stream())
                .filter(SettlementRecordPiece.class::isInstance)
                .map(piece -> ((SettlementRecordPiece) piece).getSettlement())
                .toList();
    }

}
