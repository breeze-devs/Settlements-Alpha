package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A ballista's firing angles in degrees, following {@link BallistaAim}'s conventions and pitch limits.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BallistaFiringSolution {

    /**
     * Maximum flight time. Longer flights make constant-velocity target predictions less reliable.
     */
    static final ClockTicks MAX_FLIGHT = ClockTicks.seconds(2);

    // Model movement, then drag, then gravity to match the bolt's air physics. The float literal preserves vanilla's
    // rounding when widened to double
    private static final double DRAG_PER_TICK = 0.99F;
    private static final double GRAVITY_PER_TICK = 0.05;

    private static final double TERMINAL_VERTICAL_SPEED = -GRAVITY_PER_TICK / (1.0 - DRAG_PER_TICK);

    private static final float PITCH_SCAN_STEP_DEGREES = 2.5F;
    // Limit refinement cost; a fixed pitch tolerance does not guarantee the same accuracy at every speed and distance
    private static final int PITCH_BISECTIONS = 5;

    private static final int MAX_LEAD_ITERATIONS = 10;
    // Measure convergence as a change in target position. A time tolerance would react to limited pitch precision
    // even when the predicted position barely changes, and would require an extra pass for a stationary target
    private static final double LEAD_SETTLED_BLOCKS = 0.05;

    private final float yaw;
    private final float pitch;

    /**
     * Finds an aim that intercepts a target moving at constant velocity.
     * <p>
     * Assumes the bolt launches from its seated tip position and travels through air. Does not check for obstacles.
     *
     * @param target         target position at launch, in blocks on world axes relative to the center of the
     *                       machine block's floor
     * @param targetVelocity target velocity in blocks per tick on world axes
     * @param launchSpeed    initial bolt speed in blocks per tick; must be positive
     * @return empty when no aim within the pitch range reaches the target within {@link #MAX_FLIGHT}, or when the
     * target lead does not converge
     * @throws IllegalArgumentException if launchSpeed is not positive
     */
    public static Optional<BallistaFiringSolution> solve(@Nonnull Vec3 target,
                                                         @Nonnull Vec3 targetVelocity,
                                                         double launchSpeed) {
        Validate.isTrue(launchSpeed > 0.0, "launchSpeed must be positive, got %s", launchSpeed);

        // Flight time determines target lead, but leading the target changes flight time. Iterate until the predicted
        // intercept position stabilizes
        double targetSpeed = targetVelocity.length();
        double flightTicks = 0.0;
        for (int i = 0; i < MAX_LEAD_ITERATIONS; i++) {
            Shot shot = Sighting.of(target.add(targetVelocity.scale(flightTicks)), launchSpeed).flattestShot();
            if (shot == null) {
                // TODO: account for moving targets that start outside reach but enter it during the bolt's flight
                return Optional.empty();
            }
            if (Math.abs(shot.flightTicks() - flightTicks) * targetSpeed < LEAD_SETTLED_BLOCKS) {
                return shot.flightTicks() <= MAX_FLIGHT.getTicks()
                        ? Optional.of(new BallistaFiringSolution(shot.yaw(), shot.pitch()))
                        : Optional.empty();
            }
            flightTicks = shot.flightTicks();
        }

        return Optional.empty();
    }

    /**
     * Returns a new solution with the given angular offsets, wrapping yaw and clamping pitch to the allowed range.
     */
    BallistaFiringSolution deviatedBy(double yawDegrees, double pitchDegrees) {
        return new BallistaFiringSolution(BallistaAim.wrapDegrees((float) (this.yaw + yawDegrees)),
                BallistaAim.clampPitch((float) (this.pitch + pitchDegrees)));
    }

    /**
     * Displacement in blocks after the given number of whole ticks under air drag alone.
     */
    private static double travelled(double speed, int ticks) {
        return speed * (1.0 - Math.pow(DRAG_PER_TICK, ticks)) / (1.0 - DRAG_PER_TICK);
    }

    /**
     * Vertical displacement in blocks after the given number of whole ticks, including air drag and gravity.
     */
    private static double risen(double verticalSpeed, int ticks) {
        // Subtracting terminal velocity reduces the remaining motion to the same geometric series as drag alone
        return ticks * TERMINAL_VERTICAL_SPEED + travelled(verticalSpeed - TERMINAL_VERTICAL_SPEED, ticks);
    }

    private record Shot(float yaw, float pitch, double flightTicks) {
    }

    /**
     * Vertical clearance in blocks at the target's horizontal distance, with flight time in ticks.
     * Negative clearance means the bolt is below the target.
     */
    private record Pass(double clearance, double flightTicks) {

        private static final Pass NEVER_ARRIVES = new Pass(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        boolean isBelow() {
            return this.clearance < 0.0;
        }

    }

    /**
     * A target's yaw, horizontal distance, and height relative to the machine's floor center, with a bolt launch speed.
     */
    private record Sighting(float yaw, double distance, double height, double launchSpeed) {

        static Sighting of(Vec3 point, double launchSpeed) {
            // The launch point stays in the vertical plane through the yaw axis and target. With no lateral
            // acceleration, pitch does not change the yaw needed to reach the target
            return new Sighting(BallistaLaunchPoint.headingToward(point), Math.hypot(point.x, point.z), point.y,
                    launchSpeed);
        }

        /**
         * Returns the lowest-pitch shot that reaches the point, or null if none is within the pitch limits.
         */
        @Nullable
        Shot flattestShot() {
            // Height at the target can peak below the maximum pitch, especially at low launch speeds. Scan before
            // bisecting so equal signs at the range endpoints do not rule out a reachable shot
            float lowPitch = BallistaAim.MIN_PITCH_DEGREES;
            Pass low = this.passAt(lowPitch);
            int steps = (int) Math.ceil(
                    (BallistaAim.MAX_PITCH_DEGREES - BallistaAim.MIN_PITCH_DEGREES) / PITCH_SCAN_STEP_DEGREES);
            for (int step = 1; step <= steps; step++) {
                float highPitch = Math.min(BallistaAim.MIN_PITCH_DEGREES + step * PITCH_SCAN_STEP_DEGREES,
                        BallistaAim.MAX_PITCH_DEGREES);
                Pass high = this.passAt(highPitch);
                // TODO: handle exact endpoints and crossings between scan samples so reachable shots are not missed
                if (low != null && high != null && low.isBelow() != high.isBelow()) {
                    return this.bisect(lowPitch, highPitch, low.isBelow());
                }
                lowPitch = highPitch;
                low = high;
            }
            return null;
        }

        @Nullable
        private Shot bisect(float lowPitch, float highPitch, boolean lowIsBelow) {
            for (int i = 0; i < PITCH_BISECTIONS; i++) {
                float middlePitch = (lowPitch + highPitch) / 2.0F;
                Pass middle = this.passAt(middlePitch);
                if (middle == null) {
                    return null;
                }
                if (middle.isBelow() == lowIsBelow) {
                    lowPitch = middlePitch;
                } else {
                    highPitch = middlePitch;
                }
            }

            float pitch = (lowPitch + highPitch) / 2.0F;
            Pass pass = this.passAt(pitch);
            return pass == null ? null : new Shot(this.yaw, pitch, pass.flightTicks());
        }

        /**
         * Returns the bolt's vertical clearance and flight time at the target's horizontal distance.
         * Returns null when the target is at or behind the launch tip, or {@link Pass#NEVER_ARRIVES} when drag prevents
         * the bolt from reaching that distance.
         */
        @Nullable
        private Pass passAt(float pitch) {
            // Pitch changes the launch position, so each candidate pitch needs its own tip offset
            BallistaAim aim = BallistaAim.facing(this.yaw, pitch);
            Vec3 tip = BallistaLaunchPoint.tipOffset(aim);
            Vec3 direction = BallistaLaunchPoint.direction(aim);
            double horizontal = direction.horizontalDistance();
            double run = this.distance - (tip.x * direction.x + tip.z * direction.z) / horizontal;
            if (run <= 0.0) {
                return null;
            }

            // Air drag caps horizontal travel. Beyond that limit there is no finite arrival time
            double horizontalSpeed = this.launchSpeed * horizontal;
            double speedLeft = 1.0 - run * (1.0 - DRAG_PER_TICK) / horizontalSpeed;
            if (speedLeft <= 0.0) {
                // Use negative clearance so an unreachable pitch can bracket a reachable one
                return Pass.NEVER_ARRIVES;
            }

            // Collision checks use straight segments between tick positions, so interpolate within the arrival tick.
            // A continuous ballistic curve would give a different height
            double flightTicks = Math.log(speedLeft) / Math.log(DRAG_PER_TICK);
            int wholeTicks = (int) flightTicks;
            double segmentStart = travelled(horizontalSpeed, wholeTicks);
            double alongSegment = (run - segmentStart) / (travelled(horizontalSpeed, wholeTicks + 1) - segmentStart);

            double verticalSpeed = this.launchSpeed * direction.y;
            double riseAtStart = risen(verticalSpeed, wholeTicks);
            double rise = riseAtStart + alongSegment * (risen(verticalSpeed, wholeTicks + 1) - riseAtStart);
            return new Pass(tip.y + rise - this.height, wholeTicks + alongSegment);
        }

    }

}
