package dev.breezes.settlements.infrastructure.network.features.ui.behavior.codec;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class BehaviorControllerSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 256;
    private static final int MAX_ROWS = 512;

    public static BehaviorControllerSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        long gameTime = buffer.readLong();
        int villagerEntityId = buffer.readInt();
        String villagerName = buffer.readUtf(MAX_TEXT_LENGTH);
        SchedulePhase scheduleBucket = buffer.readEnum(SchedulePhase.class);
        String rawActivityKey = buffer.readUtf(MAX_TEXT_LENGTH);

        int rowCount = buffer.readVarInt();
        if (rowCount < 0 || rowCount > MAX_ROWS) {
            throw new IllegalArgumentException("Invalid behavior snapshot rowCount: " + rowCount);
        }

        List<BehaviorRowSnapshot> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            rows.add(BehaviorRowSnapshotCodec.read(buffer));
        }

        return BehaviorControllerSnapshot.builder()
                .gameTime(gameTime)
                .villagerEntityId(villagerEntityId)
                .villagerName(villagerName)
                .scheduleBucket(scheduleBucket)
                .rawActivityKey(rawActivityKey)
                .rows(rows)
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BehaviorControllerSnapshot snapshot) {
        buffer.writeLong(snapshot.gameTime());
        buffer.writeInt(snapshot.villagerEntityId());
        buffer.writeUtf(snapshot.villagerName(), MAX_TEXT_LENGTH);
        buffer.writeEnum(snapshot.scheduleBucket());
        buffer.writeUtf(snapshot.rawActivityKey(), MAX_TEXT_LENGTH);

        buffer.writeVarInt(snapshot.rows().size());
        for (BehaviorRowSnapshot row : snapshot.rows()) {
            BehaviorRowSnapshotCodec.write(buffer, row);
        }
    }

}
