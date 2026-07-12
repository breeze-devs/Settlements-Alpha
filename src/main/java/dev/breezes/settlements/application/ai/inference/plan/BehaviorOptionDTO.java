package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * One selectable behavior offered to SIS in a PLAN request's closed option set.
 * <p>
 * {@code id} is a {@link dev.breezes.settlements.domain.ai.catalog.BehaviorKey} id and drives
 * SIS's structured-decoding enum, so a returned selection can be mapped back to a concrete
 * behavior unambiguously. {@code category} and {@code intensity} are the raw
 * {@code BehaviorCategory}/{@code WorkIntensity} enum names — SIS reasons about them as opaque
 * tokens, the mod owns their meaning.
 * <p>
 * {@code requiredItems} and {@code produces} are authored natural language rather than structured
 * item/count data: the catalog of "what a behavior consumes/yields" is not cleanly enumerable
 * across the existing behavior set (some behaviors touch multiple item kinds, quantities vary by
 * villager state), so a short authored phrase is cheaper and clearer for the model than a partial
 * structured shape. Both are omitted from the wire entirely when not authored for a behavior.
 */
@Builder
@Getter
public final class BehaviorOptionDTO {

    private final String id;

    private final String description;

    @Nullable
    private final String requiredItems;

    @Nullable
    private final String produces;

    private final String category;

    private final String intensity;

    private final int estimatedMinutes;

    /**
     * Whether the behavior's declared opportunity requirements are currently satisfied, as
     * pre-evaluated by {@code OpportunityForecaster} at plan-generation time.
     */
    private final boolean hasOpportunity;

}
