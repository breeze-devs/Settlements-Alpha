package dev.breezes.settlements.application.ui.behavior.model;

import lombok.Builder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

@Builder
public record BehaviorControllerSnapshot(
        long gameTime,
        int villagerEntityId,
        @Nonnull String villagerName,
        @Nonnull SchedulePhase scheduleBucket,
        @Nonnull String rawActivityKey,
        @Nonnull List<BehaviorRowSnapshot> rows
) {

    public BehaviorControllerSnapshot {
        Objects.requireNonNull(villagerName, "villagerName");
        Objects.requireNonNull(scheduleBucket, "scheduleBucket");
        Objects.requireNonNull(rawActivityKey, "rawActivityKey");
        Objects.requireNonNull(rows, "rows");
        rows = List.copyOf(rows);
    }

}
