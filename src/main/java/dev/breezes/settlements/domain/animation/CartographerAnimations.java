package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CartographerAnimations {

    public static final int MARK_DURATION_TICKS = 40;

    private static final int BLEND_IN_TICKS = 2;
    private static final int BLEND_OUT_TICKS = 3;

    // Spyglass sweep keyframe ticks
    public static final int SURVEY_DURATION_TICKS = 160;
    private static final int SURVEY_RAISE_TICK = 12;
    private static final int SURVEY_LEFT_TICK = 42;
    private static final int SURVEY_RIGHT_TICK = 92;
    private static final int SURVEY_CENTER_TICK = 148;

    // Map marking keyframe ticks
    private static final int MAP_RAISE_TICK = 8;
    private static final int MAP_MARK_TICK_1 = 16;
    private static final int MAP_HOLD_TICK = 24;
    private static final int MAP_MARK_TICK_2 = 32;

    public static KeyframeAnimation surveyWithSpyglass() {
        // HEAD_ROTATION_OVERRIDE is anchored at the first and last ticks so its track spans the full animation.
        // The sweep progresses left → right → center; LOOK_* poses bundle head + arms so the
        // spyglass tracks the gaze instead of staying pointed forward.
        Pose restWithHead = CartographerPoses.ARMS_REST.with(CartographerPoses.HEAD_NEUTRAL);

        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/cartographer/survey_with_spyglass"))
                .durationTicks(SURVEY_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, restWithHead, Easing.EASE_OUT)
                .at(SURVEY_RAISE_TICK, CartographerPoses.LOOK_CENTER, Easing.EASE_IN)
                .at(SURVEY_LEFT_TICK, CartographerPoses.LOOK_LEFT, Easing.EASE_IN_OUT)
                .at(SURVEY_RIGHT_TICK, CartographerPoses.LOOK_RIGHT, Easing.EASE_IN_OUT)
                .at(SURVEY_CENTER_TICK, CartographerPoses.LOOK_CENTER, Easing.EASE_IN)
                .at(SURVEY_DURATION_TICKS, restWithHead, Easing.LINEAR)
                .build();
    }

    public static KeyframeAnimation markMap() {
        Pose restFull = CartographerPoses.ARMS_REST
                .with(CartographerPoses.HEAD_NEUTRAL)
                .with(CartographerPoses.REACH_NONE);
        Pose heldOpen = CartographerPoses.MAP_ARMS
                .with(CartographerPoses.HEAD_DOWN)
                .with(CartographerPoses.REACH_NONE);
        Pose marking = CartographerPoses.MAP_ARMS
                .with(CartographerPoses.HEAD_DOWN)
                .with(CartographerPoses.MARK_REACH);

        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/cartographer/mark_map"))
                .durationTicks(MARK_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(BLEND_IN_TICKS)
                .blendOutTicks(BLEND_OUT_TICKS)
                .at(0, restFull, Easing.EASE_OUT)
                .at(MAP_RAISE_TICK, heldOpen, Easing.EASE_OUT)
                .at(MAP_MARK_TICK_1, marking, Easing.EASE_IN)
                .at(MAP_HOLD_TICK, heldOpen, Easing.EASE_OUT)
                .at(MAP_MARK_TICK_2, marking, Easing.EASE_IN)
                .at(MARK_DURATION_TICKS, restFull, Easing.LINEAR)
                .build();
    }

}
