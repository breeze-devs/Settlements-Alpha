package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRecordPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.structures.SettlementStructure;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class GeneratedSettlementRecordServerEvents {

    private final GeneratedSettlementRecorder recorder;

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        // A generated settlement's structure start is its durable copy until its save-data record exists, so every
        // load of a start chunk offers its settlement for recording: a record never stored or was lost is rebuilt.
        // The event can fire before the chunk is full, so this reads only the chunk's own starts and the level's
        // saved data; loading another chunk from here could deadlock chunk loading.
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        for (Map.Entry<Structure, StructureStart> start : event.getChunk().getAllStarts().entrySet()) {
            if (!(start.getKey() instanceof SettlementStructure)) {
                continue;
            }
            for (StructurePiece piece : start.getValue().getPieces()) {
                if (piece instanceof SettlementRecordPiece recordPiece) {
                    this.recorder.record(level, recordPiece.getSettlement());
                }
            }
        }
    }

}
