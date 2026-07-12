package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;

/**
 * A coarse tick-range window within which SIS orders a subset of the menu.
 * <p>
 * Bounds only — the assembler (a later slice) computes {@code startTick}/{@code endTick} from
 * the day plan; this DTO just carries the shape SIS needs to reason about ordering within a window.
 */
@Builder
@Getter
public final class PlanWindowDTO {

    private final String id;

    private final int startTick;

    private final int endTick;

}
