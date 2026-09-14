package dev.breezes.settlements.domain.ballista;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;

/**
 * Where a ballista has been told to point (its intent), and where it points now.
 * Ballista turns toward the intent by at most a fixed rate on each axis.
 * <p>
 * Angles are in degrees and describe the world:
 * - Yaw 0 points the muzzle south and yaw grows clockwise seen from above (90 is west, ±180 is north, -90 is east).
 * - Pitch 0 is level, and a positive pitch raises the muzzle.
 * <p>
 * Every aim holds its yaws wrapped to [-180, 180) and its pitches within [MIN_PITCH_DEGREES, MAX_PITCH_DEGREES], so
 * one heading has exactly one representation.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BallistaAim {

    /**
     * The lowest the muzzle may point below level.
     */
    public static final float MIN_PITCH_DEGREES = -15.0F;

    /**
     * The highest the muzzle may point above level.
     */
    public static final float MAX_PITCH_DEGREES = 25.0F;

    /**
     * The spacing between manually selected headings.
     */
    public static final float MANUAL_YAW_STEP_DEGREES = 5.0F;

    private static final float FULL_TURN_DEGREES = 360.0F;
    private static final float HALF_TURN_DEGREES = 180.0F;

    private final float intentYaw;
    private final float intentPitch;
    private final float yaw;
    private final float pitch;

    /**
     * An aim at rest on the given heading.
     */
    public static BallistaAim facing(float yaw, float pitch) {
        float heading = wrapDegrees(yaw);
        float elevation = clampPitch(pitch);
        return new BallistaAim(heading, elevation, heading, elevation);
    }

    /**
     * A level aim at rest on the nearest manual yaw increment. Halfway headings round toward increasing yaw.
     */
    public static BallistaAim forPlacement(float yaw) {
        // A player's yaw can accumulate full turns; normalize before rounding to keep the step count bounded.
        float snappedYaw = Math.round(wrapDegrees(yaw) / MANUAL_YAW_STEP_DEGREES) * MANUAL_YAW_STEP_DEGREES;
        return facing(snappedYaw, 0.0F);
    }

    /**
     * This aim told to point somewhere new, still pointing where it points now.
     *
     * @return this same instance when the intent does not change
     */
    public BallistaAim withIntent(float yaw, float pitch) {
        float heading = wrapDegrees(yaw);
        float elevation = clampPitch(pitch);
        if (heading == this.intentYaw && elevation == this.intentPitch) {
            return this;
        }

        return new BallistaAim(heading, elevation, this.yaw, this.pitch);
    }

    /**
     * One tick of turning. Each axis moves toward the intent by at most its rate.
     * Yaw turns whichever way around is shorter.
     *
     * @param maxYawDegreesPerTick   the furthest yaw may turn in this step; must be positive
     * @param maxPitchDegreesPerTick the furthest pitch may turn in this step; must be positive
     * @return this same instance when the aim is already at rest
     */
    public BallistaAim step(float maxYawDegreesPerTick, float maxPitchDegreesPerTick) {
        if (this.isAtRest()) {
            return this;
        }

        float yawRemaining = wrapDegrees(this.intentYaw - this.yaw);
        float nextYaw = Math.abs(yawRemaining) <= maxYawDegreesPerTick
                ? this.intentYaw
                : wrapDegrees(this.yaw + Math.copySign(maxYawDegreesPerTick, yawRemaining));

        float pitchRemaining = this.intentPitch - this.pitch;
        float nextPitch = Math.abs(pitchRemaining) <= maxPitchDegreesPerTick
                ? this.intentPitch
                : this.pitch + Math.copySign(maxPitchDegreesPerTick, pitchRemaining);

        return new BallistaAim(this.intentYaw, this.intentPitch, nextYaw, nextPitch);
    }

    /**
     * Whether the machine points exactly where it has been told to.
     */
    public boolean isAtRest() {
        return this.yaw == this.intentYaw && this.pitch == this.intentPitch;
    }

    /**
     * The larger of the yaw and pitch angles between this aim's intent and the other's, with yaw measured the short
     * way around.
     */
    public float intentDeviationFrom(@Nonnull BallistaAim other) {
        float yawDeviation = Math.abs(wrapDegrees(this.intentYaw - other.intentYaw));
        float pitchDeviation = Math.abs(this.intentPitch - other.intentPitch);
        return Math.max(yawDeviation, pitchDeviation);
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % FULL_TURN_DEGREES;
        if (wrapped >= HALF_TURN_DEGREES) {
            return wrapped - FULL_TURN_DEGREES;
        }
        if (wrapped < -HALF_TURN_DEGREES) {
            return wrapped + FULL_TURN_DEGREES;
        }
        return wrapped;
    }

    private static float clampPitch(float pitch) {
        return Math.clamp(pitch, MIN_PITCH_DEGREES, MAX_PITCH_DEGREES);
    }

}
