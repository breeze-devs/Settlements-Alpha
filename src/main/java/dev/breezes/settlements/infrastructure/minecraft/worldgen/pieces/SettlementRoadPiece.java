package dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces;

import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SettlementRoadPiece extends StructurePiece {

    private static final String START_X_TAG = "StartX";
    private static final String START_Y_TAG = "StartY";
    private static final String START_Z_TAG = "StartZ";
    private static final String END_X_TAG = "EndX";
    private static final String END_Y_TAG = "EndY";
    private static final String END_Z_TAG = "EndZ";
    private static final String ROAD_TYPE_TAG = "RoadType";
    private static final String WEALTH_LEVEL_TAG = "WealthLevel";
    private static final String SETTLEMENT_ID_TAG = "SettlementId";

    private final BlockPos start;
    private final BlockPos end;
    private final RoadType roadType;
    private final float wealthLevel;
    @Nullable
    private final String settlementId;

    public SettlementRoadPiece(@Nonnull BlockPos start,
                               @Nonnull BlockPos end,
                               @Nonnull RoadType roadType,
                               float wealthLevel,
                               @Nullable String settlementId) {
        super(StructureRegistry.SETTLEMENT_ROAD_PIECE_TYPE.get(), 0, createBoundingBox(start, end, roadType));
        this.start = start;
        this.end = end;
        this.roadType = roadType;
        this.wealthLevel = wealthLevel;
        this.settlementId = settlementId;
    }

    private SettlementRoadPiece(@Nonnull CompoundTag tag) {
        this(new BlockPos(tag.getInt(START_X_TAG), tag.getInt(START_Y_TAG), tag.getInt(START_Z_TAG)),
                new BlockPos(tag.getInt(END_X_TAG), tag.getInt(END_Y_TAG), tag.getInt(END_Z_TAG)),
                RoadType.values()[tag.getInt(ROAD_TYPE_TAG)],
                tag.getFloat(WEALTH_LEVEL_TAG),
                tag.contains(SETTLEMENT_ID_TAG) ? tag.getString(SETTLEMENT_ID_TAG) : null);
    }

    public static SettlementRoadPiece deserialize(@Nonnull StructurePieceSerializationContext context,
                                                  @Nonnull CompoundTag tag) {
        return new SettlementRoadPiece(tag);
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull StructurePieceSerializationContext context,
                                         @Nonnull CompoundTag tag) {
        tag.putInt(START_X_TAG, this.start.getX());
        tag.putInt(START_Y_TAG, this.start.getY());
        tag.putInt(START_Z_TAG, this.start.getZ());
        tag.putInt(END_X_TAG, this.end.getX());
        tag.putInt(END_Y_TAG, this.end.getY());
        tag.putInt(END_Z_TAG, this.end.getZ());
        tag.putInt(ROAD_TYPE_TAG, this.roadType.ordinal());
        tag.putFloat(WEALTH_LEVEL_TAG, this.wealthLevel);
        if (this.settlementId != null) {
            tag.putString(SETTLEMENT_ID_TAG, this.settlementId);
        }
    }

    @Nullable
    public String getSettlementId() {
        return this.settlementId;
    }

    @Override
    public void postProcess(@Nonnull WorldGenLevel level,
                            @Nonnull StructureManager structureManager,
                            @Nonnull ChunkGenerator chunkGenerator,
                            @Nonnull RandomSource random,
                            @Nonnull BoundingBox box,
                            @Nonnull ChunkPos chunkPos,
                            @Nonnull BlockPos pivot) {
        BlockState roadState = selectRoadBlock(this.wealthLevel).defaultBlockState();
        int halfSpan = (roadWidth(this.roadType) - 1) / 2;

        for (BlockPos point : rasterizeLine(this.start, this.end)) {
            boolean xMajor = Math.abs(this.end.getX() - this.start.getX()) >= Math.abs(this.end.getZ() - this.start.getZ());
            for (int offset = -halfSpan; offset <= halfSpan; offset++) {
                int x = xMajor ? point.getX() : point.getX() + offset;
                int z = xMajor ? point.getZ() + offset : point.getZ();
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockPos placePos = new BlockPos(x, surfaceY - 1, z);
                if (box.isInside(placePos)) {
                    level.setBlock(placePos, roadState, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    public boolean isCloseToChunk(@Nonnull ChunkPos chunkPos, int offset) {
        return chunkPos.getMinBlockX() - offset <= this.boundingBox.maxX()
                && chunkPos.getMaxBlockX() + offset >= this.boundingBox.minX()
                && chunkPos.getMinBlockZ() - offset <= this.boundingBox.maxZ()
                && chunkPos.getMaxBlockZ() + offset >= this.boundingBox.minZ();
    }

    private static BoundingBox createBoundingBox(@Nonnull BlockPos start,
                                                 @Nonnull BlockPos end,
                                                 @Nonnull RoadType roadType) {
        int halfSpan = (roadWidth(roadType) - 1) / 2;
        return new BoundingBox(Math.min(start.getX(), end.getX()) - halfSpan,
                Math.min(start.getY(), end.getY()) - 10,
                Math.min(start.getZ(), end.getZ()) - halfSpan,
                Math.max(start.getX(), end.getX()) + halfSpan,
                Math.max(start.getY(), end.getY()) + 10,
                Math.max(start.getZ(), end.getZ()) + halfSpan);
    }

    private static int roadWidth(@Nonnull RoadType roadType) {
        return switch (roadType) {
            case MAIN -> 9;
            case SECONDARY -> 5;
            case SIDE -> 3;
        };
    }

    private static Block selectRoadBlock(float wealthLevel) {
        if (wealthLevel < 0.33f) {
            return Blocks.DIRT_PATH;
        }
        if (wealthLevel <= 0.66f) {
            return Blocks.GRAVEL;
        }
        return Blocks.COBBLESTONE;
    }

    private static List<BlockPos> rasterizeLine(@Nonnull BlockPos start, @Nonnull BlockPos end) {
        List<BlockPos> points = new ArrayList<>();
        int x0 = start.getX();
        int z0 = start.getZ();
        int x1 = end.getX();
        int z1 = end.getZ();

        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int err = dx - dz;

        while (true) {
            points.add(new BlockPos(x0, start.getY(), z0));
            if (x0 == x1 && z0 == z1) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dz) {
                err -= dz;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                z0 += sz;
            }
        }

        return points;
    }

}
