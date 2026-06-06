package dev.breezes.settlements.domain.presentation;

import javax.annotation.Nonnull;

/**
 * Describes the arm-set choice for each arm independently.
 * Snapped at animation transition rather than blended because geometry visibility
 * cannot crossfade — the incoming animation's declared config takes effect immediately.
 */
public record ArmConfiguration(@Nonnull ArmPose left, @Nonnull ArmPose right) {

    public static final ArmConfiguration BOTH_CROSSED = new ArmConfiguration(ArmPose.CROSSED, ArmPose.CROSSED);
    public static final ArmConfiguration BOTH_STRAIGHT = new ArmConfiguration(ArmPose.STRAIGHT, ArmPose.STRAIGHT);
    public static final ArmConfiguration LEFT_CROSSED_RIGHT_STRAIGHT = new ArmConfiguration(ArmPose.CROSSED, ArmPose.STRAIGHT);
    public static final ArmConfiguration RIGHT_CROSSED_LEFT_STRAIGHT = new ArmConfiguration(ArmPose.STRAIGHT, ArmPose.CROSSED);

}
