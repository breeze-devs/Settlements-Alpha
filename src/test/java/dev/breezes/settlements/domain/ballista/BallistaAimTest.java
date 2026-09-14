package dev.breezes.settlements.domain.ballista;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaAimTest {

    private static final float YAW_RATE = 5.0F;
    private static final float PITCH_RATE = 1.5F;

    // Generous enough for any turn here at the slowest rate used, so a stalled aim fails rather than hangs
    private static final int STEP_LIMIT = 10_000;

    private static final float[] RATES = {0.5F, 3.0F, 7.25F, 90.0F, 179.0F, 360.0F, 1_000.0F};

    // Pairs of start and intent headings, including turns across the north seam and exactly half a turn apart
    private static final float[][] HEADING_PAIRS = {
            {0.0F, 90.0F}, {90.0F, 0.0F}, {170.0F, -170.0F}, {-170.0F, 170.0F}, {-179.5F, 179.5F},
            {45.0F, -135.0F}, {-90.0F, 89.0F}, {12.3F, -167.8F}
    };

    @Test
    void placement_roundsToTheNearestManualHeading_onEitherSideOfZero() {
        // Arrange: nearest rounding must differ from both truncation and always rounding up.
        float step = BallistaAim.MANUAL_YAW_STEP_DEGREES;
        float[][] cases = {
                {0.4F * step, 0.0F}, {0.6F * step, step},
                {-0.4F * step, 0.0F}, {-0.6F * step, -step},
                {2.8F * step, 3.0F * step}, {3.2F * step, 3.0F * step},
                {3.0F * step, 3.0F * step}
        };

        for (float[] heading : cases) {
            // Act
            BallistaAim placed = BallistaAim.forPlacement(heading[0]);

            // Assert
            assertEquals(heading[1], placed.getYaw(), "placement yaw " + heading[0]);
            assertEquals(heading[1], placed.getIntentYaw());
            assertEquals(0.0F, placed.getPitch());
            assertEquals(0.0F, placed.getIntentPitch());
            assertTrue(placed.isAtRest());
        }
    }

    @Test
    void placement_wrapsHeadingsRoundedAcrossNorth() {
        // Arrange
        float nearNorth = 180.0F - BallistaAim.MANUAL_YAW_STEP_DEGREES / 4.0F;

        // Act
        BallistaAim fromPositive = BallistaAim.forPlacement(nearNorth);
        BallistaAim fromNegative = BallistaAim.forPlacement(-nearNorth);

        // Assert: north must retain one representation even when snapping produces positive 180.
        assertEquals(-180.0F, fromPositive.getYaw());
        assertEquals(-180.0F, fromNegative.getYaw());
        assertTrue(fromPositive.isAtRest());
        assertTrue(fromNegative.isAtRest());
    }

    @Test
    void placement_roundsHalfwayHeadingsTowardIncreasingYaw() {
        // Arrange
        float step = BallistaAim.MANUAL_YAW_STEP_DEGREES;

        // Act
        BallistaAim positive = BallistaAim.forPlacement(step / 2.0F);
        BallistaAim negative = BallistaAim.forPlacement(-step / 2.0F);

        // Assert
        assertEquals(step, positive.getYaw());
        assertEquals(0.0F, negative.getYaw());
    }

    @Test
    void placement_ignoresAccumulatedFullTurns() {
        // Arrange
        float step = BallistaAim.MANUAL_YAW_STEP_DEGREES;
        float yaw = 360.0F * 12.0F + step * 0.6F;

        // Act
        BallistaAim positive = BallistaAim.forPlacement(yaw);
        BallistaAim negative = BallistaAim.forPlacement(-yaw);

        // Assert
        assertEquals(step, positive.getYaw());
        assertEquals(-step, negative.getYaw());
    }

    @Test
    void step_neverPassesTheIntent_atAnyRate() {
        for (float rate : RATES) {
            for (float[] pair : HEADING_PAIRS) {
                // Arrange
                BallistaAim aim = BallistaAim.facing(pair[0], BallistaAim.MIN_PITCH_DEGREES)
                        .withIntent(pair[1], BallistaAim.MAX_PITCH_DEGREES);

                // Act, Assert: the remaining turn on each axis shrinks or holds its sign until it reaches zero; a
                // step that moved the full rate regardless of what remained would flip the sign on the last step
                int steps = 0;
                while (!aim.isAtRest()) {
                    double yawBefore = shortWayDegrees(aim.getYaw(), aim.getIntentYaw());
                    double pitchBefore = aim.getIntentPitch() - aim.getPitch();

                    aim = aim.step(rate, rate);

                    double yawAfter = shortWayDegrees(aim.getYaw(), aim.getIntentYaw());
                    double pitchAfter = aim.getIntentPitch() - aim.getPitch();
                    String context = "rate " + rate + ", " + pair[0] + " -> " + pair[1] + ", step " + steps;
                    assertTrue(yawAfter * yawBefore >= 0.0 && Math.abs(yawAfter) <= Math.abs(yawBefore),
                            "yaw passed its intent: " + context + ", remaining " + yawBefore + " -> " + yawAfter);
                    assertTrue(pitchAfter * pitchBefore >= 0.0 && Math.abs(pitchAfter) <= Math.abs(pitchBefore),
                            "pitch passed its intent: " + context + ", remaining " + pitchBefore + " -> " + pitchAfter);
                    steps++;
                    assertTrue(steps < STEP_LIMIT, "never came to rest: " + context);
                }
            }
        }
    }

    @Test
    void step_turnsAcrossNorth_whenThatIsTheShortWay() {
        // Arrange: 20 degrees apart the short way across north, 340 the long way through south
        float startYaw = 170.0F;
        float intentYaw = -170.0F;
        BallistaAim aim = BallistaAim.facing(startYaw, 0.0F).withIntent(intentYaw, 0.0F);

        // Act
        int steps = 0;
        while (!aim.isAtRest() && steps < STEP_LIMIT) {
            aim = aim.step(YAW_RATE, PITCH_RATE);
            steps++;

            // Assert: the long way would carry the muzzle through the southern half of the circle
            assertTrue(Math.abs(aim.getYaw()) >= Math.abs(startYaw),
                    "turned the long way through yaw " + aim.getYaw() + " on step " + steps);
        }
        assertEquals(Math.ceil(shortWayDistance(startYaw, intentYaw) / YAW_RATE), steps);
    }

    @Test
    void step_turnsAcrossNorth_inTheOtherDirection() {
        // Arrange
        float startYaw = -170.0F;
        float intentYaw = 170.0F;
        BallistaAim aim = BallistaAim.facing(startYaw, 0.0F).withIntent(intentYaw, 0.0F);

        // Act
        BallistaAim stepped = aim.step(YAW_RATE, PITCH_RATE);

        // Assert: from -170 the short way to 170 decreases through -180, never increases toward 0
        assertEquals(startYaw - YAW_RATE, stepped.getYaw());
    }

    @Test
    void step_endsExactlyOnTheIntent() {
        // Arrange: rates and angles that do not divide one another, so every step carries float rounding
        float intentYaw = -77.9F;
        float intentPitch = 23.1F;
        BallistaAim aim = BallistaAim.facing(10.3F, 1.7F).withIntent(intentYaw, intentPitch);

        // Act
        int steps = 0;
        while (!aim.isAtRest() && steps < STEP_LIMIT) {
            aim = aim.step(3.7F, 1.3F);
            steps++;
        }

        // Assert: an aim that settled a rounding error away from its intent would never report rest, and would
        // keep stepping forever
        assertTrue(aim.isAtRest(), "never came to rest");
        assertEquals(intentYaw, aim.getYaw());
        assertEquals(intentPitch, aim.getPitch());
    }

    @Test
    void step_limitsEachAxisByItsOwnRate() {
        // Arrange
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F).withIntent(90.0F, BallistaAim.MAX_PITCH_DEGREES);

        // Act
        BallistaAim stepped = aim.step(YAW_RATE, PITCH_RATE);

        // Assert: swapped rates would turn yaw by the pitch rate and pitch by the yaw rate
        assertEquals(YAW_RATE, stepped.getYaw());
        assertEquals(PITCH_RATE, stepped.getPitch());
    }

    @Test
    void withIntent_holdsPitchAtTheNearerLimit() {
        // Arrange
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F);

        // Act
        BallistaAim tooHigh = aim.withIntent(0.0F, BallistaAim.MAX_PITCH_DEGREES + 40.0F);
        BallistaAim tooLow = aim.withIntent(0.0F, BallistaAim.MIN_PITCH_DEGREES - 40.0F);

        // Assert
        assertEquals(BallistaAim.MAX_PITCH_DEGREES, tooHigh.getIntentPitch());
        assertEquals(BallistaAim.MIN_PITCH_DEGREES, tooLow.getIntentPitch());
    }

    @Test
    void facing_holdsPitchAtTheNearerLimit() {
        // Act
        BallistaAim tooHigh = BallistaAim.facing(0.0F, 90.0F);
        BallistaAim tooLow = BallistaAim.facing(0.0F, -90.0F);

        // Assert: a heading restored from outside the range must not start the muzzle beyond a limit
        assertEquals(BallistaAim.MAX_PITCH_DEGREES, tooHigh.getPitch());
        assertEquals(BallistaAim.MIN_PITCH_DEGREES, tooLow.getPitch());
    }

    @Test
    void withIntent_returnsTheSameAim_whenTheIntentDoesNotChange() {
        // Arrange: 190 and -170 are one heading
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F).withIntent(-170.0F, 10.0F);

        // Act
        BallistaAim sameHeading = aim.withIntent(190.0F, 10.0F);

        // Assert: an aim that reported a change for the same heading would be re-sent to clients on every tick
        assertSame(aim, sameHeading);
    }

    @Test
    void withIntent_keepsTheCurrentAngles_whenTheIntentChanges() {
        // Arrange
        BallistaAim aim = BallistaAim.facing(30.0F, 5.0F);

        // Act
        BallistaAim retargeted = aim.withIntent(-60.0F, 15.0F);

        // Assert: the machine turns toward a new intent rather than jumping to it
        assertNotSame(aim, retargeted);
        assertEquals(30.0F, retargeted.getYaw());
        assertEquals(5.0F, retargeted.getPitch());
        assertEquals(-60.0F, retargeted.getIntentYaw());
    }

    @Test
    void intentDeviationFrom_measuresYawTheShortWayAcrossNorth() {
        // Arrange
        BallistaAim east = BallistaAim.facing(0.0F, 0.0F).withIntent(179.0F, 0.0F);
        BallistaAim west = BallistaAim.facing(0.0F, 0.0F).withIntent(-179.0F, 0.0F);

        // Act
        float deviation = east.intentDeviationFrom(west);

        // Assert: measured the long way, a two-degree nudge across north would read as a near full turn
        assertEquals(2.0F, deviation, 1.0E-3F);
    }

    @Test
    void intentDeviationFrom_reportsTheLargerAxis() {
        // Arrange
        BallistaAim reference = BallistaAim.facing(0.0F, 0.0F);
        BallistaAim raised = reference.withIntent(3.0F, 12.0F);

        // Act, Assert: a pitch-only change must count even when yaw barely moved
        assertEquals(12.0F, raised.intentDeviationFrom(reference));
    }

    @Test
    void isAtRest_requiresBothAxesToHaveArrived() {
        // Arrange: a short turn in heading and a long one in pitch, so yaw arrives well before pitch
        BallistaAim aim = BallistaAim.facing(0.0F, BallistaAim.MIN_PITCH_DEGREES)
                .withIntent(YAW_RATE / 2.0F, BallistaAim.MAX_PITCH_DEGREES);

        // Act
        BallistaAim stepped = aim.step(YAW_RATE, PITCH_RATE);

        // Assert: an arrival read from yaw alone would call this at rest while pitch still has most of its turn left
        assertFalse(stepped.isAtRest());
    }

    @Test
    void isAtRest_isJudgedFromTheTrueAngle_notFromWholeDegrees() {
        // Arrange: less than half a degree from the intent on each axis, which a whole-degree readout shows as arrived
        BallistaAim aim = BallistaAim.facing(10.0F, 10.0F).withIntent(10.3F, 10.3F);

        // Act, Assert
        assertFalse(aim.isAtRest());
    }

    /**
     * The signed turn from one heading to another the short way, in [-180, 180).
     */
    private static double shortWayDegrees(double from, double to) {
        return ((to - from) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    private static double shortWayDistance(double from, double to) {
        return Math.abs(shortWayDegrees(from, to));
    }

}
