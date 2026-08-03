package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocomotionAnimations {

    public static KeyframeAnimation stroll() {
        return gait("stroll", 25,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, Easing.LINEAR, keys(
                        r(0, -1.96F, -4.89F, -1.63F), r(1, -2.7F, -5.69F, -1.9F), r(2, -3.0F, -6.0F, -2.0F), r(3, -2.7F, -5.69F, -1.9F),
                        r(4, -1.96F, -4.89F, -1.63F), r(5, -1.0F, -3.75F, -1.25F), r(6, -0.04F, -2.44F, -0.81F), r(7, 0.7F, -1.14F, -0.38F),
                        r(8, 1.0F, 0, 0), r(9, 0.7F, 1.14F, 0.38F), r(10, -0.04F, 2.44F, 0.81F), r(11, -1.0F, 3.75F, 1.25F),
                        r(13, -1.96F, 4.89F, 1.63F), r(14, -2.7F, 5.69F, 1.9F), r(15, -3.0F, 6.0F, 2.0F), r(16, -2.7F, 5.69F, 1.9F),
                        r(17, -1.96F, 4.89F, 1.63F), r(18, -1.0F, 3.75F, 1.25F), r(19, -0.04F, 2.44F, 0.81F), r(20, 0.7F, 1.14F, 0.38F),
                        r(21, 1.0F, 0, 0), r(22, 0.7F, -1.14F, -0.38F), r(23, -0.04F, -2.44F, -0.81F), r(24, -1.0F, -3.75F, -1.25F),
                        r(25, -1.96F, -4.89F, -1.63F))),
                // Skipped ticks in the dense sequences of this clip are deliberate rather than gaps to be
                // filled in: the curve interpolates straight across them.
                translations(AnimationTargets.ARMS_CROSSED_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, 0.28, 0), p(1, 0, 0.39, 0), p(2, 0, 0.47, 0), p(3, 0, 0.5, 0), p(4, 0, 0.46, 0),
                        p(5, 0, 0.35, 0), p(7, 0, 0.09, 0), p(8, 0, 0.01, 0), p(9, 0, 0, 0), p(10, 0, 0.08, 0),
                        p(14, 0, 0.42, 0), p(15, 0, 0.49, 0), p(16, 0, 0.49, 0), p(17, 0, 0.42, 0), p(20, 0, 0.08, 0),
                        p(21, 0, 0.01, 0), p(22, 0, 0.01, 0), p(23, 0, 0.07, 0), p(24, 0, 0.16, 0), p(25, 0, 0.28, 0))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -5, 0, 0), r(6, -10, 0, 0), r(13, 15, 0, 0), r(19, 25, 0, 0), r(25, -5, 0, 0))),
                translations(AnimationTargets.LEG_LEFT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -0.2, -3.8), p(6, 0, -0.1, 0), p(13, 0, 0.3, 0.5), p(19, 0, -1.7, -2.5), p(25, 0, -0.2, -3.8))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, 15, 0, 0), r(6, 25, 0, 0), r(13, -5, 0, 0), r(19, -10, 0, 0), r(25, 15, 0, 0))),
                translations(AnimationTargets.LEG_RIGHT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0.3, 0.5), p(6, 0, -1.7, -2.5), p(13, 0, -0.2, -3.8), p(19, 0, -0.1, 0), p(25, 0, 0.3, 0.5))),
                rotations(AnimationTargets.NOSE_ROTATION, Easing.LINEAR, keys(
                        r(0, -2.56F, 0, -1.9F), r(1, -1.44F, 0, -1.63F), r(3, 1.44F, 0, -0.81F), r(4, 2.56F, 0, -0.38F),
                        r(5, 3.0F, 0, 0), r(6, 2.56F, 0, 0.38F), r(7, 1.44F, 0, 0.81F), r(9, -1.44F, 0, 1.63F),
                        r(10, -2.56F, 0, 1.9F), r(11, -3.0F, 0, 2.0F), r(13, -2.56F, 0, 1.9F), r(14, -1.44F, 0, 1.63F),
                        r(16, 1.44F, 0, 0.81F), r(17, 2.56F, 0, 0.38F), r(18, 3.0F, 0, 0), r(19, 2.56F, 0, -0.38F),
                        r(20, 1.44F, 0, -0.81F), r(22, -1.44F, 0, -1.63F), r(23, -2.56F, 0, -1.9F), r(24, -3.0F, 0, -2.0F),
                        r(25, -2.56F, 0, -1.9F))),
                rotations(AnimationTargets.BODY_ROTATION, Easing.CUBIC, keys(
                        r(0, -4, 4, 0), r(6, -1, 0, 0), r(13, -4, -4, 0), r(19, -1, 0, 0), r(25, -4, 4, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, 0), p(6, 0, -1.0, 0), p(13, 0, 0, 0), p(19, 0, -1.0, 0), p(25, 0, 0, 0))),
                rotations(AnimationTargets.HEAD_ROTATION_OVERRIDE, Easing.LINEAR, keys(
                        r(0, 1.68F, 0, 0), r(1, 3.31F, 0, 0), r(2, 4.52F, 0, 0), r(3, 5.0F, 0, 0), r(4, 4.35F, 0, 0),
                        r(5, 2.75F, 0, 0), r(6, 0.74F, 0, 0), r(7, -1.13F, 0, 0), r(8, -2.33F, 0, 0), r(9, -2.5F, 0, 0),
                        r(10, -1.33F, 0, 0), r(11, 0.32F, 0, 0), r(13, 2.18F, 0, 0), r(14, 3.83F, 0, 0), r(15, 4.85F, 0, 0),
                        r(16, 4.85F, 0, 0), r(17, 3.83F, 0, 0), r(18, 2.18F, 0, 0), r(19, 0.32F, 0, 0), r(20, -1.33F, 0, 0),
                        r(21, -2.35F, 0, 0), r(22, -2.37F, 0, 0), r(23, -1.49F, 0, 0), r(24, -0.03F, 0, 0), r(25, 1.68F, 0, 0))),
                rotations(AnimationTargets.MONOBROW_ROTATION, Easing.CUBIC, keys(
                        r(0, 0, 0, -1.0F), r(13, 0, 0, 1.0F), r(25, 0, 0, -1.0F))),
                translations(AnimationTargets.MONOBROW_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -0.1, 0), p(6, 0, 0, 0), p(13, 0, -0.1, 0), p(19, 0, 0, 0), p(25, 0, -0.1, 0))),
                translations(AnimationTargets.PUPIL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0.1, 0, 0))),
                translations(AnimationTargets.PUPIL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, -0.1, 0, 0))));
    }

    public static KeyframeAnimation walk() {
        return gait("walk", 20,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, Easing.LINEAR, keys(
                        r(0, -5, 0, -1.58F), r(1, -5, 0, -1.89F), r(2, -5, 0, -2.0F), r(3, -5, 0, -1.89F), r(4, -5, 0, -1.58F),
                        r(5, -5, 0, -1.14F), r(9, -5, 0, 1.14F), r(10, -5, 0, 1.58F), r(11, -5, 0, 1.89F), r(12, -5, 0, 2.0F),
                        r(13, -5, 0, 1.89F), r(14, -5, 0, 1.58F), r(15, -5, 0, 1.14F), r(19, -5, 0, -1.14F), r(20, -5, 0, -1.58F))),
                translations(AnimationTargets.ARMS_CROSSED_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, 0.18, 0), p(1, 0, 0.05, 0), p(2, 0, 0, 0), p(3, 0, 0.05, 0), p(6, 0, 0.45, 0),
                        p(7, 0, 0.5, 0), p(8, 0, 0.45, 0), p(11, 0, 0.05, 0), p(12, 0, 0, 0), p(13, 0, 0.05, 0),
                        p(16, 0, 0.45, 0), p(17, 0, 0.5, 0), p(18, 0, 0.45, 0), p(20, 0, 0.18, 0))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -5, 0, 0), r(5, -10, 0, 0), r(10, 10, 0, 0), r(15, 20, 0, 0), r(20, -5, 0, 0))),
                translations(AnimationTargets.LEG_LEFT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -0.2, -2.5), p(4, 0, -0.1, 0), p(10, 0, 0.3, 0.5), p(15, 0, -1.7, -1.5), p(20, 0, -0.2, -2.5))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, 10, 0, 0), r(5, 20, 0, 0), r(10, -5, 0, 0), r(15, -10, 0, 0), r(20, 10, 0, 0))),
                translations(AnimationTargets.LEG_RIGHT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0.3, 0.5), p(5, 0, -1.7, -1.5), p(10, 0, -0.2, -2.5), p(14, 0, -0.1, 0), p(20, 0, 0.3, 0.5))),
                rotations(AnimationTargets.NOSE_ROTATION, Easing.LINEAR, keys(
                        r(0, -5, 0, 3.0F), r(1, -4.22F, 0, 2.78F), r(2, -2.36F, 0, 2.23F), r(3, -0.14F, 0, 1.49F), r(4, 1.72F, 0, 0.7F),
                        r(5, 2.5F, 0, 0), r(6, 1.72F, 0, -0.7F), r(7, -0.14F, 0, -1.49F), r(8, -2.36F, 0, -2.23F), r(9, -4.22F, 0, -2.78F),
                        r(10, -5, 0, -3.0F), r(11, -4.22F, 0, -2.78F), r(12, -2.36F, 0, -2.23F), r(13, -0.14F, 0, -1.49F), r(14, 1.72F, 0, -0.7F),
                        r(15, 2.5F, 0, 0), r(16, 1.72F, 0, 0.7F), r(17, -0.14F, 0, 1.49F), r(18, -2.36F, 0, 2.23F), r(19, -4.22F, 0, 2.78F),
                        r(20, -5, 0, 3.0F))),
                rotations(AnimationTargets.BODY_ROTATION, Easing.CUBIC, keys(
                        r(0, 4, -4, 0), r(5, 6, -3, 0), r(10, 4, 4, 0), r(15, 6, 3, 0), r(20, 4, -4, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, 0), p(5, 0, -0.4, 0), p(7, 0, -0.4, 0), p(10, 0, 0, 0), p(15, 0, -0.4, 0), p(17, 0, -0.4, 0), p(20, 0, 0, 0))),
                rotations(AnimationTargets.HEAD_ROTATION_OVERRIDE, Easing.LINEAR, keys(
                        r(0, 0.59F, 0, 0), r(1, -0.59F, 0, 0), r(2, -1.58F, 0, 0), r(3, -2.0F, 0, 0), r(4, -1.58F, 0, 0),
                        r(5, -0.59F, 0, 0), r(6, 0.59F, 0, 0), r(7, 1.58F, 0, 0), r(8, 2.0F, 0, 0), r(9, 1.58F, 0, 0),
                        r(10, 0.59F, 0, 0), r(11, -0.59F, 0, 0), r(12, -1.58F, 0, 0), r(13, -2.0F, 0, 0), r(14, -1.58F, 0, 0),
                        r(15, -0.59F, 0, 0), r(16, 0.59F, 0, 0), r(17, 1.58F, 0, 0), r(18, 2.0F, 0, 0), r(19, 1.58F, 0, 0),
                        r(20, 0.59F, 0, 0))),
                translations(AnimationTargets.MONOBROW_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, 0, 0), p(1, 0, -0.01, 0), p(4, 0, -0.09, 0), p(5, 0, -0.1, 0), p(9, 0, -0.01, 0),
                        p(10, 0, 0, 0), p(11, 0, -0.01, 0), p(14, 0, -0.09, 0), p(15, 0, -0.1, 0), p(16, 0, -0.09, 0),
                        p(19, 0, -0.01, 0), p(20, 0, 0, 0))),
                translations(AnimationTargets.PUPIL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0.1, 0, 0))),
                translations(AnimationTargets.PUPIL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, -0.1, 0, 0))));
    }

    public static KeyframeAnimation jog() {
        return gait("jog", 16,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, Easing.LINEAR, keys(
                        r(0, -2.5F, -5.0F, -6.25F), r(2, -10, -8, -10), r(4, -2.5F, -5.0F, -6.25F), r(6, 5, 0, 0),
                        r(8, -2.5F, 5.0F, 6.25F), r(10, -10, 8, 10), r(12, -2.5F, 5.0F, 6.25F), r(14, 5, 0, 0),
                        r(16, -2.5F, -5.0F, -6.25F))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -30, 0, 0), r(8, 30, 0, 0), r(16, -30, 0, 0))),
                translations(AnimationTargets.LEG_LEFT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, -3.0), p(4, 0, 0.5, 0), p(8, 0, -1.0, 2.0), p(12, 0, -2.0, -4.0), p(16, 0, 0, -3.0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, 30, 0, 0), r(8, -30, 0, 0), r(16, 30, 0, 0))),
                translations(AnimationTargets.LEG_RIGHT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -1.0, 2.0), p(4, 0, -2.0, -4.0), p(8, 0, 0, -3.0), p(12, 0, 0.5, 0), p(16, 0, -1.0, 2.0))),
                rotations(AnimationTargets.NOSE_ROTATION, Easing.LINEAR, keys(
                        r(0, -5, 0, -4.37F), r(2, -15, 0, -2.0F), r(4, -5, 0, 1.88F), r(6, 5, 0, 5.0F),
                        r(8, -5, 0, 4.38F), r(10, -15, 0, 2.0F), r(12, -5, 0, -1.87F), r(14, 5, 0, -5.0F), r(16, -5, 0, -4.37F))),
                rotations(AnimationTargets.BODY_ROTATION, Easing.CUBIC, keys(
                        r(0, 10, -3, -2), r(4, 20, -0.38F, -0.25F), r(8, 10, 3, 2), r(12, 20, 0.05F, 0.03F), r(16, 10, -3, -2))),
                translations(AnimationTargets.BODY_TRANSLATION, Easing.CUBIC, keys(
                        p(2, 0, 0, 0), p(4, 0, -1.5, 0), p(6, 0, -1.5, 0), p(8, 0, 0, 0), p(10, 0, 0, 0), p(12, 0, -1.5, 0), p(14, 0, -1.5, 0), p(16, 0, 0, 0))),
                rotations(AnimationTargets.HEAD_ROTATION_OVERRIDE, Easing.LINEAR, keys(
                        r(0, -20, 0, 0), r(4, -10, 0, 0), r(8, -20, 0, 0), r(12, -10, 0, 0), r(16, -20, 0, 0))),
                translations(AnimationTargets.MONOBROW_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0), p(4, 0, -0.7, 0), p(8, 0, -1.0, 0), p(12, 0, -0.7, 0), p(16, 0, -1.0, 0))),
                translations(AnimationTargets.EYELID_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                translations(AnimationTargets.EYEBALL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -0.4, 0))),
                scales(AnimationTargets.EYEBALL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0.2, 0, 0))),
                scales(AnimationTargets.PUPIL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.7F, 1.0F))),
                translations(AnimationTargets.EYELID_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                translations(AnimationTargets.EYEBALL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -0.4, 0))),
                scales(AnimationTargets.EYEBALL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, -0.2, 0, 0))),
                scales(AnimationTargets.PUPIL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.7F, 1.0F))),
                scales(AnimationTargets.MOUTH_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.3F, 1.0F))));
    }

    public static KeyframeAnimation run() {
        return gait("run", 10,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, Easing.CUBIC, keys(
                        r(0, -20, -10, -5), r(3, 0, 0, 0), r(5, -20, 10, 5), r(8, 0, 0, 0), r(10, -20, -10, -5))),
                rotations(AnimationTargets.HEAD_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -5, 0, -5), r(3, 2.5F, 0, 0), r(5, -5, 0, 5), r(8, 2.5F, 0, 0), r(10, -5, 0, -5))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -30, 0, 0), r(5, 30, 0, 0), r(10, -30, 0, 0))),
                translations(AnimationTargets.LEG_LEFT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, -3.0), p(3, 0, 0.5, 0), p(5, 0, -1.0, 2.0), p(8, 0, -2.0, -4.0), p(10, 0, 0, -3.0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, 30, 0, 0), r(5, -30, 0, 0), r(10, 30, 0, 0))),
                translations(AnimationTargets.LEG_RIGHT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -1.0, 2.0), p(3, 0, -2.0, -4.0), p(5, 0, 0, -3.0), p(8, 0, 0.5, 0), p(10, 0, -1.0, 2.0))),
                rotations(AnimationTargets.NOSE_ROTATION, Easing.LINEAR, keys(
                        r(0, -10, 0, 0), r(1, -25, 0, 0), r(4, 5, 0, 0), r(6, -25, 0, 0), r(9, 5, 0, 0), r(10, -10, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, Easing.CUBIC, keys(
                        r(0, 10, 0, 0), r(3, 5, 3, 2), r(5, 10, 0, 0), r(8, 5, -3, -2), r(10, 10, 0, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, Easing.CUBIC, keys(
                        p(1, 0, 0, 0), p(3, 0, -1.0, 0), p(4, 0, -1.0, 0), p(5, 0, 0, 0), p(6, 0, 0, 0), p(8, 0, -1.0, 0), p(9, 0, -1.0, 0), p(10, 0, 0, 0))),
                translations(AnimationTargets.MONOBROW_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -1.5, 0), p(3, 0, -1.0, 0), p(5, 0, -1.5, 0), p(8, 0, -1.0, 0), p(10, 0, -1.5, 0))),
                translations(AnimationTargets.EYELID_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                translations(AnimationTargets.EYEBALL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -0.4, 0))),
                scales(AnimationTargets.EYEBALL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0.2, 0, 0))),
                scales(AnimationTargets.PUPIL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.7F, 1.0F))),
                translations(AnimationTargets.EYELID_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                translations(AnimationTargets.EYEBALL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -0.4, 0))),
                scales(AnimationTargets.EYEBALL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, -0.2, 0, 0))),
                scales(AnimationTargets.PUPIL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.7F, 1.0F))),
                scales(AnimationTargets.MOUTH_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 1.3F, 1.0F))));
    }

    public static KeyframeAnimation panicRun() {
        return gait("panic_run", 10,
                rotations(AnimationTargets.ARMS_CROSSED_ROTATION, Easing.CUBIC, keys(
                        r(0, -20, -10, -5), r(3, 0, 0, 0), r(5, -20, 10, 5), r(8, 0, 0, 0), r(10, -20, -10, -5))),
                translations(AnimationTargets.ARMS_CROSSED_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, -2.0), p(3, 0, 2.0, 0), p(5, 0, 0, -2.0), p(8, 0, 2.0, 0), p(10, 0, 0, -2.0))),
                rotations(AnimationTargets.HEAD_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -5, 0, -5), r(3, 2.5F, 0, 0), r(5, -5, 0, 5), r(8, 2.5F, 0, 0), r(10, -5, 0, -5))),
                rotations(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, -30, 0, 0), r(5, 30, 0, 0), r(10, -30, 0, 0))),
                translations(AnimationTargets.LEG_LEFT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0, -3.0), p(3, 0, 0.5, 0), p(5, 0, -1.0, 2.0), p(8, 0, -2.0, -4.0), p(10, 0, 0, -3.0))),
                rotations(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE, Easing.CUBIC, keys(
                        r(0, 30, 0, 0), r(5, -30, 0, 0), r(10, 30, 0, 0))),
                translations(AnimationTargets.LEG_RIGHT_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -1.0, 2.0), p(3, 0, -2.0, -4.0), p(5, 0, 0, -3.0), p(8, 0, 0.5, 0), p(10, 0, -1.0, 2.0))),
                rotations(AnimationTargets.NOSE_ROTATION, Easing.LINEAR, keys(
                        r(0, -10, 0, 0), r(1, -25, 0, 0), r(4, 5, 0, 0), r(6, -25, 0, 0), r(9, 5, 0, 0), r(10, -10, 0, 0))),
                rotations(AnimationTargets.BODY_ROTATION, Easing.CUBIC, keys(
                        r(0, -10, 0, 0), r(3, -5, 3, 2), r(5, -10, 0, 0), r(8, -5, -3, -2), r(10, -10, 0, 0))),
                translations(AnimationTargets.BODY_TRANSLATION, Easing.CUBIC, keys(
                        p(1, 0, 0, 0), p(3, 0, -1.0, 0), p(4, 0, -1.0, 0), p(5, 0, 0, 0), p(6, 0, 0, 0), p(8, 0, -1.0, 0), p(9, 0, -1.0, 0), p(10, 0, 0, 0))),
                translations(AnimationTargets.MONOBROW_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, -1.5, 0), p(3, 0, -1.0, 0), p(5, 0, -1.5, 0), p(8, 0, -1.0, 0), p(10, 0, -1.5, 0))),
                translations(AnimationTargets.EYELID_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                scales(AnimationTargets.EYEBALL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 2.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_LEFT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0.2, 0, 0))),
                // Return to the opening scale before the loop seam to keep dilation continuous.
                scales(AnimationTargets.PUPIL_LEFT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.3F, 1.0F), s(1, 1.0F, 0.4F, 1.0F), s(10, 1.0F, 0.3F, 1.0F))),
                translations(AnimationTargets.EYELID_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, 0, -1.0, 0))),
                scales(AnimationTargets.EYEBALL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 2.5F, 1.0F))),
                translations(AnimationTargets.PUPIL_RIGHT_TRANSLATION, Easing.LINEAR, keys(
                        p(0, -0.2, 0, 0))),
                scales(AnimationTargets.PUPIL_RIGHT_SCALE, Easing.LINEAR, keys(
                        s(0, 1.0F, 0.3F, 1.0F), s(1, 1.0F, 0.4F, 1.0F), s(10, 1.0F, 0.3F, 1.0F))),
                translations(AnimationTargets.MOUTH_TRANSLATION, Easing.CUBIC, keys(
                        p(0, 0, 0.2, 0), p(3, 0, 0.4, 0), p(5, 0, 0.2, 0), p(8, 0, 0.4, 0), p(10, 0, 0.2, 0))),
                scales(AnimationTargets.MOUTH_SCALE, Easing.CUBIC, keys(
                        s(0, 1.0F, 1.5F, 1.0F), s(3, 1.0F, 2.0F, 1.0F), s(5, 1.0F, 1.5F, 1.0F), s(8, 1.0F, 2.0F, 1.0F), s(10, 1.0F, 1.5F, 1.0F))));
    }

    @SafeVarargs
    private static KeyframeAnimation gait(@Nonnull String name, int durationTicks, AnimationTrack<?>... tracks) {
        // Each gait now carries a different channel count (face/eye tracks appear only on the faster
        // gaits), so tracks are applied by iteration rather than fixed positional indexing.
        TrackAnimationBuilder builder = KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/locomotion/" + name))
                .durationTicks(durationTicks)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(3)
                .blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_CROSSED);
        for (AnimationTrack<?> track : tracks) {
            builder = builder.track(track);
        }
        return builder.build();
    }

    private static AnimationTrack<Vector3f> rotations(AnimationTarget<Vector3f> target, Easing easing, List<Keyframe<Vector3f>> keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(withEasing(keyframes, easing)).build();
    }

    private static AnimationTrack<Vec3> translations(AnimationTarget<Vec3> target, Easing easing, List<Keyframe<Vec3>> keyframes) {
        return AnimationTrack.<Vec3>builder().target(target).keyframes(withEasing(keyframes, easing)).build();
    }

    private static AnimationTrack<Vector3f> scales(AnimationTarget<Vector3f> target, Easing easing, List<Keyframe<Vector3f>> keyframes) {
        return AnimationTrack.<Vector3f>builder().target(target).keyframes(withEasing(keyframes, easing)).build();
    }

    // r()/p()/s() author keyframes against Easing.LINEAR as a placeholder; the track helpers above
    // stamp in the real per-track easing here so call sites don't repeat it on every single keyframe.
    private static <V> List<Keyframe<V>> withEasing(List<Keyframe<V>> keyframes, Easing easing) {
        return keyframes.stream().map(keyframe -> new Keyframe<>(keyframe.tick(), keyframe.value(), easing)).toList();
    }

    @SafeVarargs
    private static <V> List<Keyframe<V>> keys(Keyframe<V>... keyframes) {
        return List.of(keyframes);
    }

    /**
     * Rotation keyframe, authored in degrees.
     */
    private static Keyframe<Vector3f> r(int tick, float pitch, float yaw, float roll) {
        return new Keyframe<>(tick, RotationUtil.degrees(pitch, yaw, roll), Easing.LINEAR);
    }

    /**
     * Translation keyframe.
     */
    private static Keyframe<Vec3> p(int tick, double x, double y, double z) {
        return new Keyframe<>(tick, new Vec3(x, y, z), Easing.LINEAR);
    }

    /**
     * Per-axis scale keyframe.
     */
    private static Keyframe<Vector3f> s(int tick, float x, float y, float z) {
        return new Keyframe<>(tick, new Vector3f(x, y, z), Easing.LINEAR);
    }

}
