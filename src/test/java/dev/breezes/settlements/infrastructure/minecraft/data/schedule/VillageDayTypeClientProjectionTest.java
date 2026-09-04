package dev.breezes.settlements.infrastructure.minecraft.data.schedule;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillageDayTypeClientProjectionTest {

    private static final long SYNCED_DAY = 1830L;

    private final VillageDayTypeClientProjection projection = new VillageDayTypeClientProjection();

    @Test
    void dayTypeFor_reportsTheSyncedVerdictForTheDayItSpeaksFor() {
        // Arrange
        this.projection.applySnapshot(SYNCED_DAY, PlanDayType.REST_DAY);

        // Act
        Optional<PlanDayType> dayType = this.projection.dayTypeFor(SYNCED_DAY);

        // Assert
        assertEquals(Optional.of(PlanDayType.REST_DAY), dayType);
    }

    @Test
    void dayTypeFor_withholdsTheVerdictOnceTheClientHasRolledPastItsDay() {
        // Arrange
        this.projection.applySnapshot(SYNCED_DAY, PlanDayType.REST_DAY);

        // Act
        Optional<PlanDayType> dayType = this.projection.dayTypeFor(SYNCED_DAY + 1);

        // Assert
        assertEquals(Optional.empty(), dayType);
    }

    @Test
    void dayTypeFor_hasNothingToReportBeforeTheFirstSync() {
        // Arrange, Act
        Optional<PlanDayType> dayType = this.projection.dayTypeFor(0L);

        // Assert
        assertEquals(Optional.empty(), dayType);
    }

    @Test
    void dayTypeFor_forgetsTheVerdictWhenTheSessionEnds() {
        // Arrange
        this.projection.applySnapshot(SYNCED_DAY, PlanDayType.WORK_DAY);

        // Act
        this.projection.onClientSessionEnded();

        // Assert
        assertEquals(Optional.empty(), this.projection.dayTypeFor(SYNCED_DAY));
    }

}
