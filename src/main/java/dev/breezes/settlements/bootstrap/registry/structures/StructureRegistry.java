package dev.breezes.settlements.bootstrap.registry.structures;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRecordPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.structures.SettlementStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class StructureRegistry {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, SettlementsMod.MOD_ID);

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, SettlementsMod.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<SettlementStructure>> SETTLEMENT_STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("settlement", () -> () -> SettlementStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SETTLEMENT_BUILDING_PIECE_TYPE =
            PIECE_TYPES.register("settlement_building_piece", () -> SettlementBuildingPiece::deserialize);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SETTLEMENT_ROAD_PIECE_TYPE =
            PIECE_TYPES.register("settlement_road_piece", () -> SettlementRoadPiece::deserialize);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> SETTLEMENT_RECORD_PIECE_TYPE =
            PIECE_TYPES.register("settlement_record_piece", () -> SettlementRecordPiece::deserialize);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
    }

}
