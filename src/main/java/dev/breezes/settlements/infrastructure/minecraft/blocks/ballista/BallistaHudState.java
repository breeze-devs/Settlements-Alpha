package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaStateMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What a player looking at a ballista is told.
 *
 * @param boltNeeded whether the machine waits on a bolt the player's hand does not hold
 * @param use        what a standing use does, or null when it would be refused
 * @param aimOffered whether a sneaking use enters aim mode
 */
public record BallistaHudState(@Nonnull Status status,
                               boolean boltNeeded,
                               @Nullable Gesture use,
                               boolean aimOffered) {

    static BallistaHudState resolve(@Nonnull BallistaStateMachine.Stage stage,
                                    boolean loaded,
                                    @Nonnull BallistaVerb.MainHand mainHand) {
        BallistaVerb.Situation.SituationBuilder situation = BallistaVerb.Situation.builder()
                .mainHand(mainHand)
                .stage(stage)
                .loaded(loaded);
        BallistaVerb standing = BallistaVerb.resolve(situation.sneaking(false).build());
        BallistaVerb sneaking = BallistaVerb.resolve(situation.sneaking(true).build());

        Status status = statusOf(stage, loaded);
        return new BallistaHudState(status,
                status == Status.COCKED_EMPTY && standing != BallistaVerb.LOAD_AMMO,
                gestureOf(standing),
                sneaking == BallistaVerb.ENTER_AIM);
    }

    private static Status statusOf(@Nonnull BallistaStateMachine.Stage stage, boolean loaded) {
        return switch (stage) {
            case UNWOUND -> Status.UNWOUND;
            case WINDING -> Status.WINDING;
            case COCKED -> loaded ? Status.COCKED_LOADED : Status.COCKED_EMPTY;
            case FIRING -> Status.FIRING;
        };
    }

    @Nullable
    private static Gesture gestureOf(@Nonnull BallistaVerb verb) {
        return switch (verb) {
            case WIND -> Gesture.WIND;
            case LOAD_AMMO -> Gesture.LOAD;
            case UNLOAD_AMMO -> Gesture.UNLOAD;
            case ENTER_AIM, DEFER_TO_VANILLA, REFUSE -> null;
        };
    }

    public enum Status {

        UNWOUND,
        WINDING,
        COCKED_EMPTY,
        COCKED_LOADED,
        FIRING

    }

    public enum Gesture {

        WIND,
        LOAD,
        UNLOAD

    }

}
