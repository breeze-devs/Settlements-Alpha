package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.personality.OriginType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class VillagerStatsSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 256;
    // The persona's characterSketch is a short paragraph (SIS caps it around ~1000 chars), well
    // beyond the short-label MAX_TEXT_LENGTH used by every other string field in this snapshot.
    private static final int MAX_SKETCH_LENGTH = 1024;
    // Spec targets 3-6 adjectives; this is a sanity ceiling against a malformed/hostile payload, not a design limit.
    private static final int MAX_ADJECTIVES = 8;
    private static final int GENE_COUNT = GeneType.VALUES.length;

    public static VillagerStatsSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        long gameTime = buffer.readLong();
        int villagerEntityId = buffer.readInt();
        String villagerName = readNullableString(buffer);
        String professionKey = buffer.readUtf(MAX_TEXT_LENGTH);
        int expertiseLevel = buffer.readInt();
        float currentHealth = buffer.readFloat();
        float maxHealth = buffer.readFloat();

        double[] geneValues = new double[GENE_COUNT];
        for (int i = 0; i < GENE_COUNT; i++) {
            geneValues[i] = buffer.readDouble();
        }

        List<String> adjectives = readAdjectives(buffer);
        String characterSketch = readNullableSketch(buffer);
        OriginType origin = readNullableOrigin(buffer);

        BlockPos homePos = readNullableBlockPos(buffer);
        BlockPos workstationPos = readNullableBlockPos(buffer);

        String activeBehaviorNameKey = readNullableString(buffer);
        String activeBehaviorIconId = readNullableString(buffer);

        SchedulePhase schedulePhase = buffer.readEnum(SchedulePhase.class);
        int reputation = buffer.readInt();
        float hunger = buffer.readFloat();
        int walletBalance = buffer.readInt();

        return VillagerStatsSnapshot.builder()
                .gameTime(gameTime)
                .villagerEntityId(villagerEntityId)
                .villagerName(villagerName)
                .professionKey(professionKey)
                .expertiseLevel(expertiseLevel)
                .currentHealth(currentHealth)
                .maxHealth(maxHealth)
                .geneValues(geneValues)
                .adjectives(adjectives)
                .characterSketch(characterSketch)
                .origin(origin)
                .homePos(homePos)
                .workstationPos(workstationPos)
                .activeBehaviorNameKey(activeBehaviorNameKey)
                .activeBehaviorIconId(activeBehaviorIconId)
                .schedulePhase(schedulePhase)
                .reputation(reputation)
                .hunger(hunger)
                .walletBalance(walletBalance)
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull VillagerStatsSnapshot snapshot) {
        buffer.writeLong(snapshot.gameTime());
        buffer.writeInt(snapshot.villagerEntityId());
        writeNullableString(buffer, snapshot.villagerName());
        buffer.writeUtf(snapshot.professionKey(), MAX_TEXT_LENGTH);
        buffer.writeInt(snapshot.expertiseLevel());
        buffer.writeFloat(snapshot.currentHealth());
        buffer.writeFloat(snapshot.maxHealth());

        for (int i = 0; i < GENE_COUNT; i++) {
            buffer.writeDouble(snapshot.geneValues()[i]);
        }

        writeAdjectives(buffer, snapshot.adjectives());
        writeNullableSketch(buffer, snapshot.characterSketch());
        writeNullableOrigin(buffer, snapshot.origin());

        writeNullableBlockPos(buffer, snapshot.homePos());
        writeNullableBlockPos(buffer, snapshot.workstationPos());

        writeNullableString(buffer, snapshot.activeBehaviorNameKey());
        writeNullableString(buffer, snapshot.activeBehaviorIconId());

        buffer.writeEnum(snapshot.schedulePhase());
        buffer.writeInt(snapshot.reputation());
        buffer.writeFloat(snapshot.hunger());
        buffer.writeInt(snapshot.walletBalance());
    }

    @Nullable
    private static String readNullableString(@Nonnull FriendlyByteBuf buffer) {
        boolean present = buffer.readBoolean();
        return present ? buffer.readUtf(MAX_TEXT_LENGTH) : null;
    }

    private static void writeNullableString(@Nonnull FriendlyByteBuf buffer, @Nullable String value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeUtf(value, MAX_TEXT_LENGTH);
        }
    }

    @Nullable
    private static BlockPos readNullableBlockPos(@Nonnull FriendlyByteBuf buffer) {
        boolean present = buffer.readBoolean();
        return present ? buffer.readBlockPos() : null;
    }

    private static void writeNullableBlockPos(@Nonnull FriendlyByteBuf buffer, @Nullable BlockPos pos) {
        buffer.writeBoolean(pos != null);
        if (pos != null) {
            buffer.writeBlockPos(pos);
        }
    }

    @Nonnull
    private static List<String> readAdjectives(@Nonnull FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ADJECTIVES) {
            throw new IllegalArgumentException("Invalid adjective count: " + size);
        }

        List<String> adjectives = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            adjectives.add(buffer.readUtf(MAX_TEXT_LENGTH));
        }
        return adjectives;
    }

    private static void writeAdjectives(@Nonnull FriendlyByteBuf buffer, @Nonnull List<String> adjectives) {
        if (adjectives.size() > MAX_ADJECTIVES) {
            throw new IllegalArgumentException("Invalid adjective count: " + adjectives.size());
        }

        buffer.writeVarInt(adjectives.size());
        for (String adjective : adjectives) {
            buffer.writeUtf(adjective, MAX_TEXT_LENGTH);
        }
    }

    @Nullable
    private static String readNullableSketch(@Nonnull FriendlyByteBuf buffer) {
        boolean present = buffer.readBoolean();
        return present ? buffer.readUtf(MAX_SKETCH_LENGTH) : null;
    }

    private static void writeNullableSketch(@Nonnull FriendlyByteBuf buffer, @Nullable String sketch) {
        buffer.writeBoolean(sketch != null);
        if (sketch != null) {
            buffer.writeUtf(sketch, MAX_SKETCH_LENGTH);
        }
    }

    @Nullable
    private static OriginType readNullableOrigin(@Nonnull FriendlyByteBuf buffer) {
        boolean present = buffer.readBoolean();
        return present ? buffer.readEnum(OriginType.class) : null;
    }

    private static void writeNullableOrigin(@Nonnull FriendlyByteBuf buffer, @Nullable OriginType origin) {
        buffer.writeBoolean(origin != null);
        if (origin != null) {
            buffer.writeEnum(origin);
        }
    }

}
