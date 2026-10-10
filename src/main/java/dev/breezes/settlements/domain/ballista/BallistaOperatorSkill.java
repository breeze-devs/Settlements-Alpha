package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.entities.Expertise;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;

/**
 * A villager's ballista accuracy and loading time.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BallistaOperatorSkill {

    // Multipliers at the highest gene value or master tier. A value of 1 leaves the baseline unchanged
    private static final double HIGHEST_INTELLIGENCE_AIM_ERROR_RATIO = 0.5;
    private static final double MASTER_AIM_ERROR_RATIO = 0.25;
    private static final double HIGHEST_AGILITY_LOAD_RATIO = 0.5;
    private static final double MASTER_LOAD_RATIO = 0.5;

    // Baselines for a novice with the lowest intelligence and agility
    private static final double LEAST_LEAD_QUALITY = 0.25;
    private static final ClockTicks SLOWEST_LOAD = ClockTicks.seconds(3);

    /**
     * Maximum yaw/pitch offset radius in degrees, before pitch clamping. Non-negative.
     */
    private final double scatterDegrees;

    /**
     * Fraction of target velocity used for aiming. 0 ignores movement; 1 uses the full velocity.
     */
    private final double leadQuality;

    /**
     * Time needed to load a bolt. At least one tick.
     */
    private final ClockTicks loadDuration;

    /**
     * Calculates skill from the villager's genes and trade tier.
     *
     * @param maxScatterDegrees non-negative scatter radius for a novice with minimum intelligence
     */
    public static BallistaOperatorSkill of(@Nonnull GeneticsProfile genetics,
                                           @Nonnull Expertise expertise,
                                           double maxScatterDegrees) {
        Validate.isTrue(maxScatterDegrees >= 0.0, "maxScatterDegrees must not be negative, got %s", maxScatterDegrees);

        double tier = (double) (expertise.getLevel() - Expertise.NOVICE.getLevel())
                / (Expertise.MASTER.getLevel() - Expertise.NOVICE.getLevel());
        // Multiplying keeps gene bonuses useful at every expertise level.
        // Scatter and lead error use the same factor because both measure aiming accuracy
        double aimError = towards(HIGHEST_INTELLIGENCE_AIM_ERROR_RATIO, genetics.getGeneValue(GeneType.INTELLIGENCE))
                * towards(MASTER_AIM_ERROR_RATIO, tier);
        double loadRatio = towards(HIGHEST_AGILITY_LOAD_RATIO, genetics.getGeneValue(GeneType.AGILITY))
                * towards(MASTER_LOAD_RATIO, tier);

        double scatterDegrees = maxScatterDegrees * aimError;
        double leadQuality = 1.0 - (1.0 - LEAST_LEAD_QUALITY) * aimError;
        // Small multipliers must not round the load time down to zero
        long loadTicks = Math.max(1L, Math.round(SLOWEST_LOAD.getTicks() * loadRatio));
        return new BallistaOperatorSkill(scatterDegrees, leadQuality, ClockTicks.of(loadTicks));
    }

    /**
     * Returns the supplied aim with scatter applied and pitch clamped to the allowed range.
     * <p>
     * Reusing the draws produces the same offset before pitch clamping.
     *
     * @param directionDraw a uniform draw in [0, 1] for the offset direction
     * @param distanceDraw  an independent uniform draw in [0, 1]; 0 gives no offset, 1 gives the maximum radius
     *                      before pitch clamping
     */
    public BallistaFiringSolution scatter(@Nonnull BallistaFiringSolution solution,
                                          double directionDraw,
                                          double distanceDraw) {
        Validate.inclusiveBetween(0.0, 1.0, directionDraw, "directionDraw must lie in [0, 1]");
        Validate.inclusiveBetween(0.0, 1.0, distanceDraw, "distanceDraw must lie in [0, 1]");

        // Disk area grows with radius squared, so the square root spreads yaw/pitch offsets evenly over the disk
        double stray = this.scatterDegrees * Math.sqrt(distanceDraw);
        double direction = 2.0 * Math.PI * directionDraw;
        return solution.deviatedBy(stray * Math.cos(direction), stray * Math.sin(direction));
    }

    /**
     * Linearly interpolates from 1 to ratioAtFull as progress goes from 0 to 1.
     */
    private static double towards(double ratioAtFull, double progress) {
        return 1.0 + (ratioAtFull - 1.0) * progress;
    }

}
