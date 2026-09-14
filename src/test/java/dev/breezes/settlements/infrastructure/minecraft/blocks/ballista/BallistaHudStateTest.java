package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaVerb.MainHand.AMMUNITION;
import static dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaVerb.MainHand.EMPTY;
import static dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaVerb.MainHand.OTHER;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BallistaHudStateTest {

    private static final boolean LOADED = true;
    private static final boolean EMPTY_SOCKET = false;

    private static final boolean BOLT_NEEDED = true;
    private static final boolean NO_BOLT_NEEDED = false;
    private static final boolean AIM_OFFERED = true;
    private static final boolean NO_AIM = false;

    /**
     * Every cell of the look-at table: the machine's state and the player's main hand, and what the player is told.
     */
    private static Stream<Arguments> lookAtTable() {
        return Stream.of(
                cell(BallistaStateMachine.Stage.UNWOUND, EMPTY_SOCKET, EMPTY,
                        BallistaHudState.Status.UNWOUND, NO_BOLT_NEEDED, BallistaHudState.Gesture.WIND, AIM_OFFERED),
                cell(BallistaStateMachine.Stage.UNWOUND, EMPTY_SOCKET, AMMUNITION,
                        BallistaHudState.Status.UNWOUND, NO_BOLT_NEEDED, BallistaHudState.Gesture.WIND, NO_AIM),
                cell(BallistaStateMachine.Stage.UNWOUND, EMPTY_SOCKET, OTHER,
                        BallistaHudState.Status.UNWOUND, NO_BOLT_NEEDED, BallistaHudState.Gesture.WIND, NO_AIM),

                cell(BallistaStateMachine.Stage.WINDING, EMPTY_SOCKET, EMPTY,
                        BallistaHudState.Status.WINDING, NO_BOLT_NEEDED, null, AIM_OFFERED),
                cell(BallistaStateMachine.Stage.WINDING, EMPTY_SOCKET, AMMUNITION,
                        BallistaHudState.Status.WINDING, NO_BOLT_NEEDED, null, NO_AIM),
                cell(BallistaStateMachine.Stage.WINDING, EMPTY_SOCKET, OTHER,
                        BallistaHudState.Status.WINDING, NO_BOLT_NEEDED, null, NO_AIM),

                cell(BallistaStateMachine.Stage.COCKED, EMPTY_SOCKET, EMPTY,
                        BallistaHudState.Status.COCKED_EMPTY, BOLT_NEEDED, null, AIM_OFFERED),
                cell(BallistaStateMachine.Stage.COCKED, EMPTY_SOCKET, AMMUNITION,
                        BallistaHudState.Status.COCKED_EMPTY, NO_BOLT_NEEDED, BallistaHudState.Gesture.LOAD, NO_AIM),
                cell(BallistaStateMachine.Stage.COCKED, EMPTY_SOCKET, OTHER,
                        BallistaHudState.Status.COCKED_EMPTY, BOLT_NEEDED, null, NO_AIM),

                cell(BallistaStateMachine.Stage.COCKED, LOADED, EMPTY,
                        BallistaHudState.Status.COCKED_LOADED, NO_BOLT_NEEDED, BallistaHudState.Gesture.UNLOAD, AIM_OFFERED),
                cell(BallistaStateMachine.Stage.COCKED, LOADED, AMMUNITION,
                        BallistaHudState.Status.COCKED_LOADED, NO_BOLT_NEEDED, null, NO_AIM),
                cell(BallistaStateMachine.Stage.COCKED, LOADED, OTHER,
                        BallistaHudState.Status.COCKED_LOADED, NO_BOLT_NEEDED, null, NO_AIM),

                cell(BallistaStateMachine.Stage.FIRING, EMPTY_SOCKET, EMPTY,
                        BallistaHudState.Status.FIRING, NO_BOLT_NEEDED, null, AIM_OFFERED),
                cell(BallistaStateMachine.Stage.FIRING, EMPTY_SOCKET, AMMUNITION,
                        BallistaHudState.Status.FIRING, NO_BOLT_NEEDED, null, NO_AIM),
                cell(BallistaStateMachine.Stage.FIRING, EMPTY_SOCKET, OTHER,
                        BallistaHudState.Status.FIRING, NO_BOLT_NEEDED, null, NO_AIM));
    }

    @ParameterizedTest
    @MethodSource("lookAtTable")
    void resolve_tellsThePlayerWhatTheirHandDoes(BallistaStateMachine.Stage stage,
                                                 boolean loaded,
                                                 BallistaVerb.MainHand mainHand,
                                                 BallistaHudState expected) {
        // Act
        BallistaHudState readout = BallistaHudState.resolve(stage, loaded, mainHand);

        // Assert: a gesture shown that the machine would refuse, or one hidden that it would perform, misleads the
        // player about the one hand they are holding
        assertEquals(expected, readout);
    }

    private static Arguments cell(BallistaStateMachine.Stage stage,
                                  boolean loaded,
                                  BallistaVerb.MainHand mainHand,
                                  BallistaHudState.Status status,
                                  boolean boltNeeded,
                                  BallistaHudState.Gesture use,
                                  boolean aimOffered) {
        return Arguments.of(stage, loaded, mainHand, new BallistaHudState(status, boltNeeded, use, aimOffered));
    }

}
