package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import dev.breezes.settlements.shared.util.RotationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdleLifeAnimations {

    public static final int BASE_IDLE_DURATION_TICKS = 80;  // breathe fills 0..60 (3 s), then holds neutral for a natural pause between breaths
    public static final int BLINK_DURATION_TICKS = 6;
    public static final int PONDER_DURATION_TICKS = 80;

    /**
     * Looping breathe animation for the at-rest state.
     * <p>
     * Head rotation is intentionally omitted: HEAD_ROTATION_OVERRIDE is an ABSOLUTE target that
     * suppresses vanilla look-tracking for the clip's entire duration. A 1.5° breathe bob is not
     * worth permanently blinding the villager's gaze — look-tracking must remain active on the
     * base layer. The head bob visual is imperceptible without it.
     * <p>
     * Eyelid channels are also intentionally omitted: the dedicated blink() clip owns them.
     * Keeping eyelids here too would cause both clips to additively close the eyelid at once
     * whenever blink fires on top of baseIdle, resulting in a double-close artifact.
     * <p>
     * A hold keyframe is appended at tick 80 (= the tick-60 neutral) on every track so the loop
     * pauses for one second between breathe cycles rather than snapping straight back to the top.
     */
    public static KeyframeAnimation baseIdle() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/base_idle"))
                .durationTicks(BASE_IDLE_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(60, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        // posVec(0, 0.2, -0.05) → Y negated → (0.0, -0.2, -0.05)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, -0.2, -0.05), Easing.LINEAR),
                                new Keyframe<>(60, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        // posVec(0, -0.05, 0) → Y negated → (0.0, 0.05, 0.0)
                        // posVec(0,  0.05, 0) → Y negated → (0.0, -0.05, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(6, new Vec3(0.0, 0.05, 0.0), Easing.LINEAR),
                                new Keyframe<>(28, new Vec3(0.0, -0.05, 0.0), Easing.LINEAR),
                                new Keyframe<>(60, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        // posVec(0, -0.2, 0) → Y negated → (0.0, 0.2, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, 0.2, 0.0), Easing.LINEAR),
                                new Keyframe<>(60, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        // posVec(0, -0.2, 0) → Y negated → (0.0, 0.2, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, 0.2, 0.0), Easing.LINEAR),
                                new Keyframe<>(60, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-1.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(60, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(80, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        // posVec(0, 0.1, 0.05) → Y negated → (0.0, -0.1, 0.05)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(24, new Vec3(0.0, -0.1, 0.05), Easing.LINEAR),
                                new Keyframe<>(60, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(80, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Single-shot eyelid blink, fired by the idle controller on a random timer.
     * <p>
     * Eyelid value is derived from the idle-breathe source's authored eyelid peak:
     * posVec(0, -1.1, 0) → Y negated → Vec3(0, 1.1, 0).
     * Keeping the blink isolated here means it can be independently scheduled and
     * cancelled without disturbing the baseIdle breathe rhythm.
     */
    public static KeyframeAnimation blink() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/blink"))
                .durationTicks(BLINK_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        // posVec(0, -1.1, 0) → Y negated → (0.0, 1.1, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        // posVec(0, -1.1, 0) → Y negated → (0.0, 1.1, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(2, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(6, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Occasional fidget overlay: the villager glances to one side, furrows the brow, and
     * bobs twice as if turning a thought over. Played ONCE and discarded rather than looped —
     * the Blockbench .looping() flag is a preview artifact, not a runtime directive.
     * <p>
     * Head rotation IS kept here (HEAD_ROTATION_OVERRIDE) because this gesture is intentionally
     * a brief, full "look-away-and-think" interruption that should suppress vanilla look-tracking
     * for its 4-second duration. The clip ends and look-tracking resumes immediately after.
     * <p>
     * Arms are driven at BOTH_CROSSED throughout; the small arms_crossed position nudge at
     * ticks 56–70 represents a subtle shuffle as the villager shifts weight while pondering.
     */
    public static KeyframeAnimation ponder() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/idle/ponder"))
                .durationTicks(PONDER_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4)
                .blendOutTicks(8)
                .arms(ArmConfiguration.BOTH_CROSSED)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.HEAD_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-10.0f, -18.0f, -4.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-10.0f, -18.0f, -4.0f), Easing.LINEAR),
                                new Keyframe<>(50, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(56, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(62, RotationUtil.degrees(-2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(68, RotationUtil.degrees(6.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(74, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARMS_CROSSED_TRANSLATION)
                        // posVec(0, 0.22, 0) → Y negated → (0.0, -0.22, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(56, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(62, new Vec3(0.0, -0.22, 0.0), Easing.LINEAR),
                                new Keyframe<>(70, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_LEFT_TRANSLATION)
                        // posVec(0, -1.1, 0) → Y negated → (0.0, 1.1, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(36, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(38, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(68, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(70, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(72, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.EYELID_RIGHT_TRANSLATION)
                        // posVec(0, -1.1, 0) → Y negated → (0.0, 1.1, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(36, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(38, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(68, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(70, new Vec3(0.0, 1.1, 0.0), Easing.LINEAR),
                                new Keyframe<>(72, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.MONOBROW_TRANSLATION)
                        // posVec(0, 0.15, 0) → Y negated → (0.0, -0.15, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(20, new Vec3(0.0, -0.15, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(0.0, -0.15, 0.0), Easing.LINEAR),
                                new Keyframe<>(50, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_LEFT_TRANSLATION)
                        // posVec(-0.3, 0.3, 0) → Y negated → (-0.3, -0.3, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(16, new Vec3(-0.3, -0.3, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(-0.3, -0.3, 0.0), Easing.LINEAR),
                                new Keyframe<>(48, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.PUPIL_RIGHT_TRANSLATION)
                        // posVec(-0.3, 0.3, 0) → Y negated → (-0.3, -0.3, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(16, new Vec3(-0.3, -0.3, 0.0), Easing.LINEAR),
                                new Keyframe<>(40, new Vec3(-0.3, -0.3, 0.0), Easing.LINEAR),
                                new Keyframe<>(48, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        // posVec(0, 0.08, 0) → Y negated → (0.0, -0.08, 0.0)
                        .keyframes(List.of(
                                new Keyframe<>(56, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(62, new Vec3(0.0, -0.08, 0.0), Easing.LINEAR),
                                new Keyframe<>(70, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
