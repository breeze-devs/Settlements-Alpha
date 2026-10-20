package dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder.GeneratedSettlement;
import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.domain.generation.history.DrawnHistoryEvent;
import dev.breezes.settlements.infrastructure.minecraft.persistence.SettlementSavedDataCodec;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import javax.annotation.Nonnull;

/**
 * A structure piece that places nothing but carries the settlement metadata its structure generated, so the settlement survives
 * in its structure start, saved with the start chunk, until its record exists.
 */
@Getter
public class SettlementRecordPiece extends StructurePiece {

    private static final String SETTLEMENT_TAG = "Settlement";

    private static final Codec<DrawnHistoryEvent> HISTORY_EVENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("event_id").forGetter(DrawnHistoryEvent::eventId),
            Codec.INT.fieldOf("time_horizon").forGetter(DrawnHistoryEvent::timeHorizon)
    ).apply(instance, DrawnHistoryEvent::new));

    private static final Codec<GeneratedSettlement> SETTLEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(GeneratedSettlement::id),
            SettlementSavedDataCodec.BOUNDS_CODEC.fieldOf("bounds").forGetter(GeneratedSettlement::bounds),
            SettlementSavedDataCodec.PROFILE_CODEC.fieldOf("profile").forGetter(GeneratedSettlement::profile),
            HISTORY_EVENT_CODEC.listOf().fieldOf("history").forGetter(GeneratedSettlement::history)
    ).apply(instance, GeneratedSettlement::new));

    private final GeneratedSettlement settlement;

    /**
     * @param position the one block the piece occupies; it places nothing there
     */
    public SettlementRecordPiece(@Nonnull GeneratedSettlement settlement, @Nonnull BlockPos position) {
        super(StructureRegistry.SETTLEMENT_RECORD_PIECE_TYPE.get(), 0, new BoundingBox(position));
        this.settlement = settlement;
    }

    private SettlementRecordPiece(@Nonnull CompoundTag tag) {
        super(StructureRegistry.SETTLEMENT_RECORD_PIECE_TYPE.get(), tag);
        // Throwing drops only this piece: its structure start logs a piece that fails to load and keeps the others
        this.settlement = SETTLEMENT_CODEC.parse(NbtOps.INSTANCE, tag.getCompound(SETTLEMENT_TAG)).getOrThrow();
    }

    public static SettlementRecordPiece deserialize(@Nonnull StructurePieceSerializationContext context,
                                                    @Nonnull CompoundTag tag) {
        return new SettlementRecordPiece(tag);
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull StructurePieceSerializationContext context, @Nonnull CompoundTag tag) {
        tag.put(SETTLEMENT_TAG, SETTLEMENT_CODEC.encodeStart(NbtOps.INSTANCE, this.settlement).getOrThrow());
    }

    @Override
    public void postProcess(@Nonnull WorldGenLevel level,
                            @Nonnull StructureManager structureManager,
                            @Nonnull ChunkGenerator chunkGenerator,
                            @Nonnull RandomSource random,
                            @Nonnull BoundingBox box,
                            @Nonnull ChunkPos chunkPos,
                            @Nonnull BlockPos pivot) {
    }

}
