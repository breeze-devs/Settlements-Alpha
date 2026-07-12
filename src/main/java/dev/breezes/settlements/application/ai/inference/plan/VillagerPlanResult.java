package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PLAN response payload for one villager.
 * <p>
 * The overlay is a behavior selection — which behaviors, ordered, per window — where any
 * selection may additionally carry a specific civil half-hour via {@link PlanSelection#getAt()}.
 * Per-slot reasons are intentionally absent: the day-plan model was deliberately slimmed of plan
 * reasons, so re-introducing them (on the wire and as {@code PlanSlot.reason} together) is a P1.5
 * immersion concern kept out of this slice to hold the response payload minimal.
 */
@Builder
@Getter
public final class VillagerPlanResult {

    private final UUID villagerId;

    /**
     * windowId -> ordered list of {@link PlanSelection}s the villager intends, best-first. The
     * ordering is the intent for selections without {@code at} — the head of each window's list is
     * the villager's strongest preference. A selection carrying {@code at} is a pinned placement
     * instead; its position in the list only sets conflict priority against other pins.
     */
    @Singular
    private final Map<String, List<PlanSelection>> selections;

}
