package dev.breezes.settlements.infrastructure.minecraft.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.function.Function;

public abstract class SettlementsStructurePiece extends TemplateStructurePiece {

    private static final String ROTATION_TAG = "Rot";

    protected SettlementsStructurePiece(StructurePieceType type, StructureTemplateManager structureTemplateManager,
                                        ResourceLocation templateId, BlockPos position, Rotation rotation) {
        super(type, 0, structureTemplateManager, templateId,
                templateId.toString(), createSettings(rotation), position);
    }

    protected SettlementsStructurePiece(StructurePieceType type, CompoundTag tag,
                                        StructureTemplateManager structureTemplateManager,
                                        Function<ResourceLocation, StructurePlaceSettings> settingsFactory) {
        super(type, tag, structureTemplateManager, settingsFactory);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString(ROTATION_TAG, this.placeSettings.getRotation().name());
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
    }

    public static StructurePlaceSettings createSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
    }

    public static Rotation readRotation(CompoundTag tag) {
        if (!tag.contains(ROTATION_TAG)) {
            return Rotation.NONE;
        }
        return Rotation.valueOf(tag.getString(ROTATION_TAG));
    }

}
