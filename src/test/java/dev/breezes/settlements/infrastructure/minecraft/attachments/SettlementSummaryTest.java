package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.UUID;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.firstMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.rosterEntry;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.secondMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.settlement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SettlementSummaryTest {

    private static final UUID ID = new UUID(0L, 1L);

    /**
     * A fading duration of the test's own, three loaded days long.
     */
    private static final int DURATION_DAYS = 3;

    private static final ClockTicks DURATION = ClockTicks.of((long) DURATION_DAYS * TimeOfDay.TICKS_PER_DAY);

    private static final int MINIMUM_RESIDENTS = 4;

    @Test
    void of_summarizesOnlyTheName_whenNotFading() {
        // Arrange
        Settlement settlement = settlement(ID).fading(null).build();

        // Act
        SettlementSummary summary = SettlementSummary.of(settlement, DURATION, MINIMUM_RESIDENTS);

        // Assert
        assertEquals(new SettlementSummary(settlement.name(), null), summary);
    }

    @ParameterizedTest(name = "{0} days and {1} ticks faded leaves {2} days")
    @CsvSource({
            // Whole days faded
            "0, 0, 3",
            "1, 0, 2",
            "2, 0, 1",
            // Part of a day faded still leaves that whole day
            "0, 1, 3",
            "1, -1, 3",
            "1, 1, 2",
            // The last tick before the duration is spent
            "3, -1, 1",
    })
    void of_roundsTheDaysLeftUp(int daysFaded, int ticksFaded, int expectedDaysLeft) {
        // Arrange
        Settlement settlement = fadedFor((long) daysFaded * TimeOfDay.TICKS_PER_DAY + ticksFaded);

        // Act
        SettlementSummary.Fading fading = SettlementSummary.of(settlement, DURATION, MINIMUM_RESIDENTS).fading();

        // Assert
        assertNotNull(fading);
        assertEquals(expectedDaysLeft, fading.daysLeft());
    }

    @Test
    void of_leavesNoDaysLeft_whenTheFadingOutlastsTheDuration() {
        // Arrange: a duration shortened after the settlement began fading
        Settlement settlement = fadedFor(DURATION.getTicks() + TimeOfDay.TICKS_PER_DAY);

        // Act
        SettlementSummary.Fading fading = SettlementSummary.of(settlement, DURATION, MINIMUM_RESIDENTS).fading();

        // Assert
        assertNotNull(fading);
        assertEquals(0, fading.daysLeft());
    }

    @Test
    void of_summarizesOnlyTheName_whenDecayIsDisabled() {
        // Arrange: a settlement left fading when decay was disabled
        Settlement settlement = fadedFor(TimeOfDay.TICKS_PER_DAY);

        // Act
        SettlementSummary summary = SettlementSummary.of(settlement, null, MINIMUM_RESIDENTS);

        // Assert
        assertNull(summary.fading());
    }

    @Test
    void of_countsTheRosterAgainstTheMinimum_whenFading() {
        // Arrange
        Settlement settlement = fadedFor(0L).toBuilder()
                .roster(new SettlementRoster(List.of(firstMember(), secondMember(),
                        rosterEntry(new UUID(0L, 103L)).build())))
                .build();

        // Act
        SettlementSummary.Fading fading = SettlementSummary.of(settlement, DURATION, MINIMUM_RESIDENTS).fading();

        // Assert
        assertNotNull(fading);
        assertEquals(3, fading.livingMembers());
        assertEquals(MINIMUM_RESIDENTS, fading.minimumResidents());
    }

    @Test
    void of_isUnchanged_byAFadingStepWithinADay() {
        // Arrange: a health visit's step that crosses no day boundary
        Settlement earlier = fadedFor(TimeOfDay.TICKS_PER_DAY + 1L);
        Settlement later = fadedFor(2L * TimeOfDay.TICKS_PER_DAY - 1L);

        // Act, Assert
        assertEquals(SettlementSummary.of(earlier, DURATION, MINIMUM_RESIDENTS),
                SettlementSummary.of(later, DURATION, MINIMUM_RESIDENTS));
    }

    @Test
    void of_changes_whenAFadingStepCrossesADay() {
        // Arrange
        Settlement earlier = fadedFor(TimeOfDay.TICKS_PER_DAY - 1L);
        Settlement later = fadedFor(TimeOfDay.TICKS_PER_DAY);

        // Act, Assert
        assertNotEquals(SettlementSummary.of(earlier, DURATION, MINIMUM_RESIDENTS),
                SettlementSummary.of(later, DURATION, MINIMUM_RESIDENTS));
    }

    @Test
    void of_isUnchanged_byARosterChangeWhileNotFading() {
        // Arrange: a member joining a healthy settlement, as a check-in rewriting a roster row is too
        Settlement before = settlement(ID).fading(null).roster(new SettlementRoster(List.of(firstMember()))).build();
        Settlement after = before.toBuilder().roster(new SettlementRoster(List.of(firstMember(), secondMember())))
                .build();

        // Act, Assert
        assertEquals(SettlementSummary.of(before, DURATION, MINIMUM_RESIDENTS),
                SettlementSummary.of(after, DURATION, MINIMUM_RESIDENTS));
    }

    @Test
    void of_changes_whenAFadingSettlementGainsAMember() {
        // Arrange
        Settlement before = fadedFor(0L).toBuilder().roster(new SettlementRoster(List.of(firstMember()))).build();
        Settlement after = before.toBuilder().roster(new SettlementRoster(List.of(firstMember(), secondMember())))
                .build();

        // Act, Assert
        assertNotEquals(SettlementSummary.of(before, DURATION, MINIMUM_RESIDENTS),
                SettlementSummary.of(after, DURATION, MINIMUM_RESIDENTS));
    }

    private static Settlement fadedFor(long ticks) {
        return settlement(ID).fading(new SettlementFading(ClockTicks.of(ticks))).build();
    }

}
