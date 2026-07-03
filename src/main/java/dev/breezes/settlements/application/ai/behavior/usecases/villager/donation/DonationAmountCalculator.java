package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

import dev.breezes.settlements.domain.genetics.GeneticMultiplierResolver;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Pure arithmetic for the need-based donation ceremony.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DonationAmountCalculator {

    /**
     * The safe cap bounds a donation from both ends: it must not lift the recipient past the
     * floor, and it must not drop the donor below their own comfort cushion.
     */
    public static int cap(int floor, int recipientBalance, int donorBalance, int comfort) {
        return Math.min(floor - recipientBalance, donorBalance - comfort);
    }

    /**
     * Draws a uniform amount within the safe cap
     */
    public static int pick(int cap, int minDonation) {
        if (cap < minDonation) {
            throw new IllegalArgumentException("cap (%d) must be >= minDonation (%d)".formatted(cap, minDonation));
        }
        return RandomUtil.randomInt(minDonation, cap, true);
    }

    /**
     * Higher CHARISMA lowers the comfort cushion (negative impact) so generous villagers donate sooner
     */
    public static int comfort(double charisma, int baseComfort, double comfortImpact, int floor) {
        double multiplier = GeneticMultiplierResolver.centeredMultiplier(charisma, -comfortImpact);
        int computed = (int) Math.round(baseComfort * multiplier);
        return Math.max(floor, computed);
    }

    /**
     * Higher CHARISMA raises the chance that a given eligible tick actually donates
     */
    public static double chance(double charisma, double baseChance, double chanceImpact) {
        double multiplier = GeneticMultiplierResolver.centeredMultiplier(charisma, chanceImpact);
        return RandomUtil.clamp(baseChance * multiplier, 0.0, 1.0);
    }

}
