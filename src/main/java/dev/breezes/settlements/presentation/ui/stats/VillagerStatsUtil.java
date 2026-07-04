package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.domain.entities.Expertise;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Pure utility methods for villager stats rendering — extracted from VillagerStatsScreen
 * to allow unit testing without Minecraft class loading dependencies.
 */
final class VillagerStatsUtil {

    private static final String PROFESSION_KEY_PREFIX = "entity.settlements.base_villager.";
    private static final String EXPERTISE_KEY_PREFIX = "merchant.level.";

    private static final String REPUTATION_HOSTILE_KEY = "ui.settlements.stats.reputation.hostile";
    private static final String REPUTATION_UNFRIENDLY_KEY = "ui.settlements.stats.reputation.unfriendly";
    private static final String REPUTATION_NEUTRAL_KEY = "ui.settlements.stats.reputation.neutral";
    private static final String REPUTATION_FRIENDLY_KEY = "ui.settlements.stats.reputation.friendly";
    private static final String REPUTATION_HONORED_KEY = "ui.settlements.stats.reputation.honored";
    private static final String REPUTATION_EXALTED_KEY = "ui.settlements.stats.reputation.exalted";

    static boolean isUnemployed(@Nonnull String professionKey) {
        String suffix = professionKeySuffix(professionKey);
        return "none".equals(suffix) || "nitwit".equals(suffix);
    }

    /**
     * Extracts the local name after the namespace colon (e.g., {@code "minecraft:farmer"} → {@code "farmer"}).
     * Returns the full key unchanged if no colon is present.
     */
    static String professionKeySuffix(@Nonnull String professionKey) {
        int colonIndex = professionKey.lastIndexOf(':');
        return colonIndex >= 0 ? professionKey.substring(colonIndex + 1) : professionKey;
    }

    /**
     * Builds the translation key for a villager's profession label, reusing the mod's own profession
     * names (e.g. {@code "minecraft:farmer"} → {@code "entity.settlements.base_villager.farmer"}) so the
     * label localizes consistently with the villager's chat prefix.
     */
    static String professionTranslationKey(@Nonnull String professionKey) {
        return PROFESSION_KEY_PREFIX + professionKeySuffix(professionKey);
    }

    /**
     * Builds the vanilla expertise translation key for a villager level (e.g. {@code 1} →
     * {@code "merchant.level.1"} → "Novice"). Reuses Minecraft's own strings so every shipped language
     * localizes for free. Returns empty for levels outside the defined 1–5 range.
     */
    static Optional<String> expertiseTranslationKey(int level) {
        try {
            return Optional.of(EXPERTISE_KEY_PREFIX + Expertise.fromLevel(level).getLevel());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // TODO: move this out
    static String getReputationTitleKey(int reputation) {
        if (reputation <= -50) return REPUTATION_HOSTILE_KEY;
        if (reputation <= -10) return REPUTATION_UNFRIENDLY_KEY;
        if (reputation < 10) return REPUTATION_NEUTRAL_KEY;
        if (reputation < 50) return REPUTATION_FRIENDLY_KEY;
        if (reputation < 100) return REPUTATION_HONORED_KEY;
        return REPUTATION_EXALTED_KEY;
    }

}
