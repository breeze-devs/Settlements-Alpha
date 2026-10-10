package dev.breezes.settlements.domain.ballista;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaFiringSolutionTest {

    // Vanilla's arrow motion each tick (AbstractArrow.tick): move by the velocity, then scale the velocity by the drag,
    // a float literal widened to double, then take the gravity off its vertical part
    private static final double VANILLA_DRAG = 0.99F;
    private static final double VANILLA_GRAVITY = 0.05;

    // A bolt-thrower's launch, faster than a fully drawn bow's arrow
    private static final double LAUNCH_SPEED = 3.9;

    // A launch slow enough that height at a fixed distance stops rising with pitch inside the pitch range
    private static final double SLOW_LAUNCH_SPEED = 0.5;

    // A tenth of a block: well inside the margin vanilla's hit test adds around every hitbox, so a solved shot leaves
    // room for an operator's scatter even against the smallest hostile
    private static final double HIT_TOLERANCE_BLOCKS = 0.1;

    // How far past the steepest or flattest possible shot a target sits, when no shot may reach it
    private static final double OUT_OF_RANGE_MARGIN_BLOCKS = 1.0;

    // Ticks either side of the longest flight, so the case is decided by the bound and not by rounding
    private static final int FLIGHT_BOUND_MARGIN_TICKS = 2;

    // Long enough to cover every flight here, and short enough that a missed target fails rather than hangs
    private static final int SIMULATED_TICKS = 200;

    // Bearings around the machine, including both sides of north, where yaw wraps
    private static final float[] BEARINGS = {0.0F, 45.0F, 90.0F, 135.0F, 179.5F, -179.5F, -90.0F, -30.0F};

    // Horizontal distance from the yaw axis and height above the machine's floor, at ranges from a few blocks to far
    // beyond any perception radius, each above, level with and below the machine
    private static final double[][] RANGES_AND_HEIGHTS = {
            {6.0, 0.0}, {6.0, 1.5},
            {12.0, -1.0}, {12.0, 3.0},
            {24.0, -3.0}, {24.0, 6.0},
            {48.0, -8.0}, {48.0, 0.0}, {48.0, 15.0},
            {96.0, -10.0}, {96.0, 0.0}, {96.0, 20.0}
    };

    // A target's position as the bolt leaves and its velocity per tick: crossing, charging, retreating, climbing
    // diagonally, and crossing while closing from the far side of the machine
    private static final Vec3[][] MOVING_TARGETS = {
            {new Vec3(0.0, 0.0, 16.0), new Vec3(0.2, 0.0, 0.0)},
            {new Vec3(0.0, 1.0, 24.0), new Vec3(0.0, 0.0, -0.4)},
            {new Vec3(0.0, 0.0, 32.0), new Vec3(0.0, 0.0, 0.3)},
            {new Vec3(5.0, 2.0, 20.0), new Vec3(0.15, 0.1, -0.15)},
            {new Vec3(-30.0, -2.0, -40.0), new Vec3(-0.3, 0.0, 0.1)}
    };

    // Ranges a fast target stays within reach of over its whole flight, and heights as a fraction of the range that
    // keep every lead inside the pitch range
    private static final double[] FAST_TARGET_RANGES = {12.0, 18.0, 24.0, 32.0, 40.0, 48.0, 56.0, 64.0, 72.0, 80.0};
    private static final double[] FAST_TARGET_HEIGHT_RATIOS = {-0.1, 0.0, 0.05, 0.1, 0.2};

    // Velocities per tick of a fast flier, some climbing or diving, that cross, close on or retreat from each bearing
    private static final Vec3[] FAST_TARGET_VELOCITIES = {
            new Vec3(0.6, 0.1, 0.2), new Vec3(-0.6, -0.1, 0.2), new Vec3(0.2, 0.05, -0.6), new Vec3(-0.3, 0.0, -0.5)
    };

    @Test
    void solve_landsTheBoltOnAStillTarget_atEveryBearingRangeAndHeight() {
        for (float bearing : BEARINGS) {
            for (double[] rangeAndHeight : RANGES_AND_HEIGHTS) {
                // Arrange
                Vec3 target = onBearing(bearing, rangeAndHeight[0], rangeAndHeight[1]);

                // Act
                Optional<BallistaFiringSolution> solution =
                        BallistaFiringSolution.solve(target, Vec3.ZERO, LAUNCH_SPEED);

                // Assert: a solver that ignores drag, gravity, the launch point moving with pitch, or the machine's own
                // yaw convention misses here
                assertTrue(solution.isPresent(), "no solution for " + target);
                assertLands(solution.get(), LAUNCH_SPEED, target, Vec3.ZERO);
            }
        }
    }

    @Test
    void solve_landsTheBoltOnAMovingTarget_whereItIsWhenTheBoltArrives() {
        for (Vec3[] targetAndVelocity : MOVING_TARGETS) {
            // Arrange
            Vec3 target = targetAndVelocity[0];
            Vec3 velocity = targetAndVelocity[1];

            // Act
            Optional<BallistaFiringSolution> solution = BallistaFiringSolution.solve(target, velocity, LAUNCH_SPEED);

            // Assert: aiming where the target is as the bolt leaves misses by its travel over the flight
            assertTrue(solution.isPresent(), "no solution for " + target + " moving " + velocity);
            assertLands(solution.get(), LAUNCH_SPEED, target, velocity);
        }
    }

    @Test
    void solve_landsTheBoltOnAFastTarget_atEveryBearingRangeAndHeightInReach() {
        for (float bearing : BEARINGS) {
            for (double range : FAST_TARGET_RANGES) {
                for (double heightRatio : FAST_TARGET_HEIGHT_RATIOS) {
                    for (Vec3 velocity : FAST_TARGET_VELOCITIES) {
                        // Arrange
                        Vec3 target = onBearing(bearing, range, range * heightRatio);

                        // Act
                        Optional<BallistaFiringSolution> solution =
                                BallistaFiringSolution.solve(target, velocity, LAUNCH_SPEED);

                        // Assert: a lead that waits for the flight time to stop changing more finely than the solved
                        // pitch can resolve it keeps chasing that jitter, and gives up on targets well within reach
                        assertTrue(solution.isPresent(), "no solution for " + target + " moving " + velocity);
                        assertLands(solution.get(), LAUNCH_SPEED, target, velocity);
                    }
                }
            }
        }
    }

    @Test
    void solve_findsTheFlattestShot_whenBothEndsOfThePitchRangePassBelowTheTarget() {
        // Arrange: where a slow bolt fired due south from the middle of the pitch range is late in its flight, which
        // the most depressed and the most raised shots both pass below
        float middlePitch = (BallistaAim.MIN_PITCH_DEGREES + BallistaAim.MAX_PITCH_DEGREES) / 2.0F;
        int lateTick = (int) (BallistaFiringSolution.MAX_FLIGHT.getTicks() * 4 / 5);
        Vec3 target = boltAfter(BallistaAim.facing(0.0F, middlePitch), SLOW_LAUNCH_SPEED, lateTick);
        double lowestPass = heightAt(BallistaAim.facing(0.0F, BallistaAim.MIN_PITCH_DEGREES), SLOW_LAUNCH_SPEED,
                target.z);
        double highestPass = heightAt(BallistaAim.facing(0.0F, BallistaAim.MAX_PITCH_DEGREES), SLOW_LAUNCH_SPEED,
                target.z);
        assertTrue(lowestPass < target.y && highestPass < target.y,
                "the ends of the pitch range no longer both pass below the target, so the case needs re-deriving");

        // Act
        Optional<BallistaFiringSolution> solution =
                BallistaFiringSolution.solve(target, Vec3.ZERO, SLOW_LAUNCH_SPEED);

        // Assert: a search that only brackets the ends of the pitch range finds no crossing here
        assertTrue(solution.isPresent(), "no solution for " + target);
        assertLands(solution.get(), SLOW_LAUNCH_SPEED, target, Vec3.ZERO);
    }

    @Test
    void solve_reportsNoSolution_whenTheFlightWouldOutlastTheLongestAllowed() {
        // Arrange: points a level shot passes just before and just after the longest flight
        BallistaAim level = BallistaAim.facing(0.0F, 0.0F);
        int longestFlight = (int) BallistaFiringSolution.MAX_FLIGHT.getTicks();
        Vec3 justInside = boltAfter(level, LAUNCH_SPEED, longestFlight - FLIGHT_BOUND_MARGIN_TICKS);
        Vec3 justOutside = boltAfter(level, LAUNCH_SPEED, longestFlight + FLIGHT_BOUND_MARGIN_TICKS);

        // Act
        Optional<BallistaFiringSolution> inside = BallistaFiringSolution.solve(justInside, Vec3.ZERO, LAUNCH_SPEED);
        Optional<BallistaFiringSolution> outside = BallistaFiringSolution.solve(justOutside, Vec3.ZERO, LAUNCH_SPEED);

        // Assert: no shot carries a bolt out faster than a level one, so only an unenforced bound solves the far point
        assertTrue(inside.isPresent(), "the bound refuses a flight shorter than it");
        assertTrue(outside.isEmpty(), "a flight longer than the bound was solved");
    }

    @Test
    void solve_reportsNoSolution_forATargetFartherThanAnyBoltCarries() {
        // Arrange: drag caps how far a bolt ever travels horizontally, whatever its pitch
        double farthestCarry = LAUNCH_SPEED / (1.0 - VANILLA_DRAG);
        Vec3 target = new Vec3(0.0, 0.0, 2.0 * farthestCarry);

        // Act
        Optional<BallistaFiringSolution> solution = BallistaFiringSolution.solve(target, Vec3.ZERO, LAUNCH_SPEED);

        // Assert: a solver that settles for the steepest shot when nothing reaches reports a miss as a solution
        assertTrue(solution.isEmpty(), "solved an unreachable target at " + target);
    }

    @Test
    void solve_reportsNoSolution_forATargetBelowTheLowestShot() {
        // Arrange: under where the most depressed shot passes, early in its flight
        Vec3 lowestPass = boltAfter(BallistaAim.facing(0.0F, BallistaAim.MIN_PITCH_DEGREES), LAUNCH_SPEED, 2);
        Vec3 target = lowestPass.add(0.0, -OUT_OF_RANGE_MARGIN_BLOCKS, 0.0);

        // Act
        Optional<BallistaFiringSolution> solution = BallistaFiringSolution.solve(target, Vec3.ZERO, LAUNCH_SPEED);

        // Assert: clamping the pitch to the floor of its range gives a shot that passes over the target
        assertTrue(solution.isEmpty(), "solved a target below the depression limit at " + target);
    }

    @Test
    void solve_reportsNoSolution_forATargetAboveTheHighestShot() {
        // Arrange: over where the most raised shot passes, early in its flight
        Vec3 highestPass = boltAfter(BallistaAim.facing(0.0F, BallistaAim.MAX_PITCH_DEGREES), LAUNCH_SPEED, 2);
        Vec3 target = highestPass.add(0.0, OUT_OF_RANGE_MARGIN_BLOCKS, 0.0);

        // Act
        Optional<BallistaFiringSolution> solution = BallistaFiringSolution.solve(target, Vec3.ZERO, LAUNCH_SPEED);

        // Assert: clamping the pitch to the top of its range gives a shot that passes under the target
        assertTrue(solution.isEmpty(), "solved a target above the elevation limit at " + target);
    }

    @Test
    void solve_reportsNoSolution_forATargetTheBoltStartsPast() {
        // Arrange: on the yaw axis, and in front of the machine short of the seated tip
        Vec3 levelTip = BallistaLaunchPoint.tipOffset(BallistaAim.facing(0.0F, 0.0F));
        Vec3[] targets = {new Vec3(0.0, levelTip.y, 0.0), new Vec3(0.0, levelTip.y, levelTip.z / 2.0)};

        for (Vec3 target : targets) {
            // Act
            Optional<BallistaFiringSolution> solution =
                    BallistaFiringSolution.solve(target, Vec3.ZERO, LAUNCH_SPEED);

            // Assert: a bolt cannot travel back to a point behind where it leaves
            assertTrue(solution.isEmpty(), "solved a target behind the tip at " + target);
        }
    }

    @Test
    void solve_reportsNoSolution_forATargetFleeingAsFastAsTheBolt() {
        // Arrange: the bolt only slows after it leaves, so it never closes on a target retreating at its launch speed
        Vec3 target = new Vec3(0.0, 0.0, 16.0);
        Vec3 velocity = new Vec3(0.0, 0.0, LAUNCH_SPEED);

        // Act
        Optional<BallistaFiringSolution> solution = BallistaFiringSolution.solve(target, velocity, LAUNCH_SPEED);

        // Assert: a lead that stops after a fixed number of passes aims at a point the target has already left
        assertTrue(solution.isEmpty(), "solved a target the bolt never catches");
    }

    private static void assertLands(BallistaFiringSolution solution, double launchSpeed, Vec3 target, Vec3 velocity) {
        String described = "yaw " + solution.getYaw() + " pitch " + solution.getPitch() + " at " + target;
        assertTrue(solution.getPitch() >= BallistaAim.MIN_PITCH_DEGREES
                && solution.getPitch() <= BallistaAim.MAX_PITCH_DEGREES, described + " is outside the pitch range");

        double miss = closestApproach(BallistaAim.facing(solution.getYaw(), solution.getPitch()), launchSpeed,
                target, velocity);
        assertTrue(miss <= HIT_TOLERANCE_BLOCKS, described + " misses by " + miss);
    }

    private static Vec3 onBearing(float bearingDegrees, double range, double height) {
        double bearing = Math.toRadians(bearingDegrees);
        return new Vec3(-Math.sin(bearing) * range, height, Math.cos(bearing) * range);
    }

    /**
     * The least distance between a bolt the machine fires on the given aim and a target moving at a steady velocity,
     * taken at the same moment for both.
     */
    private static double closestApproach(BallistaAim aim, double launchSpeed, Vec3 target, Vec3 velocity) {
        Vec3 position = BallistaLaunchPoint.tipOffset(aim);
        Vec3 boltVelocity = BallistaLaunchPoint.direction(aim).scale(launchSpeed);
        double closest = Double.POSITIVE_INFINITY;
        for (int tick = 0; tick < SIMULATED_TICKS; tick++) {
            Vec3 next = position.add(boltVelocity);

            // Within one tick both move in straight lines, so the gap between them changes linearly and its least
            // length over the tick has a closed form
            Vec3 gap = position.subtract(target.add(velocity.scale(tick)));
            Vec3 gapChange = next.subtract(position).subtract(velocity);
            double alongTick = Math.clamp(-gap.dot(gapChange) / gapChange.lengthSqr(), 0.0, 1.0);
            closest = Math.min(closest, gap.add(gapChange.scale(alongTick)).length());

            position = next;
            boltVelocity = nextVelocity(boltVelocity);
        }
        return closest;
    }

    /**
     * Where a bolt the machine fires on the given aim is after the given number of ticks.
     */
    private static Vec3 boltAfter(BallistaAim aim, double launchSpeed, int ticks) {
        Vec3 position = BallistaLaunchPoint.tipOffset(aim);
        Vec3 boltVelocity = BallistaLaunchPoint.direction(aim).scale(launchSpeed);
        for (int tick = 0; tick < ticks; tick++) {
            position = position.add(boltVelocity);
            boltVelocity = nextVelocity(boltVelocity);
        }
        return position;
    }

    /**
     * How high a bolt fired due south on the given aim is as it passes the given distance south.
     */
    private static double heightAt(BallistaAim aim, double launchSpeed, double southDistance) {
        Vec3 position = BallistaLaunchPoint.tipOffset(aim);
        Vec3 boltVelocity = BallistaLaunchPoint.direction(aim).scale(launchSpeed);
        for (int tick = 0; tick < SIMULATED_TICKS; tick++) {
            Vec3 next = position.add(boltVelocity);
            if (next.z >= southDistance) {
                double alongTick = (southDistance - position.z) / (next.z - position.z);
                return position.y + alongTick * (next.y - position.y);
            }
            position = next;
            boltVelocity = nextVelocity(boltVelocity);
        }
        return Double.NEGATIVE_INFINITY;
    }

    private static Vec3 nextVelocity(Vec3 velocity) {
        return new Vec3(velocity.x * VANILLA_DRAG, velocity.y * VANILLA_DRAG - VANILLA_GRAVITY,
                velocity.z * VANILLA_DRAG);
    }

}
