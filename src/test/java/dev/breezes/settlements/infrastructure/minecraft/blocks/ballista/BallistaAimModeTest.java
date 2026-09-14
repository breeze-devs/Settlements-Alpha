package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaAimModeTest {

    private static final String FIRST = "first ballista";
    private static final String SECOND = "second ballista";

    private static final boolean EMPTY_HAND = true;
    private static final boolean HOLDING_ITEM = false;
    private static final boolean SNEAKING = true;
    private static final boolean STANDING = false;

    private static final double ONE_NOTCH_UP = 1.0;
    private static final double ONE_NOTCH_DOWN = -1.0;

    // A trackpad reports a notch in pieces smaller than half of one
    private static final double TRACKPAD_PIECE = 0.4;

    @Test
    void mainHandUse_freshSneakingEmptyHandUse_entersTheMachineOnRotation() {
        // Arrange
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);

        // Assert
        assertEquals(BallistaAimMode.UseResponse.ENTERED, response);
        assertEquals(FIRST, mode.aimed());
        assertEquals(BallistaAimMode.Axis.ROTATION, mode.axis());
    }

    @Test
    void mainHandUse_sneakingWithAnItemInHand_passesThrough() {
        // Arrange
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(FIRST, HOLDING_ITEM, SNEAKING);

        // Assert: taking the gesture from a held item would stop a player placing a block against the machine
        assertEquals(BallistaAimMode.UseResponse.PASS_THROUGH, response);
        assertNull(mode.aimed());
    }

    @Test
    void mainHandUse_heldEntryClick_neverSwitchesTheAxis() {
        // Arrange: entered, then the click is held while the sneak key is let go
        BallistaAimMode<String> mode = entered(FIRST);

        // Act: the use repeats with no new press
        BallistaAimMode.UseResponse sneakingRepeat = mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);
        BallistaAimMode.UseResponse standingRepeat = mode.mainHandUse(FIRST, EMPTY_HAND, STANDING);

        // Assert: the repeat is still claimed, or it would wind or unload the machine
        assertEquals(BallistaAimMode.UseResponse.HELD, sneakingRepeat);
        assertEquals(BallistaAimMode.UseResponse.HELD, standingRepeat);
        assertEquals(BallistaAimMode.Axis.ROTATION, mode.axis());
    }

    @Test
    void mainHandUse_freshEmptyHandUseOnTheAimedMachine_switchesTheAxisBothWaysAndNeverExits() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);

        // Act
        mode.usePressed();
        BallistaAimMode.UseResponse toElevation = mode.mainHandUse(FIRST, EMPTY_HAND, STANDING);
        BallistaAimMode.Axis afterFirst = mode.axis();
        mode.usePressed();
        BallistaAimMode.UseResponse toRotation = mode.mainHandUse(FIRST, EMPTY_HAND, STANDING);

        // Assert
        assertEquals(BallistaAimMode.UseResponse.SWITCHED_AXIS, toElevation);
        assertEquals(BallistaAimMode.Axis.ELEVATION, afterFirst);
        assertEquals(BallistaAimMode.UseResponse.SWITCHED_AXIS, toRotation);
        assertEquals(BallistaAimMode.Axis.ROTATION, mode.axis());
        assertEquals(FIRST, mode.aimed());
    }

    @Test
    void mainHandUse_freshSneakingUseOnTheAimedMachine_switchesTheAxis() {
        // Arrange: the entry sneak is still held
        BallistaAimMode<String> mode = entered(FIRST);
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);

        // Assert: a player who keeps the sneak key down while aiming still switches with a click
        assertEquals(BallistaAimMode.UseResponse.SWITCHED_AXIS, response);
        assertEquals(BallistaAimMode.Axis.ELEVATION, mode.axis());
    }

    @Test
    void mainHandUse_itemUseOnTheAimedMachine_passesThrough() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(FIRST, HOLDING_ITEM, STANDING);

        // Assert: loading a bolt into the aimed machine is an ordinary use
        assertEquals(BallistaAimMode.UseResponse.PASS_THROUGH, response);
        assertEquals(BallistaAimMode.Axis.ROTATION, mode.axis());
    }

    @Test
    void mainHandUse_standingEmptyHandUseOnAnotherMachine_passesThroughAndKeepsTheAim() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(SECOND, EMPTY_HAND, STANDING);

        // Assert: winding a neighbor must not take the player out of aiming this one
        assertEquals(BallistaAimMode.UseResponse.PASS_THROUGH, response);
        assertEquals(FIRST, mode.aimed());
    }

    @Test
    void mainHandUse_emptyHandUseOnSomethingElse_passesThrough() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);
        mode.usePressed();

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(null, EMPTY_HAND, SNEAKING);

        // Assert: the off hand's item still gets its use
        assertEquals(BallistaAimMode.UseResponse.PASS_THROUGH, response);
        assertEquals(FIRST, mode.aimed());
    }

    @Test
    void mainHandUse_freshSneakingUseOnAnotherMachine_movesTheAimToIt() {
        // Arrange: the first machine aimed on elevation
        BallistaAimMode<String> mode = entered(FIRST);
        mode.usePressed();
        mode.mainHandUse(FIRST, EMPTY_HAND, STANDING);

        // Act
        mode.usePressed();
        BallistaAimMode.UseResponse response = mode.mainHandUse(SECOND, EMPTY_HAND, SNEAKING);

        // Assert: one machine per player, and a new machine starts on rotation
        assertEquals(BallistaAimMode.UseResponse.ENTERED, response);
        assertEquals(SECOND, mode.aimed());
        assertEquals(BallistaAimMode.Axis.ROTATION, mode.axis());
    }

    @Test
    void mainHandUse_heldSneakingUseSweptOntoAMachine_doesNotEnterIt() {
        // Arrange: a press already answered somewhere else
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        mode.usePressed();
        mode.mainHandUse(null, EMPTY_HAND, SNEAKING);

        // Act
        BallistaAimMode.UseResponse response = mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);

        // Assert
        assertEquals(BallistaAimMode.UseResponse.HELD, response);
        assertNull(mode.aimed());
    }

    @Test
    void sneakPressed_afterEntry_isDone() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);

        // Act
        mode.sneakPressed();

        // Assert
        assertNull(mode.aimed());
    }

    @Test
    void sneakPressedBeforeEntry_doesNotEndTheModeItEnters() {
        // Arrange: the sneak press that begins the entry gesture comes before the click
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        mode.sneakPressed();
        mode.usePressed();

        // Act
        mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);

        // Assert: a press remembered and applied after entry would end the mode the moment it began
        assertEquals(FIRST, mode.aimed());
    }

    @Test
    void suppressesOffHandUse_onlyWhenMainHandWasHandled() {
        // Arrange
        BallistaAimMode<String> mode = new BallistaAimMode<>();

        // Act
        mode.usePressed();
        mode.mainHandUse(FIRST, EMPTY_HAND, SNEAKING);
        boolean afterEntry = mode.shouldSuppressOffHandUse();
        mode.usePressed();
        mode.mainHandUse(null, EMPTY_HAND, STANDING);
        boolean afterPassThrough = mode.shouldSuppressOffHandUse();

        // Assert: an unsuppressed off hand would raise a shield or place a torch on the click that entered aim mode
        assertTrue(afterEntry);
        assertFalse(afterPassThrough);
    }

    @Test
    void scrolled_upOnRotation_turnsTheMuzzleClockwiseSeenFromAbove() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);
        BallistaAim aim = BallistaAim.facing(30.0F, 0.0F);

        // Act
        BallistaAim scrolled = mode.scrolled(aim, ONE_NOTCH_UP);

        // Assert: seen from above, a clockwise turn from one heading to the next points the cross product down
        Vec3 before = BallistaLaunchPoint.direction(aim);
        Vec3 after = BallistaLaunchPoint.direction(BallistaAim.facing(scrolled.getIntentYaw(), scrolled.getIntentPitch()));
        assertTrue(before.cross(after).y < 0.0, "turned counterclockwise: " + before + " -> " + after);
        assertEquals(aim.getIntentPitch(), scrolled.getIntentPitch());
    }

    @Test
    void scrolled_upOnElevation_raisesTheMuzzle() {
        // Arrange
        BallistaAimMode<String> mode = onElevation(FIRST);
        BallistaAim aim = BallistaAim.facing(30.0F, 0.0F);

        // Act
        BallistaAim scrolled = mode.scrolled(aim, ONE_NOTCH_UP);

        // Assert
        assertTrue(scrolled.getIntentPitch() > aim.getIntentPitch());
        assertEquals(aim.getIntentYaw(), scrolled.getIntentYaw());
    }

    @Test
    void scrolled_turnsFromTheIntent_notFromWhereTheBarrelHasReached() {
        // Arrange: a barrel still turning toward an earlier adjustment
        BallistaAimMode<String> mode = entered(FIRST);
        BallistaAim turning = BallistaAim.facing(0.0F, 0.0F).withIntent(60.0F, 0.0F);

        // Act
        BallistaAim scrolled = mode.scrolled(turning, ONE_NOTCH_UP);

        // Assert: quick notches would otherwise undo each other while the barrel lags behind them
        assertEquals(60.0F + BallistaAim.MANUAL_YAW_STEP_DEGREES, scrolled.getIntentYaw());
    }

    @Test
    void scrolled_addsTrackpadPiecesUpIntoWholeNotches() {
        // Arrange
        BallistaAimMode<String> mode = entered(FIRST);
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F);

        // Act
        BallistaAim afterOnePiece = mode.scrolled(aim, TRACKPAD_PIECE);
        BallistaAim afterTwoPieces = mode.scrolled(afterOnePiece, TRACKPAD_PIECE);
        BallistaAim afterThreePieces = mode.scrolled(afterTwoPieces, TRACKPAD_PIECE);

        // Assert
        assertSame(aim, afterOnePiece);
        assertSame(aim, afterTwoPieces);
        assertEquals(BallistaAim.MANUAL_YAW_STEP_DEGREES, afterThreePieces.getIntentYaw());
    }

    @Test
    void scrolled_downFromTheTopOfThePitchRange_lowersAtOnce() {
        // Arrange: scrolled well past the top of the range
        BallistaAimMode<String> mode = onElevation(FIRST);
        BallistaAim aim = BallistaAim.facing(0.0F, BallistaAim.MAX_PITCH_DEGREES);
        for (int notch = 0; notch < 10; notch++) {
            aim = mode.scrolled(aim, ONE_NOTCH_UP);
        }

        // Act
        BallistaAim lowered = mode.scrolled(aim, ONE_NOTCH_DOWN);

        // Assert
        assertEquals(BallistaAim.MAX_PITCH_DEGREES, aim.getIntentPitch());
        assertTrue(lowered.getIntentPitch() < BallistaAim.MAX_PITCH_DEGREES);
    }

    @Test
    void scrolled_leavesAPartNotchBehind_whenTheAxisSwitches() {
        // Arrange: most of a notch scrolled on rotation
        BallistaAimMode<String> mode = entered(FIRST);
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F);
        aim = mode.scrolled(aim, TRACKPAD_PIECE);
        aim = mode.scrolled(aim, TRACKPAD_PIECE);
        mode.usePressed();
        mode.mainHandUse(FIRST, EMPTY_HAND, STANDING);

        // Act
        BallistaAim scrolled = mode.scrolled(aim, TRACKPAD_PIECE);

        // Assert: carried over, the rotation's scrolling would move the elevation
        assertSame(aim, scrolled);
    }

    @Test
    void scrolled_doesNothing_whenNotAiming() {
        // Arrange
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        BallistaAim aim = BallistaAim.facing(0.0F, 0.0F);

        // Act
        BallistaAim scrolled = mode.scrolled(aim, ONE_NOTCH_UP);

        // Assert
        assertSame(aim, scrolled);
    }

    private static BallistaAimMode<String> entered(String ballista) {
        BallistaAimMode<String> mode = new BallistaAimMode<>();
        mode.usePressed();
        mode.mainHandUse(ballista, EMPTY_HAND, SNEAKING);
        return mode;
    }

    private static BallistaAimMode<String> onElevation(String ballista) {
        BallistaAimMode<String> mode = entered(ballista);
        mode.usePressed();
        mode.mainHandUse(ballista, EMPTY_HAND, STANDING);
        return mode;
    }

}
