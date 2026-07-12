package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;

/**
 * One unmet economic demand offered to SIS as prioritization context in a PLAN request.
 * <p>
 * {@code shortfall} and {@code priority} are mod-resolved decisions — the mod has already
 * evaluated the villager's stock thresholds and ranked the result — not raw inventory counts for
 * the model to re-derive. This keeps threshold math a single-owner concern (the economy system)
 * and keeps the model's job to sequencing/judgment rather than re-doing arithmetic it would get
 * wrong or spend tokens on.
 * <p>
 * Callers must supply the list already sorted by descending priority; this DTO does not carry
 * ordering information beyond list position, so the wire order is the priority order.
 */
@Builder
@Getter
public final class DemandDTO {

    /**
     * The demand match's string form: an item id, a tag such as {@code c:foods}, or a set label.
     */
    private final String token;

    private final int shortfall;

    private final int priority;

}
