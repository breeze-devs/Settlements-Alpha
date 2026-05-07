package dev.breezes.settlements.infrastructure.network.features.ui.dayplan.codec;

import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotSnapshot;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotVisualStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DayPlanSlotSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 512;

    public static DayPlanSlotSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        return DayPlanSlotSnapshot.builder()
                .behaviorKey(buffer.readUtf(MAX_TEXT_LENGTH))
                .displayNameKey(buffer.readUtf(MAX_TEXT_LENGTH))
                .formattedTime(buffer.readUtf(MAX_TEXT_LENGTH))
                .iconItemId(ResourceLocation.parse(buffer.readUtf(MAX_TEXT_LENGTH)))
                .status(buffer.readEnum(DayPlanSlotVisualStatus.class))
                .description(readNullableString(buffer))
                .flexible(buffer.readBoolean())
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull DayPlanSlotSnapshot snapshot) {
        buffer.writeUtf(snapshot.behaviorKey(), MAX_TEXT_LENGTH);
        buffer.writeUtf(snapshot.displayNameKey(), MAX_TEXT_LENGTH);
        buffer.writeUtf(snapshot.formattedTime(), MAX_TEXT_LENGTH);
        buffer.writeUtf(snapshot.iconItemId().toString(), MAX_TEXT_LENGTH);
        buffer.writeEnum(snapshot.status());
        writeNullableString(buffer, snapshot.description());
        buffer.writeBoolean(snapshot.flexible());
    }

    @Nullable
    private static String readNullableString(@Nonnull FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUtf(MAX_TEXT_LENGTH) : null;
    }

    private static void writeNullableString(@Nonnull FriendlyByteBuf buffer, @Nullable String value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeUtf(value, MAX_TEXT_LENGTH);
        }
    }

}
