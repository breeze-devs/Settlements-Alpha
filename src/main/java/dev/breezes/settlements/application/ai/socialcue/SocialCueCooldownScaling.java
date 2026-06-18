package dev.breezes.settlements.application.ai.socialcue;

import java.util.Locale;

/**
 * Supported curves for mapping CHARISMA onto a social-cue cooldown multiplier.
 */
public enum SocialCueCooldownScaling {

    LINEAR,
    EXPONENTIAL;

    /**
     * Parses a server config string, falling back to exponential for unknown values.
     * Unknown values should not crash world load for a soft tuning knob; exponential is the
     * gentler curve for wide multiplier ranges such as 4.0x to 0.5x.
     */
    public static SocialCueCooldownScaling fromConfig(String value) {
        if (value == null) {
            return EXPONENTIAL;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "linear" -> LINEAR;
            case "exponential" -> EXPONENTIAL;
            default -> EXPONENTIAL;
        };
    }

}
