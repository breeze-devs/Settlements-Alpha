package dev.breezes.settlements.infrastructure.network.features.ui.behavior.codec;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.PreconditionSummary;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class BehaviorRowSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 256;
    private static final int MAX_REGISTERED_SCHEDULES = 16;

    public static BehaviorRowSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        String behaviorId = buffer.readUtf(MAX_TEXT_LENGTH);
        String displayNameKey = buffer.readUtf(MAX_TEXT_LENGTH);

        String displaySuffix = null;
        if (buffer.readBoolean()) {
            displaySuffix = buffer.readUtf(MAX_TEXT_LENGTH);
        }

        ResourceLocation iconItemId = buffer.readResourceLocation();
        int priority = buffer.readInt();
        int uiBehaviorIndex = buffer.readInt();

        int registeredSchedulesCount = buffer.readVarInt();
        if (registeredSchedulesCount < 0 || registeredSchedulesCount > MAX_REGISTERED_SCHEDULES) {
            throw new IllegalArgumentException("Invalid registeredSchedulesCount: " + registeredSchedulesCount);
        }
        List<SchedulePhase> registeredSchedules = new ArrayList<>(registeredSchedulesCount);
        for (int i = 0; i < registeredSchedulesCount; i++) {
            registeredSchedules.add(buffer.readEnum(SchedulePhase.class));
        }

        boolean running = buffer.readBoolean();

        String currentStageLabel = null;
        if (buffer.readBoolean()) {
            currentStageLabel = buffer.readUtf(MAX_TEXT_LENGTH);
        }

        int cooldownRemainingTicks = buffer.readInt();
        PreconditionSummary preconditionSummary = buffer.readEnum(PreconditionSummary.class);

        return BehaviorRowSnapshot.builder()
                .behaviorId(behaviorId)
                .displayNameKey(displayNameKey)
                .displaySuffix(displaySuffix)
                .iconItemId(iconItemId)
                .priority(priority)
                .uiBehaviorIndex(uiBehaviorIndex)
                .registeredSchedules(registeredSchedules)
                .running(running)
                .currentStageLabel(currentStageLabel)
                .cooldownRemainingTicks(cooldownRemainingTicks)
                .preconditionSummary(preconditionSummary)
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull BehaviorRowSnapshot row) {
        buffer.writeUtf(row.behaviorId(), MAX_TEXT_LENGTH);
        buffer.writeUtf(row.displayNameKey(), MAX_TEXT_LENGTH);

        boolean hasDisplaySuffix = row.displaySuffix() != null;
        buffer.writeBoolean(hasDisplaySuffix);
        if (hasDisplaySuffix) {
            buffer.writeUtf(row.displaySuffix(), MAX_TEXT_LENGTH);
        }

        buffer.writeResourceLocation(row.iconItemId());
        buffer.writeInt(row.priority());
        buffer.writeInt(row.uiBehaviorIndex());

        buffer.writeVarInt(row.registeredSchedules().size());
        for (SchedulePhase phase : row.registeredSchedules()) {
            buffer.writeEnum(phase);
        }

        buffer.writeBoolean(row.running());

        boolean hasCurrentStageLabel = row.currentStageLabel() != null;
        buffer.writeBoolean(hasCurrentStageLabel);
        if (hasCurrentStageLabel) {
            buffer.writeUtf(row.currentStageLabel(), MAX_TEXT_LENGTH);
        }

        buffer.writeInt(row.cooldownRemainingTicks());
        buffer.writeEnum(row.preconditionSummary());
    }

}
