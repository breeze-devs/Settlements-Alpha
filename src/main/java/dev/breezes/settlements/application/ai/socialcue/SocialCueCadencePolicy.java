package dev.breezes.settlements.application.ai.socialcue;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Pure math utilities for desynchronizing villager social-cue cadences
 * <p>
 * A crowd of freshly loaded villagers would otherwise share the same
 * {@code nextAdmissionScanTick = 0} and identical fixed cooldowns, causing
 * every villager to act in lockstep. This class provides two independent
 * spread mechanisms:
 * <ul>
 *   <li>A per-villager initial scan-phase offset that staggers the first
 *       admission scan for a freshly loaded crowd.</li>
 *   <li>A CHA-weighted, jittered per-key cooldown so identical-charisma
 *       villagers still re-arm on different ticks after completing a cue.</li>
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SocialCueCadencePolicy {

    /**
     * Computes a per-key cue cooldown that varies by charisma and a random jitter so a crowd of
     * villagers that fired the same cue on the same tick re-arm on different ticks (no lockstep)
     * <p>
     * Charisma shortens the wait — sociable villagers act more often — mapping CHA in [0,1] from
     * the low-charisma multiplier to the high-charisma multiplier. A uniform jitter of
     * {@code ±jitterFraction} is then layered on so identical-charisma villagers still spread out.
     *
     * @param baseTicks              the nominal cue cooldown in ticks
     * @param charisma               the CHARISMA gene value in [0,1]
     * @param lowCharismaMultiplier  the multiplier at CHA = 0.0
     * @param highCharismaMultiplier the multiplier at CHA = 1.0
     * @param scaling                the curve used to interpolate between the multiplier endpoints
     * @param activityFactor         situation multiplier from the villager's current occasion
     * @param jitterFraction         the half-width of the uniform jitter band, e.g. 0.25 for ±25 %
     * @param random01               a uniform sample in [0,1) sourced from the villager's RandomSource
     * @return adjusted cooldown in ticks, never negative
     */
    public static long cooldownTicks(long baseTicks,
                                     double charisma,
                                     double lowCharismaMultiplier,
                                     double highCharismaMultiplier,
                                     SocialCueCooldownScaling scaling,
                                     double activityFactor,
                                     double jitterFraction,
                                     double random01) {
        double chaFactor = charismaFactor(charisma, lowCharismaMultiplier, highCharismaMultiplier, scaling);
        double boundedActivityFactor = Math.max(0.0, activityFactor);
        double jitterFactor = (1.0 - jitterFraction) + random01 * (2.0 * jitterFraction);
        return Math.max(0L, Math.round(baseTicks * chaFactor * boundedActivityFactor * jitterFactor));
    }

    private static double charismaFactor(double charisma,
                                         double lowCharismaMultiplier,
                                         double highCharismaMultiplier,
                                         SocialCueCooldownScaling scaling) {
        double boundedCharisma = Math.clamp(charisma, 0.0, 1.0);
        double boundedLowMultiplier = Math.max(0.0, lowCharismaMultiplier);
        double boundedHighMultiplier = Math.max(0.0, highCharismaMultiplier);

        if (scaling == SocialCueCooldownScaling.EXPONENTIAL
                && boundedLowMultiplier > 0.0 && boundedHighMultiplier > 0.0) {
            // Interpolate in log space so wide ranges behave proportionally instead of making
            // average-CHA villagers inherit most of the low-CHA penalty.
            return boundedLowMultiplier * Math.pow(boundedHighMultiplier / boundedLowMultiplier, boundedCharisma);
        }

        return boundedLowMultiplier + (boundedHighMultiplier - boundedLowMultiplier) * boundedCharisma;
    }

    /**
     * Maps a random sample to a per-villager initial scan-phase offset in [0, spreadTicks),
     * so a freshly loaded crowd does not all run their first admission scan on the same tick
     *
     * @param spreadTicks the maximum phase offset window in ticks
     * @param random01    a uniform sample in [0,1) sourced from the villager's RandomSource
     * @return an offset in [0, spreadTicks)
     */
    public static long initialScanPhaseTicks(long spreadTicks, double random01) {
        return (long) (random01 * spreadTicks);
    }

}
