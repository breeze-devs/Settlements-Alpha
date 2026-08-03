package dev.breezes.settlements.domain.animation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Declares how a sampled target should be applied by the render/model layer.
 */
@AllArgsConstructor
@Getter
public enum AnimationTargetPolicy {

    /**
     * The authored value is a delta added onto whatever pose the model already holds.
     */
    ADDITIVE(false),

    /**
     * The authored value scales the pose the model already holds, so a unit neutral leaves it untouched.
     */
    MULTIPLICATIVE(false),

    /**
     * The authored value replaces the pose outright for as long as the target is present.
     */
    ABSOLUTE(false),

    /**
     * The authored value replaces the pose, but ownership fades in and out rather than snapping.
     */
    BLENDED_ABSOLUTE(true);

    /**
     * Whether a frame carries a separate application weight for targets under this policy.
     */
    private final boolean coverageTracked;

}
