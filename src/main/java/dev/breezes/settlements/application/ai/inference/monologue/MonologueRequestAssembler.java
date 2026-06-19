package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueConfig;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds {@link VillagerMonologueRequest} and {@link MonologueBatchRequest} from live villager state.
 * <p>
 * Centralizing assembly here means both the P2 dev-tool dump command and the future P3 evening sweep
 * drive from the same request shape — the sweep cannot silently diverge from what P2 fixtures captured.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MonologueRequestAssembler {

    private final PersonaBundleAssembler personaBundleAssembler;
    private final VillagerFacetDeriver facetDeriver;
    private final MonologueSeedProjector seedProjector;
    private final InferenceConfig inferenceConfig;
    private final DialogueConfig dialogueConfig;

    /**
     * Assembles a single-villager {@link MonologueBatchRequest} for the given set of occasions.
     * <p>
     * Each occasion becomes one {@link OccasionBucketSpec} with the configured pack line count,
     * so the service generates the right number of lines per occasion in one round trip.
     */
    public MonologueBatchRequest assembleForVillager(@Nonnull BaseVillager villager,
                                                     @Nonnull Collection<Occasion> occasions) {
        VillagerMonologueRequest villagerRequest = this.buildVillagerRequest(villager, occasions);
        return MonologueBatchRequest.builder()
                .locale(this.inferenceConfig.locale())
                .villager(villagerRequest)
                .build();
    }

    /**
     * Assembles a multi-villager {@link MonologueBatchRequest} for the given villager-to-occasions mapping.
     * <p>
     * Intended for the P3 evening sweep: one batch encompasses all villagers that need refreshed packs,
     * keeping the number of backend round trips bounded at one per sweep regardless of village size.
     */
    public MonologueBatchRequest assembleForVillagers(
            @Nonnull Map<BaseVillager, Collection<Occasion>> villagersWithOccasions) {
        MonologueBatchRequest.MonologueBatchRequestBuilder builder = MonologueBatchRequest.builder()
                .locale(this.inferenceConfig.locale());
        for (Map.Entry<BaseVillager, Collection<Occasion>> entry : villagersWithOccasions.entrySet()) {
            builder.villager(this.buildVillagerRequest(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private VillagerMonologueRequest buildVillagerRequest(@Nonnull BaseVillager villager,
                                                          @Nonnull Collection<Occasion> occasions) {
        UUID observerId = villager.getUUID();
        List<String> seeds = this.seedProjector.project(observerId, villager.getKnowledgeStore());
        List<OccasionBucketSpec> buckets = occasions.stream()
                .map(occasion -> OccasionBucketSpec.builder()
                        .occasion(occasion)
                        .lineCount(this.dialogueConfig.packLinesPerVillager())
                        .build())
                .toList();

        VillagerMonologueRequest.VillagerMonologueRequestBuilder requestBuilder =
                VillagerMonologueRequest.builder()
                        .villagerId(villager.getUUID())
                        .persona(this.personaBundleAssembler.assemble(villager));

        this.facetDeriver.derive(villager).forEach(requestBuilder::facet);
        seeds.forEach(requestBuilder::seed);
        buckets.forEach(requestBuilder::bucket);

        return requestBuilder.build();
    }

}
