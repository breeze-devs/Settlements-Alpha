package dev.breezes.settlements.domain.ballista;

import dev.breezes.settlements.domain.animation.BallistaAnimationTargets;
import dev.breezes.settlements.domain.animation.BallistaAnimations;
import dev.breezes.settlements.domain.animation.KeyframeAnimation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallistaStateMachineTest {

    private static final KeyframeAnimation WIND = BallistaAnimations.wind();
    private static final int WIND_LENGTH = WIND.getDurationTicks();

    private static final KeyframeAnimation FIRE = BallistaAnimations.fire();
    private static final int FIRE_LENGTH = FIRE.getDurationTicks();

    private static final long STARTED_AT = 1_000L;

    // Far longer than any wind or firing, standing in for a long stretch in which the machine did not tick
    private static final long LONG_GAP_TICKS = 1_000_000L;

    @Test
    void settledAt_isStillWinding_oneTickBeforeTheWindClipEnds() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);

        // Act
        BallistaStateMachine settled = state.settledAt(STARTED_AT + WIND_LENGTH - 1);

        // Assert: finishing early would hold the machine cocked while its clip is still drawing the last stroke
        assertEquals(BallistaStateMachine.Stage.WINDING, settled.getStage());
    }

    @Test
    void settledAt_isCocked_onTheTickTheWindClipEnds() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);

        // Act
        BallistaStateMachine settled = state.settledAt(STARTED_AT + WIND_LENGTH);

        // Assert: finishing late would refuse the operator as still winding while the machine is drawn cocked
        assertEquals(BallistaStateMachine.Stage.COCKED, settled.getStage());
    }

    @Test
    void settledAt_isCockedAtOnce_whenAWindIsNextSettledLongAfterItsClipRanOut() {
        // Arrange: a wind on a machine that stopped ticking soon after it started, as one in a chunk that stays loaded
        // but is not ticked does
        BallistaStateMachine state = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);

        // Act: the machine's next tick
        BallistaStateMachine settled = state.settledAt(STARTED_AT + LONG_GAP_TICKS);

        // Assert: a wind that became cocked only on the exact tick its clip ends would still be winding here, stranded
        // for good by the machine missing that tick
        assertEquals(BallistaStateMachine.Stage.COCKED, settled.getStage());
    }

    @Test
    void settledAt_isStillFiring_oneTickBeforeTheFireClipEnds() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act
        BallistaStateMachine settled = state.settledAt(STARTED_AT + FIRE_LENGTH - 1);

        // Assert: settling early would let the operator start a wind while the machine is still drawn recoiling
        assertEquals(BallistaStateMachine.Stage.FIRING, settled.getStage());
    }

    @Test
    void settledAt_isUnwound_onTheTickTheFireClipEnds() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act
        BallistaStateMachine settled = state.settledAt(STARTED_AT + FIRE_LENGTH);

        // Assert: settling late would refuse the operator as still firing while the machine is drawn at rest
        assertEquals(BallistaStateMachine.Stage.UNWOUND, settled.getStage());
    }

    @Test
    void settledAt_isUnwoundAtOnce_whenAFiringIsNextSettledLongAfterItsClipRanOut() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act
        BallistaStateMachine settled = state.settledAt(STARTED_AT + LONG_GAP_TICKS);

        // Assert: a firing that settled only on the exact tick its clip ends would refuse every use for good once the
        // machine missed that tick
        assertEquals(BallistaStateMachine.Stage.UNWOUND, settled.getStage());
    }

    @Test
    void settledAt_neverMovesAMachineAtRest() {
        // Arrange
        long muchLater = STARTED_AT + LONG_GAP_TICKS;

        // Act, Assert: only a started wind or firing moves on with time
        assertEquals(BallistaStateMachine.Stage.UNWOUND,
                BallistaStateMachine.unwound().settledAt(muchLater).getStage());
        assertEquals(BallistaStateMachine.Stage.COCKED, BallistaStateMachine.cocked().settledAt(muchLater).getStage());
    }

    @Test
    void isTransient_exactlyForTheStagesTimeMovesOn() {
        // Arrange: one machine in each stage
        List<BallistaStateMachine> states = List.of(
                BallistaStateMachine.unwound(),
                BallistaStateMachine.unwound().startWindingAt(STARTED_AT),
                BallistaStateMachine.cocked(),
                BallistaStateMachine.cocked().fireAt(STARTED_AT));

        // Act, Assert: a stage time moves on that read as one the machine rests in would be skipped by a machine that
        // stops ticking at rest, stranding the wind or the shot in it
        for (BallistaStateMachine state : states) {
            boolean movesOn = state.settledAt(STARTED_AT + LONG_GAP_TICKS).getStage() != state.getStage();
            assertEquals(movesOn, state.isTransient(), state.getStage().name());
        }
    }

    @Test
    void startWindingAt_refusesAMachineThatIsNotUnwound() {
        // Act, Assert: starting over a wind in progress would throw away its progress, and winding a cocked or firing
        // machine would draw it unwinding
        assertThrows(IllegalStateException.class, () -> BallistaStateMachine.cocked().startWindingAt(STARTED_AT));
        assertThrows(IllegalStateException.class,
                () -> BallistaStateMachine.unwound().startWindingAt(STARTED_AT).startWindingAt(STARTED_AT + 1));
        assertThrows(IllegalStateException.class,
                () -> BallistaStateMachine.cocked().fireAt(STARTED_AT).startWindingAt(STARTED_AT + 1));
    }

    @Test
    void fireAt_refusesAMachineThatIsNotCocked() {
        // Act, Assert: firing an unwound or winding machine would launch a shot it never drew, and firing again during
        // a firing would launch a second shot from one wind
        assertThrows(IllegalStateException.class, () -> BallistaStateMachine.unwound().fireAt(STARTED_AT));
        assertThrows(IllegalStateException.class,
                () -> BallistaStateMachine.unwound().startWindingAt(STARTED_AT).fireAt(STARTED_AT + 1));
        assertThrows(IllegalStateException.class,
                () -> BallistaStateMachine.cocked().fireAt(STARTED_AT).fireAt(STARTED_AT + 1));
    }

    @Test
    void holdsShotAt_untilTheFireClipsReleaseTick_whereHasReleasedAtTakesOver() {
        // Arrange
        BallistaStateMachine firing = BallistaStateMachine.cocked().fireAt(STARTED_AT);
        long releaseAt = STARTED_AT + BallistaAnimations.FIRE_RELEASE_TICK;

        // Act, Assert: releasing early launches the shot while the seated bolt is still drawn riding the pusher, and
        // releasing late leaves it parked on a pusher already at rest
        assertTrue(firing.holdsShotAt(STARTED_AT), "holds the shot on the commit");
        assertTrue(firing.holdsShotAt(releaseAt - 1), "holds the shot the tick before release");
        assertFalse(firing.hasReleasedAt(releaseAt - 1), "released the tick before release");

        assertFalse(firing.holdsShotAt(releaseAt), "holds the shot on the release tick");
        assertTrue(firing.hasReleasedAt(releaseAt), "not released on the release tick");
    }

    @Test
    void hasReleasedAt_staysTrue_forAFiringNotSettledSinceItsReleaseTickWentBy() {
        // Arrange: a firing on a machine that missed its release tick and its clip's end
        BallistaStateMachine firing = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act, Assert: a release due only on the exact tick would lose the shot of a machine that missed that tick
        assertTrue(firing.hasReleasedAt(STARTED_AT + LONG_GAP_TICKS));
    }

    @Test
    void holdsShotAt_andHasReleasedAt_areBothFalse_outsideAFiring() {
        // Arrange
        List<BallistaStateMachine> states = List.of(
                BallistaStateMachine.unwound(),
                BallistaStateMachine.unwound().startWindingAt(STARTED_AT),
                BallistaStateMachine.cocked());

        // Act, Assert: a machine not firing has no shot to draw on its socket or to launch
        for (BallistaStateMachine state : states) {
            for (long now = STARTED_AT; now <= STARTED_AT + FIRE_LENGTH; now++) {
                assertFalse(state.holdsShotAt(now), state.getStage() + " holds a shot at " + now);
                assertFalse(state.hasReleasedAt(now), state.getStage() + " released a shot at " + now);
            }
        }
    }

    @Test
    void beatAt_landsWhereTheWindClipsPusherStartsAndStopsDrawing_andLocksWhenTheClipEnds() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);

        // Act, Assert: a beat off its stroke is heard while the machine is visibly still or already moving, which is
        // what a re-export that retimed the strokes without their beats would produce
        for (int tick = 1; tick <= WIND_LENGTH; tick++) {
            assertEquals(beatSeenInTheClip(tick), state.beatAt(STARTED_AT + tick), "tick " + tick + " of the wind");
        }
    }

    @Test
    void beatAt_isSilentOnTheStartInstant_andOutsideTheWind() {
        // Arrange
        BallistaStateMachine state = BallistaStateMachine.unwound().startWindingAt(STARTED_AT);

        // Act, Assert: the start instant carrying a beat would sound the first stroke twice where the starting gesture
        // sounds it too, and a beat outside the wind would sound strokes nobody is turning
        assertEquals(Optional.empty(), state.beatAt(STARTED_AT));
        assertEquals(Optional.empty(), state.beatAt(STARTED_AT - 1));
        assertEquals(Optional.empty(), state.beatAt(STARTED_AT + WIND_LENGTH + 1));
    }

    @Test
    void beatAt_isSilentThroughoutAFiring() {
        // Arrange
        BallistaStateMachine firing = BallistaStateMachine.cocked().fireAt(STARTED_AT);

        // Act, Assert: wind beats keyed on elapsed ticks alone would sound crank strokes over the recoil
        for (long now = STARTED_AT; now <= STARTED_AT + FIRE_LENGTH; now++) {
            assertEquals(Optional.empty(), firing.beatAt(now), "tick " + (now - STARTED_AT) + " of the firing");
        }
    }

    /**
     * The beat the wind clip's own motion puts on the given tick: a stroke begins where the pusher starts drawing back
     * and catches where it stops, and the wind locks on the clip's last tick.
     */
    private static Optional<BallistaStateMachine.Beat> beatSeenInTheClip(int tick) {
        if (tick == WIND_LENGTH) {
            return Optional.of(BallistaStateMachine.Beat.LOCKS);
        }

        boolean drawingBefore = pusherMovesBetween(tick - 1, tick);
        boolean drawingAfter = pusherMovesBetween(tick, tick + 1);
        if (drawingAfter && !drawingBefore) {
            return Optional.of(BallistaStateMachine.Beat.STROKE_BEGINS);
        }
        if (drawingBefore && !drawingAfter) {
            return Optional.of(BallistaStateMachine.Beat.STROKE_CATCHES);
        }
        return Optional.empty();
    }

    private static boolean pusherMovesBetween(int fromTick, int toTick) {
        Vec3 from = WIND.sample(fromTick).get(BallistaAnimationTargets.PUSHER_TRANSLATION);
        Vec3 to = WIND.sample(toTick).get(BallistaAnimationTargets.PUSHER_TRANSLATION);
        return !from.equals(to);
    }

}
