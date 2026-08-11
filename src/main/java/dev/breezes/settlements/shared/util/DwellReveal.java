package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * What a look-driven reveal should do this frame.
 * <p>
 * The two halves are independent.
 * {@link #revealed()} answers "may I resolve fresh content for the current target" (expensive), true only
 * while a target is actually held.
 * {@link #alpha()} answers "how transparent is it drawn", which outlives the target: content whose
 * target is gone is still fading, and content whose target is new is still arriving.
 */
@ClientSide
public record DwellReveal(boolean revealed, float alpha) {

    public boolean paints() {
        return this.alpha > 0.0F;
    }

}
