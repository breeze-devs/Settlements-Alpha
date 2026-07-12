package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;

/**
 * One economic surplus offered to SIS as prioritization context in a PLAN request.
 * <p>
 * {@code sellable} and {@code dumpable} are mod-resolved decisions from the StockPolicy ladder —
 * the mod has already classified how much of the surplus is worth trading versus discarding — not
 * raw stock counts. As with {@link DemandDTO}, this keeps the model out of threshold math it
 * cannot reliably reproduce.
 */
@Builder
@Getter
public final class SurplusDTO {

    /**
     * The surplus match's string form: an item id, a tag such as {@code c:foods}, or a set label.
     */
    private final String token;

    private final int sellable;

    private final int dumpable;

}
