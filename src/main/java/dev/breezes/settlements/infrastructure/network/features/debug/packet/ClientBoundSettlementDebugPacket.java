package dev.breezes.settlements.infrastructure.network.features.debug.packet;

import dev.breezes.settlements.domain.generation.model.layout.RoadType;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public record ClientBoundSettlementDebugPacket(
        @Nonnull BoundingBox settlementBox,
        @Nonnull List<BuildingBox> buildings,
        @Nonnull List<RoadBox> roads
) implements ClientBoundPacket {

    private static final int MAX_BUILDINGS = 256;
    private static final int MAX_ROADS = 512;

    public static final Type<ClientBoundSettlementDebugPacket> ID =
            new Type<>(ResourceLocationUtil.mod("packet_settlement_debug_clientbound"));

    public static final StreamCodec<FriendlyByteBuf, ClientBoundSettlementDebugPacket> CODEC =
            CustomPacketPayload.codec(ClientBoundSettlementDebugPacket::write, ClientBoundSettlementDebugPacket::decode);

    public ClientBoundSettlementDebugPacket {
        buildings = List.copyOf(buildings);
        roads = List.copyOf(roads);
    }

    private static ClientBoundSettlementDebugPacket decode(@Nonnull FriendlyByteBuf buffer) {
        BoundingBox settlementBox = readBoundingBox(buffer);

        int buildingCount = buffer.readVarInt();
        if (buildingCount < 0 || buildingCount > MAX_BUILDINGS) {
            throw new IllegalArgumentException("Invalid settlement debug buildingCount: " + buildingCount);
        }

        List<BuildingBox> buildings = new ArrayList<>(buildingCount);
        for (int i = 0; i < buildingCount; i++) {
            buildings.add(new BuildingBox(readBoundingBox(buffer), buffer.readUtf()));
        }

        int roadCount = buffer.readVarInt();
        if (roadCount < 0 || roadCount > MAX_ROADS) {
            throw new IllegalArgumentException("Invalid settlement debug roadCount: " + roadCount);
        }

        List<RoadBox> roads = new ArrayList<>(roadCount);
        for (int i = 0; i < roadCount; i++) {
            roads.add(new RoadBox(
                    readBoundingBox(buffer),
                    readRoadType(buffer),
                    readBlockPos(buffer),
                    readBlockPos(buffer)));
        }

        return new ClientBoundSettlementDebugPacket(settlementBox, buildings, roads);
    }

    private void write(@Nonnull FriendlyByteBuf buffer) {
        if (this.buildings.size() > MAX_BUILDINGS) {
            throw new IllegalArgumentException("Too many settlement debug buildings: " + this.buildings.size());
        }
        if (this.roads.size() > MAX_ROADS) {
            throw new IllegalArgumentException("Too many settlement debug roads: " + this.roads.size());
        }

        writeBoundingBox(buffer, this.settlementBox);

        buffer.writeVarInt(this.buildings.size());
        for (BuildingBox building : this.buildings) {
            writeBoundingBox(buffer, building.box());
            buffer.writeUtf(building.displayName());
        }

        buffer.writeVarInt(this.roads.size());
        for (RoadBox road : this.roads) {
            writeBoundingBox(buffer, road.box());
            writeRoadType(buffer, road.roadType());
            writeBlockPos(buffer, road.start());
            writeBlockPos(buffer, road.end());
        }
    }

    @Nonnull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static void writeBoundingBox(@Nonnull FriendlyByteBuf buffer, @Nonnull BoundingBox box) {
        buffer.writeInt(box.minX());
        buffer.writeInt(box.minY());
        buffer.writeInt(box.minZ());
        buffer.writeInt(box.maxX());
        buffer.writeInt(box.maxY());
        buffer.writeInt(box.maxZ());
    }

    @Nonnull
    private static BoundingBox readBoundingBox(@Nonnull FriendlyByteBuf buffer) {
        return new BoundingBox(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt());
    }

    private static void writeBlockPos(@Nonnull FriendlyByteBuf buffer, @Nonnull BlockPos blockPos) {
        buffer.writeInt(blockPos.getX());
        buffer.writeInt(blockPos.getY());
        buffer.writeInt(blockPos.getZ());
    }

    @Nonnull
    private static BlockPos readBlockPos(@Nonnull FriendlyByteBuf buffer) {
        return new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    private static void writeRoadType(@Nonnull FriendlyByteBuf buffer, @Nonnull RoadType roadType) {
        buffer.writeByte(roadType.ordinal());
    }

    @Nonnull
    private static RoadType readRoadType(@Nonnull FriendlyByteBuf buffer) {
        int ordinal = buffer.readUnsignedByte();
        RoadType[] values = RoadType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid road type ordinal: " + ordinal);
        }
        return values[ordinal];
    }

    public record BuildingBox(@Nonnull BoundingBox box, @Nonnull String displayName) {
    }

    public record RoadBox(@Nonnull BoundingBox box,
                          @Nonnull RoadType roadType,
                          @Nonnull BlockPos start,
                          @Nonnull BlockPos end) {
    }

}
