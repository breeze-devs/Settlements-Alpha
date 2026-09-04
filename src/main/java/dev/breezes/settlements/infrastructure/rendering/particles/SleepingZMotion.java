package dev.breezes.settlements.infrastructure.rendering.particles;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Flight curve of one sleeping "Z": a widening spiral upward that grows and fades as it climbs.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SleepingZMotion {

    private static final double SWAY_TURNS = 1.4;
    private static final double SWAY_RADIUS = 0.09;
    private static final double TOTAL_RISE = 0.7;

    private static final float START_SIZE = 0.07f;
    private static final float END_SIZE = 0.15f;

    private static final float FADE_IN_END = 0.12f;
    private static final float FADE_OUT_START = 0.5f;

    /**
     * Samples the curve.
     *
     * @param progress  fraction of the particle's lifetime elapsed, in [0, 1)
     * @param swayPhase where in the sway turn this particle starts, in radians
     */
    public static Frame frameAt(float progress, double swayPhase) {
        // Ease-out on rise and growth together
        double eased = 1.0 - (1.0 - progress) * (1.0 - progress);

        // Amplitude ramps from zero so every Z leaves the head at the same point and spreads later
        double amplitude = SWAY_RADIUS * progress;
        double swayAngle = swayPhase + progress * SWAY_TURNS * 2.0 * Math.PI;

        return new Frame(amplitude * Math.sin(swayAngle), amplitude * Math.cos(swayAngle),
                TOTAL_RISE * eased, (float) (START_SIZE + (END_SIZE - START_SIZE) * eased),
                alphaAt(progress));
    }

    private static float alphaAt(float progress) {
        if (progress < FADE_IN_END) {
            return progress / FADE_IN_END;
        }
        if (progress > FADE_OUT_START) {
            return 1.0f - (progress - FADE_OUT_START) / (1.0f - FADE_OUT_START);
        }
        return 1.0f;
    }

    /**
     * One sampled frame of the curve.
     */
    public record Frame(double lateralX, double lateralZ, double rise, float size, float alpha) {
    }

}
