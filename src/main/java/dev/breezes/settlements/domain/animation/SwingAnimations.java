package dev.breezes.settlements.domain.animation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Shared timing constants for one-shot swing-style interactions (chop, mine, hit-with-tool, etc.).
 * <p>
 * Behavior code and animation code coordinate via these constants — there is no event bus, so the
 * impact tick and the animation's contact frame must agree by construction. Allocating one second
 * (20 ticks) per swing leaves enough room for raise + impact + recover.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SwingAnimations {

    public static final int SWING_DURATION_TICKS = 12;
    public static final int SWING_RAISE_TICKS = 5;
    public static final int SWING_IMPACT_TICKS = 8;

}
