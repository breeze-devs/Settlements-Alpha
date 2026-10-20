package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Fades the color of HUD text without crossing the font renderer's alpha floor (platform standard P12).
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class HudTextColor {

    private static final int MIN_HONORED_ALPHA_BYTE = 4;

    /**
     * Scales a packed ARGB color's alpha by a fraction from zero to one, preserving RGB.
     * Clamps to {@link #MIN_HONORED_ALPHA_BYTE}; callers must skip drawing fully hidden content.
     */
    static int withAlpha(int argb, float alphaFraction) {
        int scaled = Math.round(((argb >>> 24) & 0xFF) * alphaFraction);
        int alpha = Math.max(MIN_HONORED_ALPHA_BYTE, scaled);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

}
