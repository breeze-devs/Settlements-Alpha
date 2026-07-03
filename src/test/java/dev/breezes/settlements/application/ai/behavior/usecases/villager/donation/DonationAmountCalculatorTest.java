package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure donation arithmetic. No Minecraft types are involved.
 */
class DonationAmountCalculatorTest {

    private static final double DELTA = 0.0001;

    @Test
    void cap_recipientNearFloor_boundedByRecipientHeadroom() {
        // Arrange — recipient only needs 3 more emeralds to reach the floor, donor has plenty of room
        int floor = 50;
        int recipientBalance = 47;
        int donorBalance = 200;
        int comfort = 64;

        // Act
        int result = DonationAmountCalculator.cap(floor, recipientBalance, donorBalance, comfort);

        // Assert — the recipient's headroom (3) is the binding constraint
        assertEquals(3, result);
    }

    @Test
    void cap_donorNearComfort_boundedByDonorHeadroom() {
        // Arrange — donor only has 2 spare emeralds above their comfort cushion
        int floor = 50;
        int recipientBalance = 0;
        int donorBalance = 66;
        int comfort = 64;

        // Act
        int result = DonationAmountCalculator.cap(floor, recipientBalance, donorBalance, comfort);

        // Assert — the donor's headroom (2) is the binding constraint, not the recipient's (50)
        assertEquals(2, result);
    }

    @Test
    void cap_neitherMarginSufficient_returnsBelowMinDonation() {
        // Arrange — donor is right at their comfort cushion, no spare emeralds to give
        int floor = 50;
        int recipientBalance = 10;
        int donorBalance = 64;
        int comfort = 64;

        // Act
        int result = DonationAmountCalculator.cap(floor, recipientBalance, donorBalance, comfort);

        // Assert
        assertEquals(0, result);
        assertTrue(result < 4);
    }

    @Test
    void pick_repeatedDraws_alwaysWithinCapBounds() {
        // Arrange
        int cap = 10;
        int minDonation = 4;

        // Act & Assert — sample many draws since pick() is randomized
        for (int i = 0; i < 500; i++) {
            int result = DonationAmountCalculator.pick(cap, minDonation);
            assertTrue(result >= minDonation, "result " + result + " below minDonation " + minDonation);
            assertTrue(result <= cap, "result " + result + " above cap " + cap);
        }
    }

    @Test
    void pick_capBelowMinDonation_throws() {
        // Arrange
        int cap = 2;
        int minDonation = 4;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> DonationAmountCalculator.pick(cap, minDonation));
    }

    @Test
    void comfort_highCharisma_lowerThanBaseline() {
        // Arrange — high CHARISMA should pull comfort below the baseline (negative impact)
        double highCharisma = 1.0;
        int baseComfort = 64;
        double comfortImpact = 0.4;
        int floor = 20;

        // Act
        int result = DonationAmountCalculator.comfort(highCharisma, baseComfort, comfortImpact, floor);

        // Assert
        assertTrue(result < baseComfort, "high charisma should lower comfort below baseline");
    }

    @Test
    void comfort_lowCharisma_higherThanBaseline() {
        // Arrange — low CHARISMA should push comfort above the baseline
        double lowCharisma = 0.0;
        int baseComfort = 64;
        double comfortImpact = 0.4;
        int floor = 20;

        // Act
        int result = DonationAmountCalculator.comfort(lowCharisma, baseComfort, comfortImpact, floor);

        // Assert
        assertTrue(result > baseComfort, "low charisma should raise comfort above baseline");
    }

    @Test
    void comfort_computedBelowFloor_clampedToFloor() {
        // Arrange — a floor higher than the genetics-adjusted comfort must win the clamp
        double highCharisma = 1.0;
        int baseComfort = 10;
        double comfortImpact = 0.4;
        int floor = 50;

        // Act
        int result = DonationAmountCalculator.comfort(highCharisma, baseComfort, comfortImpact, floor);

        // Assert — a donor must never be allowed a cushion below the village floor
        assertEquals(floor, result);
    }

    @Test
    void chance_highCharisma_higherThanBaseline() {
        // Arrange — high CHARISMA should raise the donation chance (positive impact)
        double highCharisma = 1.0;
        double baseChance = 0.5;
        double chanceImpact = 0.6;

        // Act
        double result = DonationAmountCalculator.chance(highCharisma, baseChance, chanceImpact);

        // Assert
        assertTrue(result > baseChance, "high charisma should raise chance above baseline");
    }

    @Test
    void chance_lowCharisma_lowerThanBaseline() {
        // Arrange — low CHARISMA should lower the donation chance
        double lowCharisma = 0.0;
        double baseChance = 0.5;
        double chanceImpact = 0.6;

        // Act
        double result = DonationAmountCalculator.chance(lowCharisma, baseChance, chanceImpact);

        // Assert
        assertTrue(result < baseChance, "low charisma should lower chance below baseline");
    }

    @Test
    void chance_extremeInputs_clampedToUnitInterval() {
        // Arrange — a large baseChance/impact combination could exceed 1.0 without clamping
        double maxCharisma = 1.0;
        double baseChance = 0.9;
        double chanceImpact = 1.0;

        // Act
        double result = DonationAmountCalculator.chance(maxCharisma, baseChance, chanceImpact);

        // Assert
        assertEquals(1.0, result, DELTA);
    }

}
