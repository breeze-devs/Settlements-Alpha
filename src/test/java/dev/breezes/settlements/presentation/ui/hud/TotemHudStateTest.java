package dev.breezes.settlements.presentation.ui.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TotemHudStateTest {

    @Test
    void resolve_noTarget_returnsNoTarget() {
        assertEquals(TotemHudState.NO_TARGET, TotemHudState.resolve(false, true));
    }

    @Test
    void resolve_noTarget_ignoresEligibilityFlag() {
        // A caller with no crosshair target must not read the eligibility flag at all — dropping the
        // absence check first would let this case fall through to ELIGIBLE_TARGET.
        assertEquals(TotemHudState.NO_TARGET, TotemHudState.resolve(false, false));
    }

    @Test
    void resolve_ineligibleTarget_returnsIneligibleTarget() {
        assertEquals(TotemHudState.INELIGIBLE_TARGET, TotemHudState.resolve(true, false));
    }

    @Test
    void resolve_eligibleTarget_returnsEligibleTarget() {
        assertEquals(TotemHudState.ELIGIBLE_TARGET, TotemHudState.resolve(true, true));
    }

}
