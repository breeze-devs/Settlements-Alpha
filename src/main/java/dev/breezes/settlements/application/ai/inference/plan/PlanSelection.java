package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * One villager-authored menu pick within a PLAN response window: a behavior id, optionally pinned
 * to a specific civil half-hour via {@code at}.
 * <p>
 * {@code at} is the literal {@link dev.breezes.settlements.domain.time.TimeOfDay} enum name (e.g.
 * {@code "AT_18_00"}) or absent — Gson (de)serializes it as a plain string, no custom adapter
 * needed. Present means "this must happen at this half-hour"; absent means "sometime in this
 * window, in list order."
 */
@Builder
@Getter
public final class PlanSelection {

    private final String id;

    @Nullable
    private final String at;

}
