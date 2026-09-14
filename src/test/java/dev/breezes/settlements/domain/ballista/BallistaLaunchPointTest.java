package dev.breezes.settlements.domain.ballista;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaLaunchPointTest {

    // The machine's footprint is its own block, which reaches this far from the block's center on each horizontal axis
    private static final double FOOTPRINT_HALF_WIDTH_BLOCKS = 0.5;

    private static final float YAW_STEP_DEGREES = 5.0F;
    private static final float PITCH_STEP_DEGREES = 1.0F;

    private static final double TOLERANCE = 1.0E-6;

    @Test
    void tipOffset_liesOutsideTheMachinesFootprint_acrossTheWholeAimRange() {
        for (BallistaAim aim : everyAim()) {
            // Act
            Vec3 tip = BallistaLaunchPoint.tipOffset(aim);

            // Assert: a shot launched inside the machine's own block starts inside its collision box, which puts it in
            // the ground on its first tick
            double reach = Math.max(Math.abs(tip.x), Math.abs(tip.z));
            assertTrue(reach > FOOTPRINT_HALF_WIDTH_BLOCKS, describe(aim) + " launches from " + tip);
        }
    }

    @Test
    void direction_pointsWhereAnEntityFacingAlongTheAimLooks() {
        for (BallistaAim aim : everyAim()) {
            // Arrange: an entity facing along this aim, whose pitch grows as it looks down
            float entityYRot = aim.getYaw();
            float entityXRot = -aim.getPitch();

            // Act
            Vec3 direction = BallistaLaunchPoint.direction(aim);

            // Assert: a sign crossed on either axis mirrors the shot away from the heading an entity faced
            Vec3 look = lookOf(entityYRot, entityXRot);
            assertEquals(look.x, direction.x, TOLERANCE, describe(aim));
            assertEquals(look.y, direction.y, TOLERANCE, describe(aim));
            assertEquals(look.z, direction.z, TOLERANCE, describe(aim));
        }
    }

    @Test
    void tipOffset_liesStraightAheadOfTheMachine_alongTheLaunchDirection() {
        for (BallistaAim aim : everyAim()) {
            // Act
            Vec3 tip = BallistaLaunchPoint.tipOffset(aim);
            Vec3 direction = BallistaLaunchPoint.direction(aim);

            // Assert: seen from above, the tip lies on the line the shot leaves along and on its forward side, so the
            // bolt does not leave from beside or behind the barrel it flies out of
            double sideways = tip.x * direction.z - tip.z * direction.x;
            double ahead = tip.x * direction.x + tip.z * direction.z;
            assertEquals(0.0, sideways, TOLERANCE, describe(aim));
            assertTrue(ahead > 0.0, describe(aim) + " launches from behind the axis at " + tip);
        }
    }

    @Test
    void tipOffset_risesAsTheMuzzleRises() {
        for (BallistaAim aim : everyAim()) {
            if (aim.getPitch() + PITCH_STEP_DEGREES > BallistaAim.MAX_PITCH_DEGREES) {
                continue;
            }

            // Arrange
            BallistaAim raised = BallistaAim.facing(aim.getYaw(), aim.getPitch() + PITCH_STEP_DEGREES);

            // Act
            double tipHeight = BallistaLaunchPoint.tipOffset(aim).y;
            double raisedTipHeight = BallistaLaunchPoint.tipOffset(raised).y;

            // Assert: a tip that sinks as the muzzle rises launches the shot from below the barrel it is drawn along
            assertTrue(raisedTipHeight > tipHeight, describe(aim) + " raised by " + PITCH_STEP_DEGREES);
        }
    }

    @Test
    void tipOffset_turnsRigidlyAboutAPivotOnTheYawAxis_asTheMuzzleRises() {
        for (float yaw : everyYaw()) {
            // Arrange: the circle through the tip at the floor, the middle and the ceiling of the pitch range, drawn in
            // the vertical plane of the heading
            float middlePitch = (BallistaAim.MIN_PITCH_DEGREES + BallistaAim.MAX_PITCH_DEGREES) / 2.0F;
            double[] low = inHeadingPlane(yaw, BallistaAim.MIN_PITCH_DEGREES);
            double[] middle = inHeadingPlane(yaw, middlePitch);
            double[] high = inHeadingPlane(yaw, BallistaAim.MAX_PITCH_DEGREES);
            double[] center = circumcenter(low, middle, high);
            double radius = Math.hypot(low[0] - center[0], low[1] - center[1]);

            // Assert: a machine that turned about a point off its yaw axis, or did not turn rigidly at all, would
            // launch from somewhere the tip of the bolt drawn in its socket is not
            assertEquals(0.0, center[0], TOLERANCE, "yaw " + yaw + " turns about a point off the yaw axis");
            for (float pitch : everyPitch()) {
                double[] tip = inHeadingPlane(yaw, pitch);
                assertEquals(radius, Math.hypot(tip[0] - center[0], tip[1] - center[1]), TOLERANCE,
                        "yaw " + yaw + " pitch " + pitch + " leaves the circle");
            }
        }
    }

    /**
     * The tip at the given aim as its distance ahead along the heading, seen from above, and its height.
     */
    private static double[] inHeadingPlane(float yaw, float pitch) {
        Vec3 tip = BallistaLaunchPoint.tipOffset(BallistaAim.facing(yaw, pitch));
        Vec3 heading = lookOf(yaw, 0.0F);
        return new double[]{tip.x * heading.x + tip.z * heading.z, tip.y};
    }

    private static double[] circumcenter(double[] a, double[] b, double[] c) {
        double aSquared = a[0] * a[0] + a[1] * a[1];
        double bSquared = b[0] * b[0] + b[1] * b[1];
        double cSquared = c[0] * c[0] + c[1] * c[1];
        double divisor = 2.0 * (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1]));
        double x = (aSquared * (b[1] - c[1]) + bSquared * (c[1] - a[1]) + cSquared * (a[1] - b[1])) / divisor;
        double y = (aSquared * (c[0] - b[0]) + bSquared * (a[0] - c[0]) + cSquared * (b[0] - a[0])) / divisor;
        return new double[]{x, y};
    }

    /**
     * Where an entity with the given rotation looks: yRot 0 looks south and grows toward the west, and xRot grows as it
     * looks down, as vanilla turns a shooter's rotation into a shot's direction.
     */
    private static Vec3 lookOf(float yRotDegrees, float xRotDegrees) {
        double yRot = Math.toRadians(yRotDegrees);
        double xRot = Math.toRadians(xRotDegrees);
        return new Vec3(-Math.sin(yRot) * Math.cos(xRot), -Math.sin(xRot), Math.cos(yRot) * Math.cos(xRot));
    }

    /**
     * Every heading in the sweep, all the way around, at each pitch in the sweep.
     */
    private static List<BallistaAim> everyAim() {
        List<BallistaAim> aims = new ArrayList<>();
        for (float yaw : everyYaw()) {
            for (float pitch : everyPitch()) {
                aims.add(BallistaAim.facing(yaw, pitch));
            }
        }
        return aims;
    }

    private static List<Float> everyYaw() {
        List<Float> yaws = new ArrayList<>();
        for (float yaw = -180.0F; yaw < 180.0F; yaw += YAW_STEP_DEGREES) {
            yaws.add(yaw);
        }
        return yaws;
    }

    /**
     * Pitches from the floor of the range to its ceiling, both ends included.
     */
    private static List<Float> everyPitch() {
        List<Float> pitches = new ArrayList<>();
        for (float pitch = BallistaAim.MIN_PITCH_DEGREES; pitch < BallistaAim.MAX_PITCH_DEGREES;
             pitch += PITCH_STEP_DEGREES) {
            pitches.add(pitch);
        }
        pitches.add(BallistaAim.MAX_PITCH_DEGREES);
        return pitches;
    }

    private static String describe(BallistaAim aim) {
        return "yaw " + aim.getYaw() + " pitch " + aim.getPitch();
    }

}
