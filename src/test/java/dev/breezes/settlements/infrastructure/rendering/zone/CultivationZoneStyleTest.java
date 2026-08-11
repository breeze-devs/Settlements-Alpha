package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.shared.util.ArgbColorUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts the relationships between the colors, not their literal channel values (F3, T2) — what has
 * to hold is that "valid" always reads green-dominant, "invalid" always reads red-dominant, and the
 * prospective color commits to neither, not which exact shade any of the three currently is.
 */
class CultivationZoneStyleTest {

    private static final float CHANNEL_TOLERANCE = 0.001F;

    @Test
    void hudAccentColor_valid_readsGreenDominant() {
        int color = CultivationZoneStyle.hudAccentColor(true);

        assertTrue(ArgbColorUtil.green(color) > ArgbColorUtil.red(color));
    }

    @Test
    void hudAccentColor_invalid_readsRedDominant() {
        int color = CultivationZoneStyle.hudAccentColor(false);

        assertTrue(ArgbColorUtil.red(color) > ArgbColorUtil.green(color));
    }

    @Test
    void outlineColor_valid_readsGreenDominant() {
        var rgb = CultivationZoneStyle.outlineColor(true);

        assertTrue(rgb.y() > rgb.x());
    }

    @Test
    void outlineColor_invalid_readsRedDominant() {
        var rgb = CultivationZoneStyle.outlineColor(false);

        assertTrue(rgb.x() > rgb.y());
    }

    @Test
    void prospectiveOutlineColor_favorsNeitherRedNorGreen() {
        var rgb = CultivationZoneStyle.prospectiveOutlineColor();

        // A prospective zone has not been checked, so a color leaning either way would read as a
        // verdict the preview never computed — this is the property, not the specific shade.
        assertEquals(rgb.x(), rgb.y(), CHANNEL_TOLERANCE);
    }

}
