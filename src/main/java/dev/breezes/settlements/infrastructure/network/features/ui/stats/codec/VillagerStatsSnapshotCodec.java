package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import dev.breezes.settlements.domain.genetics.GeneType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VillagerStatsSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 256;
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

}
