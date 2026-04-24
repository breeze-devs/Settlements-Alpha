package dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces;

import dev.breezes.settlements.bootstrap.registry.structures.StructureRegistry;
import dev.breezes.settlements.domain.generation.model.geometry.Direction;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.SettlementsStructurePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SettlementBuildingPiece extends SettlementsStructurePiece {

    private static final String SETTLEMENT_ID_TAG = "SettlementId";
    private static final String BUILDING_DEFINITION_ID_TAG = "BuildingDefinitionId";

    @Nullable
    private final String settlementId;
    private final String buildingDefinitionId;

    public SettlementBuildingPiece(StructureTemplateManager templateManager, ResourceLocation templateId,
                                   BlockPos position, Rotation rotation,
                                   @Nullable String settlementId,
                                   @Nonnull String buildingDefinitionId) {
        super(StructureRegistry.SETTLEMENT_BUILDING_PIECE_TYPE.get(), templateManager, templateId, position, rotation);
        this.settlementId = settlementId;
        this.buildingDefinitionId = buildingDefinitionId;
    }

    private SettlementBuildingPiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(StructureRegistry.SETTLEMENT_BUILDING_PIECE_TYPE.get(), tag, templateManager,
                resourceLocation -> SettlementsStructurePiece.createSettings(SettlementsStructurePiece.readRotation(tag)));
        this.settlementId = tag.contains(SETTLEMENT_ID_TAG) ? tag.getString(SETTLEMENT_ID_TAG) : null;
        this.buildingDefinitionId = tag.contains(BUILDING_DEFINITION_ID_TAG)
                ? tag.getString(BUILDING_DEFINITION_ID_TAG)
                : "unknown";
    }

    public static SettlementBuildingPiece deserialize(StructurePieceSerializationContext context, CompoundTag tag) {
        return new SettlementBuildingPiece(context.structureTemplateManager(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        if (this.settlementId != null) {
            tag.putString(SETTLEMENT_ID_TAG, this.settlementId);
        }
        tag.putString(BUILDING_DEFINITION_ID_TAG, this.buildingDefinitionId);
    }

    @Nullable
    public String getSettlementId() {
        return this.settlementId;
    }

    @Nonnull
    public String getBuildingDefinitionId() {
        return this.buildingDefinitionId;
    }

    public static Rotation fromDirection(@Nonnull Direction direction) {
        return switch (direction) {
            case NORTH -> Rotation.NONE;
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
        };
    }

}
