package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import lombok.Builder;

import javax.annotation.Nonnull;

/**
 * The outcome of a player's use of a ballista.
 */
enum BallistaVerb {

    WIND,
    LOAD_AMMO,
    UNLOAD_AMMO,
    ENTER_AIM,
    DEFER_TO_VANILLA,
    REFUSE;

    static BallistaVerb resolve(@Nonnull Situation situation) {
        if (situation.sneaking()) {
            // Sneaking with an item must remain available for placing blocks against the ballista
            return situation.mainHand() == MainHand.EMPTY ? ENTER_AIM : DEFER_TO_VANILLA;
        }

        return switch (situation.stage()) {
            case UNWOUND -> WIND;
            case COCKED -> resolveCocked(situation);
            case WINDING, FIRING -> REFUSE;
        };
    }

    private static BallistaVerb resolveCocked(@Nonnull Situation situation) {
        if (situation.loaded()) {
            return situation.mainHand() == MainHand.EMPTY ? UNLOAD_AMMO : REFUSE;
        }

        return situation.mainHand() == MainHand.AMMUNITION ? LOAD_AMMO : REFUSE;
    }

    /**
     * Main-hand item categories used to select an action.
     */
    enum MainHand {

        EMPTY,
        AMMUNITION,
        OTHER

    }

    /**
     * Inputs for resolving a player's use.
     *
     * @param stage  the machine's stage after applying elapsed-time transitions
     * @param loaded whether ammunition is seated
     */
    @Builder
    record Situation(@Nonnull MainHand mainHand,
                     boolean sneaking,
                     @Nonnull BallistaStateMachine.Stage stage,
                     boolean loaded) {
    }

}
