package dev.breezes.settlements.domain.ballista;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The ballista bolt's measurements.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BallistaBoltGeometry {

    /**
     * The bolt's length in its layer definition.
     */
    public static final float LENGTH_PIXELS = 24.0F;

    /**
     * How far inside the tail end the pivot sits in the layer definition.
     */
    public static final float PIVOT_INSET_PIXELS = 0.5F;

    /**
     * How many times its authored size the bolt is drawn, seated and in flight.
     */
    public static final float DRAWN_SCALE = 1.5F;

    /**
     * How far ahead of its pivot the drawn bolt's tip lies, in pixels of the world, a sixteenth of a block each.
     */
    public static final float TIP_REACH_PIXELS = (LENGTH_PIXELS - PIVOT_INSET_PIXELS) * DRAWN_SCALE;

}
