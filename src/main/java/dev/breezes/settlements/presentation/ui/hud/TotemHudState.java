package dev.breezes.settlements.presentation.ui.hud;

/**
 * Which of the totem HUD's three states applies, decided from plain booleans so the branch is testable
 * without a crosshair, a villager, or any other Minecraft type.
 */
enum TotemHudState {

    NO_TARGET,
    INELIGIBLE_TARGET,
    ELIGIBLE_TARGET;

    static TotemHudState resolve(boolean hasVillagerTarget, boolean isEligibleForConversion) {
        if (!hasVillagerTarget) {
            return NO_TARGET;
        }

        return isEligibleForConversion ? ELIGIBLE_TARGET : INELIGIBLE_TARGET;
    }

}
