package dev.breezes.settlements.presentation.ui.framework;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * Centralized color constants for consistent UI appearance.
 * Immutable — create a new instance for theme overrides or use {@link #DEFAULT}.
 */
@ClientSide
public record UITheme(
        int panelColor,
        int borderLight,
        int borderDark,
        int rowColor,
        int rowAltColor,
        int textColor,
        int subtleTextColor,
        int successColor,
        int errorColor,
        int warningColor,
        int progressColor,
        int backgroundDimColor,
        int overlayColor
) {

    public static final UITheme DEFAULT = new UITheme(
            0xFF1E1E1E,   // panelColor
            0xFF555555,   // borderLight
            0xFF0F0F0F,   // borderDark
            0xFF2B2B2B,   // rowColor
            0xFF252525,   // rowAltColor
            0xFFE0E0E0,   // textColor
            0xFFA0A0A0,   // subtleTextColor
            0xFF55FF55,   // successColor
            0xFFFF5555,   // errorColor
            0xFFFFA500,   // warningColor
            0xFFFFAA00,   // progressColor
            0x50000000,   // backgroundDimColor
            0xCC000000    // overlayColor
    );

}
