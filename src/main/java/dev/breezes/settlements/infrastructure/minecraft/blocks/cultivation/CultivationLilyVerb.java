package dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation;

/**
 * Which gesture a right-click on the Cultivation Lily resolves to, decided from plain booleans so the
 * branch is testable without a player, an item stack, or any other Minecraft type.
 */
enum CultivationLilyVerb {

    RESIZE,
    SET_FILTER,
    CLEAR_FILTER,
    DECLINE;

    static CultivationLilyVerb resolve(boolean sneaking, boolean handEmpty, boolean stackIsAcceptedSeed) {
        if (sneaking) {
            // Resize is an empty-handed gesture only
            return handEmpty ? RESIZE : DECLINE;
        }
        if (handEmpty) {
            return CLEAR_FILTER;
        }

        return stackIsAcceptedSeed ? SET_FILTER : DECLINE;
    }

    /**
     * An empty offhand is not a deliberate empty-hand Use — it is only the absence of a second item,
     * seen whenever the main hand's own pass already declined and vanilla tries the offhand next.
     */
    static boolean isEmptyHandGesture(boolean stackEmpty, boolean mainHand) {
        return stackEmpty && mainHand;
    }

}
