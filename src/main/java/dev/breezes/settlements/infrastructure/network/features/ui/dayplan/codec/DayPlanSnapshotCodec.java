package dev.breezes.settlements.infrastructure.network.features.ui.dayplan.codec;

import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotSnapshot;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSnapshot;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DayPlanSnapshotCodec {

    private static final int MAX_TEXT_LENGTH = 512;
    private static final int MAX_SLOT_COUNT = 128;

    public static DayPlanSnapshot read(@Nonnull FriendlyByteBuf buffer) {
        long dayNumber = buffer.readLong();
        PlanDayType dayType = buffer.readEnum(PlanDayType.class);
        String currentTime = buffer.readUtf(MAX_TEXT_LENGTH);
        PlanStatus planStatus = buffer.readEnum(PlanStatus.class);
        int villagerEntityId = buffer.readInt();
        String villagerName = buffer.readUtf(MAX_TEXT_LENGTH);
        int slotCount = buffer.readVarInt();
        if (slotCount < 0 || slotCount > MAX_SLOT_COUNT) {
            throw new IllegalArgumentException("Invalid day plan slot count: " + slotCount);
        }
        List<DayPlanSlotSnapshot> slots = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(DayPlanSlotSnapshotCodec.read(buffer));
        }

        return DayPlanSnapshot.builder()
                .dayNumber(dayNumber)
                .dayType(dayType)
                .currentTime(currentTime)
                .planStatus(planStatus)
                .villagerEntityId(villagerEntityId)
                .villagerName(villagerName)
                .slots(slots)
                .build();
    }

    public static void write(@Nonnull FriendlyByteBuf buffer, @Nonnull DayPlanSnapshot snapshot) {
        buffer.writeLong(snapshot.dayNumber());
        buffer.writeEnum(snapshot.dayType());
        buffer.writeUtf(snapshot.currentTime(), MAX_TEXT_LENGTH);
        buffer.writeEnum(snapshot.planStatus());
        buffer.writeInt(snapshot.villagerEntityId());
        buffer.writeUtf(snapshot.villagerName(), MAX_TEXT_LENGTH);
        buffer.writeVarInt(snapshot.slots().size());
        for (DayPlanSlotSnapshot slot : snapshot.slots()) {
            DayPlanSlotSnapshotCodec.write(buffer, slot);
        }
    }

}
