package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.UUID;

/**
 * MONOLOGUE request payload for one villager.
 */
@Builder
@Getter
public final class VillagerMonologueRequest {

    private final UUID villagerId;
    private final PersonaBundle persona;

    @Singular
    private final List<DialogueFacet> facets;

    @Singular
    private final List<String> seeds;

    @Singular
    private final List<OccasionBucketSpec> buckets;

}
