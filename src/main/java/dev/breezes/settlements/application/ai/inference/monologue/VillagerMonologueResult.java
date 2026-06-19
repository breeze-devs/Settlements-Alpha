package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MONOLOGUE response payload for one villager, grouped by occasion bucket.
 */
@Builder
@Getter
public final class VillagerMonologueResult {

    private final UUID villagerId;

    @Singular
    private final Map<Occasion, List<GeneratedLine>> buckets;

}
