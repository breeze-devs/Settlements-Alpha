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
public final class FishingAnimations {

    public static final int CAST_DURATION_TICKS = 30;
    // Forward throw peak at 0.8s — the frame a server behavior should treat as hook release
    public static final int CAST_IMPACT_TICK = 16;

    public static final int FISHING_WAIT_DURATION_TICKS = 40;
    public static final int FIGHT_FISH_DURATION_TICKS = 24;
    public static final int REEL_DURATION_TICKS = 10;
    // Big yank peak at 0.2s — the frame a server behavior should treat as fish capture
    public static final int REEL_IMPACT_TICK = 4;

    private static final int BLEND_IN_TICKS = 2;
    private static final int BLEND_OUT_TICKS = 3;

    /**
     * One-shot over-the-shoulder rod cast. Arms swing back then release forward;
     * torso and legs counter-rotate to sell the weight transfer. Duration is fixed
     * so the server can schedule hook-spawn at CAST_IMPACT_TICK.
     */
    public static KeyframeAnimation cast() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/cast_hook"))
                .durationTicks(CAST_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms swing back to wind-up then snap forward on release
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-50.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Left leg braces back during wind-up, absorbs the forward transfer
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Right leg mirrors left for a balanced stance transfer
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose secondary wobble adds snappiness to the throw's reactive momentum
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(19, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(25, RotationUtil.degrees(2.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose translation follows the same reactive rhythm; Y negated from posVec
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(3, new Vec3(0.0, 0.05, 0.05), Easing.LINEAR), // posVec(0,-0.05,0.05) Y negated
                                new Keyframe<>(15, new Vec3(0.0, -0.1, -0.1), Easing.LINEAR), // posVec(0,0.1,-0.1)   Y negated
                                new Keyframe<>(19, new Vec3(0.0, 0.15, 0.15), Easing.LINEAR), // posVec(0,-0.15,0.15) Y negated
                                new Keyframe<>(25, new Vec3(0.0, 0.05, 0.05), Easing.LINEAR), // posVec(0,-0.05,0.05) Y negated
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Torso leans back on wind-up, thrusts forward on release, settles
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(16, RotationUtil.degrees(20.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(22, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Torso translation sells the shift in centre of mass; Y negated from posVec
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(12, new Vec3(0.0, 0.0, -1.5), Easing.LINEAR), // posVec(0,0,-1.5)    Y=0 unchanged
                                new Keyframe<>(16, new Vec3(0.0, 1.0, 2.0), Easing.LINEAR), // posVec(0,-1.0,2.0)  Y negated
                                new Keyframe<>(22, new Vec3(0.0, 0.3, 0.8), Easing.LINEAR), // posVec(0,-0.3,0.8)  Y negated
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Looping idle while the hook is in the water. Gentle sway keeps the arms held
     * out and the body breathing; nose secondary adds subtle life. No legs — stationary.
     */
    public static KeyframeAnimation fishingLoop() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/loop"))
                .durationTicks(FISHING_WAIT_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Crossed arms hold the rod out with a subtle breathing sway
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-15.0f, -2.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(-12.0f, 0.0f, 1.0f), Easing.LINEAR),
                                new Keyframe<>(20, RotationUtil.degrees(-15.0f, 2.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(-18.0f, 0.0f, -1.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(-15.0f, -2.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose micro-bob synced with the breath cycle for organic feel
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(0.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(13, RotationUtil.degrees(-0.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(23, RotationUtil.degrees(0.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(33, RotationUtil.degrees(-0.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose translation complements rotation for plausible nose weight; Y negated
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(3, new Vec3(0.0, 0.05, 0.0), Easing.LINEAR), // posVec(0,-0.05,0) Y negated
                                new Keyframe<>(13, new Vec3(0.0, -0.05, 0.0), Easing.LINEAR), // posVec(0,0.05,0)  Y negated
                                new Keyframe<>(23, new Vec3(0.0, 0.05, 0.0), Easing.LINEAR), // posVec(0,-0.05,0) Y negated
                                new Keyframe<>(33, new Vec3(0.0, -0.05, 0.0), Easing.LINEAR), // posVec(0,0.05,0)  Y negated
                                new Keyframe<>(40, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Torso very slightly leans forward and bobs — holding fishing stance
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(4.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(1.5f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(40, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Torso translation rises/falls with the breath; Y negated from posVec
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(10, new Vec3(0.0, 0.1, 0.0), Easing.LINEAR), // posVec(0,-0.1,0) Y negated
                                new Keyframe<>(30, new Vec3(0.0, -0.1, 0.0), Easing.LINEAR), // posVec(0,0.1,0)  Y negated
                                new Keyframe<>(40, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

    /**
     * Looping struggle animation while a fish is on the line. Aggressive pull-and-release
     * cycles; legs widen stance to anchor, nose judders reactively. Driven until the
     * behavior decides the fish is won or lost.
     */
    public static KeyframeAnimation fightFish() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/fight_fish"))
                .durationTicks(FIGHT_FISH_DURATION_TICKS)
                .loopMode(LoopMode.LOOP)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms yank up hard then dip as the fish pulls; alternating tension cycles
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-30.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-30.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Left leg digs in during yanks, braces the pulling direction
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-12.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(-5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(-8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-12.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Right leg mirrors left — wide stance maintains balance under fish load
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(12.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(12.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose rapid reactive judder — exaggerates the strain of reeling
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(3, RotationUtil.degrees(-4.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(9, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(15, RotationUtil.degrees(-6.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(21, RotationUtil.degrees(1.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose translation matches rotation direction for convincing secondary motion; Y negated
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(3, new Vec3(0.0, -0.08, -0.08), Easing.LINEAR), // posVec(0,0.08,-0.08)   Y negated
                                new Keyframe<>(9, new Vec3(0.0, 0.05, 0.05), Easing.LINEAR), // posVec(0,-0.05,0.05)   Y negated
                                new Keyframe<>(15, new Vec3(0.0, -0.12, -0.12), Easing.LINEAR), // posVec(0,0.12,-0.12)   Y negated
                                new Keyframe<>(21, new Vec3(0.0, 0.02, 0.02), Easing.LINEAR), // posVec(0,-0.02,0.02)   Y negated
                                new Keyframe<>(24, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Torso rocks back under fish tension, releases briefly, then repeats
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(6, RotationUtil.degrees(5.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(12, RotationUtil.degrees(-12.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(18, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(24, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Torso shifts weight backward to pull against fish resistance; Y negated
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, new Vec3(0.0, 0.5, -1.0), Easing.LINEAR), // posVec(0,-0.5,-1.0)  Y negated
                                new Keyframe<>(6, new Vec3(0.0, 0.2, 0.5), Easing.LINEAR), // posVec(0,-0.2,0.5)   Y negated
                                new Keyframe<>(12, new Vec3(0.0, 0.6, -1.2), Easing.LINEAR), // posVec(0,-0.6,-1.2)  Y negated
                                new Keyframe<>(18, new Vec3(0.0, 0.3, -0.2), Easing.LINEAR), // posVec(0,-0.3,-0.2)  Y negated
                                new Keyframe<>(24, new Vec3(0.0, 0.5, -1.0), Easing.LINEAR)  // posVec(0,-0.5,-1.0)  Y negated
                        ))
                        .build())
                .build();
    }

    /**
     * One-shot reel-in snap. Short and punchy — arms yank back hard at REEL_IMPACT_TICK,
     * torso follows with a full lean, then everything releases as the fish arrives.
     * Server schedules fish-item spawn at REEL_IMPACT_TICK.
     */
    public static KeyframeAnimation reelIn() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/fishing/reel_in"))
                .durationTicks(REEL_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .arms(ArmConfiguration.BOTH_CROSSED)
                // Arms drive hard upward on the yank peak then release back to neutral
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARMS_CROSSED_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-50.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-35.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Legs absorb the jolt — left braces back during the yank
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_LEFT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Right leg counter-braces to keep the villager planted
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.LEG_RIGHT_ROTATION_OVERRIDE)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(10.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose gets whipped back on the yank then settles — fast reactive energy
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.NOSE_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(1, RotationUtil.degrees(1.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(5, RotationUtil.degrees(-8.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Nose translation follows the reactive pull; Y negated from posVec
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.NOSE_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(1, new Vec3(0.0, 0.02, 0.02), Easing.LINEAR), // posVec(0,-0.02,0.02)  Y negated
                                new Keyframe<>(5, new Vec3(0.0, -0.15, -0.15), Easing.LINEAR), // posVec(0,0.15,-0.15)  Y negated
                                new Keyframe<>(10, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                // Torso leans far back into the yank then snaps upright as line clears
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.BODY_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(3.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(4, RotationUtil.degrees(-25.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(7, RotationUtil.degrees(-15.0f, 0.0f, 0.0f), Easing.LINEAR),
                                new Keyframe<>(10, RotationUtil.degrees(0.0f, 0.0f, 0.0f), Easing.LINEAR)
                        ))
                        .build())
                // Torso translation drives the weight backward and up on the yank; Y negated
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.BODY_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(4, new Vec3(0.0, 0.8, -2.0), Easing.LINEAR), // posVec(0,-0.8,-2.0)  Y negated
                                new Keyframe<>(7, new Vec3(0.0, 0.4, -1.2), Easing.LINEAR), // posVec(0,-0.4,-1.2)  Y negated
                                new Keyframe<>(10, Vec3.ZERO, Easing.LINEAR)
                        ))
                        .build())
                .build();
    }

}
