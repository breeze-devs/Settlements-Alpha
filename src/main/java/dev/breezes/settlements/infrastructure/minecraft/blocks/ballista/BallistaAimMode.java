package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The player's ballista aim interaction state machine.
 *
 * @param <M> identifies a machine; two machines are one when equal
 */
@ClientSide
public final class BallistaAimMode<M> {

    static final float ELEVATION_DEGREES_PER_NOTCH = 1.0F;

    @Nullable
    private M aimed;
    private Axis axis = Axis.ROTATION;

    /**
     * Whether the use key has been pressed since a use last reached the main hand.
     */
    private boolean usePressPending;
    private boolean mainHandUseHandled;

    /**
     * Scrolling that has not yet added up to a whole notch.
     */
    private double scrollRemainder;

    void usePressed() {
        this.usePressPending = true;
    }

    void sneakPressed() {
        this.end();
    }

    /**
     * Whether to suppress this use's off-hand attempt because aim mode handled the main hand.
     */
    boolean shouldSuppressOffHandUse() {
        return this.mainHandUseHandled;
    }

    /**
     * Answers the main hand's pass of a use.
     *
     * @param ballista      the machine the use lands on, or null for anything else
     * @param mainHandEmpty whether the player's main hand is empty
     * @param sneaking      whether the player holds the sneak key
     */
    UseResponse mainHandUse(@Nullable M ballista, boolean mainHandEmpty, boolean sneaking) {
        boolean fresh = this.usePressPending;
        this.usePressPending = false;

        UseResponse response = this.onUse(ballista, mainHandEmpty, sneaking, fresh);
        this.mainHandUseHandled = response != UseResponse.PASS_THROUGH;
        return response;
    }

    private UseResponse onUse(@Nullable M ballista, boolean mainHandEmpty, boolean sneaking, boolean fresh) {
        if (ballista == null || !mainHandEmpty) {
            return UseResponse.PASS_THROUGH;
        }

        boolean onAimed = ballista.equals(this.aimed);
        if (!sneaking && !onAimed) {
            return UseResponse.PASS_THROUGH;
        }
        if (!fresh) {
            return UseResponse.HELD;
        }

        this.scrollRemainder = 0.0;
        // With the entry sneak still held, a click on the aimed machine is still the click that switches the axis
        if (onAimed) {
            this.axis = this.axis.other();
            return UseResponse.SWITCHED_AXIS;
        }

        this.aimed = ballista;
        this.axis = Axis.ROTATION;
        return UseResponse.ENTERED;
    }

    /**
     * The aim a scroll leaves the machine with.
     *
     * @return the same aim while the scrolling has not yet added up to a notch, or when not aiming
     */
    BallistaAim scrolled(@Nonnull BallistaAim aim, double scrollDelta) {
        if (this.aimed == null) {
            return aim;
        }

        this.scrollRemainder += scrollDelta;
        int notches = (int) this.scrollRemainder;
        if (notches == 0) {
            return aim;
        }

        this.scrollRemainder -= notches;
        return switch (this.axis) {
            case ROTATION ->
                    aim.withIntent(aim.getIntentYaw() + notches * BallistaAim.MANUAL_YAW_STEP_DEGREES, aim.getIntentPitch());
            case ELEVATION ->
                    aim.withIntent(aim.getIntentYaw(), aim.getIntentPitch() + notches * ELEVATION_DEGREES_PER_NOTCH);
        };
    }

    void end() {
        this.aimed = null;
        this.axis = Axis.ROTATION;
        this.scrollRemainder = 0.0;
    }

    /**
     * The machine being aimed, or null when not aiming.
     */
    @Nullable
    M aimed() {
        return this.aimed;
    }

    Axis axis() {
        return this.axis;
    }

    /**
     * Which of the machine's angles a scroll turns.
     */
    public enum Axis {

        ROTATION,
        ELEVATION;

        private Axis other() {
            return this == ROTATION ? ELEVATION : ROTATION;
        }

    }

    /**
     * What a use becomes in aim mode.
     */
    enum UseResponse {

        /**
         * The use is not relevant to the aim mode, letting vanilla handle it.
         */
        PASS_THROUGH,

        /**
         * The use is aim mode's, but repeats a press already answered, so nothing happens.
         */
        HELD,
        ENTERED,
        SWITCHED_AXIS

    }

}
