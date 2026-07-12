package dev.breezes.settlements.application.ai.inference.plan;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Capability-specific PLAN request body. Transport envelope fields are added elsewhere.
 */
@Builder
@Getter
public final class PlanBatchRequest {

    @Singular
    private final List<VillagerPlanRequest> villagers;

}
