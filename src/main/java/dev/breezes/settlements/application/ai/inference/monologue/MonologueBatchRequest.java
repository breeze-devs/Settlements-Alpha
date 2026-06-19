package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Capability-specific MONOLOGUE request body. Transport envelope fields are added elsewhere.
 */
@Builder
@Getter
public final class MonologueBatchRequest {

    private final String locale;

    @Singular
    private final List<VillagerMonologueRequest> villagers;

}
