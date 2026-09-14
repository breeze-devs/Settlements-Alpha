package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BallistaVerbTest {

    @ParameterizedTest
    @EnumSource(BallistaVerb.MainHand.class)
    void resolve_anyHand_windsAnUnwoundMachine(BallistaVerb.MainHand mainHand) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(mainHand)
                .stage(BallistaStateMachine.Stage.UNWOUND)
                .build();

        // Act, Assert: a wind refused for what the player happens to hold would send them to empty a hand before the
        // longest step of the cycle
        assertEquals(BallistaVerb.WIND, BallistaVerb.resolve(situation));
    }

    @ParameterizedTest
    @MethodSource("everyHandOnAMachineWindingOrFiring")
    void resolve_isRefused_whileTheMachineWindsOrFires(BallistaVerb.MainHand mainHand,
                                                       BallistaStateMachine.Stage stage,
                                                       boolean loaded) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(mainHand)
                .stage(stage)
                .loaded(loaded)
                .build();

        // Act, Assert: a wind restarted over one in progress throws its progress away, and a bolt unloaded or loaded
        // while firing takes the shot out of the machine before release or seats a second one behind it
        assertEquals(BallistaVerb.REFUSE, BallistaVerb.resolve(situation));
    }

    @Test
    void resolve_bolt_loadsACockedEmptyMachine() {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(BallistaVerb.MainHand.AMMUNITION)
                .stage(BallistaStateMachine.Stage.COCKED)
                .loaded(false)
                .build();

        // Act, Assert
        assertEquals(BallistaVerb.LOAD_AMMO, BallistaVerb.resolve(situation));
    }

    @ParameterizedTest
    @EnumSource(value = BallistaVerb.MainHand.class, names = {"EMPTY", "OTHER"})
    void resolve_anythingButABolt_isRefused_onACockedEmptyMachine(BallistaVerb.MainHand mainHand) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(mainHand)
                .stage(BallistaStateMachine.Stage.COCKED)
                .loaded(false)
                .build();

        // Act, Assert: an empty hand that wound the machine again or unloaded a socket with nothing in it would act
        // on a machine that is only waiting for its bolt
        assertEquals(BallistaVerb.REFUSE, BallistaVerb.resolve(situation));
    }

    @Test
    void resolve_emptyHand_unloadsALoadedMachine() {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(BallistaVerb.MainHand.EMPTY)
                .stage(BallistaStateMachine.Stage.COCKED)
                .loaded(true)
                .build();

        // Act, Assert: a use that fired instead would let a click shoot the machine, which only redstone may do
        assertEquals(BallistaVerb.UNLOAD_AMMO, BallistaVerb.resolve(situation));
    }

    @ParameterizedTest
    @EnumSource(value = BallistaVerb.MainHand.class, names = {"AMMUNITION", "OTHER"})
    void resolve_anyItem_isRefused_onALoadedMachine(BallistaVerb.MainHand mainHand) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(mainHand)
                .stage(BallistaStateMachine.Stage.COCKED)
                .loaded(true)
                .build();

        // Act, Assert: a bolt loaded here seats a second one in an occupied socket, and an item that unloaded would
        // empty the machine on every click made while carrying something past it
        assertEquals(BallistaVerb.REFUSE, BallistaVerb.resolve(situation));
    }

    @ParameterizedTest
    @MethodSource("everyMachineState")
    void resolve_sneakingWithAnEmptyMainHand_entersAim_inEveryMachineState(BallistaStateMachine.Stage stage,
                                                                          boolean loaded) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(BallistaVerb.MainHand.EMPTY)
                .sneaking(true)
                .stage(stage)
                .loaded(loaded)
                .build();

        // Act, Assert: resolving the aiming gesture as a standing use would wind or unload the machine the player
        // meant only to aim
        assertEquals(BallistaVerb.ENTER_AIM, BallistaVerb.resolve(situation));
    }

    @ParameterizedTest
    @MethodSource("everyItemInEveryMachineState")
    void resolve_sneakingWithAnItemInTheMainHand_defersToVanilla_inEveryMachineState(BallistaVerb.MainHand mainHand,
                                                                                    BallistaStateMachine.Stage stage,
                                                                                    boolean loaded) {
        // Arrange
        BallistaVerb.Situation situation = situation()
                .mainHand(mainHand)
                .sneaking(true)
                .stage(stage)
                .loaded(loaded)
                .build();

        // Act, Assert: claiming the gesture would stop a sneaking player placing a block against the machine or using
        // the item, and loading or winding on it would act where the player asked vanilla for something else
        assertEquals(BallistaVerb.DEFER_TO_VANILLA, BallistaVerb.resolve(situation));
    }

    /**
     * Every stage, with and without ammunition loaded.
     */
    private static Stream<Arguments> everyMachineState() {
        return Arrays.stream(BallistaStateMachine.Stage.values())
                .flatMap(stage -> Stream.of(Arguments.of(stage, false), Arguments.of(stage, true)));
    }

    /**
     * A bolt and any other item, each in every machine state.
     */
    private static Stream<Arguments> everyItemInEveryMachineState() {
        return Stream.of(BallistaVerb.MainHand.AMMUNITION, BallistaVerb.MainHand.OTHER)
                .flatMap(mainHand -> everyMachineState()
                        .map(state -> Arguments.of(mainHand, state.get()[0], state.get()[1])));
    }

    /**
     * Every hand, on a machine winding or firing, with and without ammunition loaded.
     */
    private static Stream<Arguments> everyHandOnAMachineWindingOrFiring() {
        return Arrays.stream(BallistaVerb.MainHand.values())
                .flatMap(mainHand -> Stream.of(BallistaStateMachine.Stage.WINDING, BallistaStateMachine.Stage.FIRING)
                        .flatMap(stage -> Stream.of(
                                Arguments.of(mainHand, stage, false),
                                Arguments.of(mainHand, stage, true))));
    }

    /**
     * A standing, empty-handed use on an unwound, empty machine.
     */
    private static BallistaVerb.Situation.SituationBuilder situation() {
        return BallistaVerb.Situation.builder()
                .mainHand(BallistaVerb.MainHand.EMPTY)
                .sneaking(false)
                .stage(BallistaStateMachine.Stage.UNWOUND)
                .loaded(false);
    }

}
