package dev.breezes.settlements.infrastructure.minecraft.items;

import dev.breezes.settlements.shared.util.ArgbColorUtil;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TotemModeStyleTest {

    @Test
    void hoverOutline_sharesNoColorWithAnyModesRestingOutline() {
        // Arrange, Act & Assert: the hovered villager is told apart by hue alone, so a hover color that
        // collided with the mode a player happens to be holding would leave that mode with no aiming signal
        // at all. Only a collision is detectable here — whether two distinct colors read apart in motion is
        // an in-game judgment, not a unit-testable one.
        for (TotemMode mode : TotemMode.values()) {
            assertNotEquals(TotemModeStyle.restingOutlineColor(mode), TotemModeStyle.hoverOutlineColor(),
                    mode + " resting outline collides with the hover outline");
        }
    }

    @Test
    void everyOutline_sharesOneAlpha() {
        // Arrange: the hover color is the reference, since it is the one outline that is not mode-derived.
        int expectedAlpha = ArgbColorUtil.alpha(TotemModeStyle.hoverOutlineColor());

        // Act & Assert: opacity was tried as the resting/hover signal and rejected for being unreadable in
        // game. Reintroducing a per-state or per-mode alpha would quietly restage that failure, so alpha is
        // pinned uniform and hue is left to carry the whole distinction.
        for (TotemMode mode : TotemMode.values()) {
            assertEquals(expectedAlpha, ArgbColorUtil.alpha(TotemModeStyle.restingOutlineColor(mode)),
                    mode + " resting outline alpha diverges from the shared outline alpha");
        }
    }

    @Test
    void restingOutline_isDistinctAcrossEveryMode() {
        // Arrange
        TotemMode[] modes = TotemMode.values();

        // Act & Assert: two modes sharing a resting outline color would make cycling invisible in the world,
        // defeating the entire point of a mode-colored outline.
        for (int i = 0; i < modes.length; i++) {
            for (int j = i + 1; j < modes.length; j++) {
                assertNotEquals(TotemModeStyle.restingOutlineColor(modes[i]), TotemModeStyle.restingOutlineColor(modes[j]),
                        modes[i] + " and " + modes[j] + " share a resting outline color");
            }
        }
    }

    @Test
    void restingOutline_matchesRawHueChannels_forEveryMode() {
        // Arrange, Act & Assert: the mode's own hue is the single authority for its color, so no surface that
        // draws it may deviate from it. A per-mode adjustment reintroduced here — a legibility tweak applied
        // at the surface rather than to the hue — would put one mode's outline and its particles on colors
        // that no longer match, which is the drift this pins down. Legibility is answered by retuning the
        // hue itself, where every surface picks the change up at once.
        for (TotemMode mode : TotemMode.values()) {
            Vector3f rawHue = mode.hue();
            int rawPacked = ArgbColorUtil.pack(rawHue.x(), rawHue.y(), rawHue.z(), 1.0F);
            int displayed = TotemModeStyle.restingOutlineColor(mode);

            assertEquals(ArgbColorUtil.red(rawPacked), ArgbColorUtil.red(displayed), mode + " red channel");
            assertEquals(ArgbColorUtil.green(rawPacked), ArgbColorUtil.green(displayed), mode + " green channel");
            assertEquals(ArgbColorUtil.blue(rawPacked), ArgbColorUtil.blue(displayed), mode + " blue channel");
        }
    }

}
