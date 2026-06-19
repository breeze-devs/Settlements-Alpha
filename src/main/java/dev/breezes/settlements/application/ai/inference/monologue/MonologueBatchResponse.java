package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Parsed MONOLOGUE capability response.
 */
@Builder
@Getter
public final class MonologueBatchResponse {

    @Singular
    private final List<VillagerMonologueResult> villagers;

}
