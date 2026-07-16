package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.dialogue.RehearsedDialogueConfig;
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
 * Centralizing assembly here means both the dev-tool dump command and the evening sweep
 * drive from the same request shape — the sweep cannot silently diverge from what dump fixtures captured.
 * <p>
 * Persona content (including facets and anchors) is built by {@link PersonaBundleAssembler} and the
 * spatial snapshot by {@link SnapshotAssembler}, so this class only stitches together the
 * per-villager request: persona, snapshot, and the occasion buckets. Phrasing is owned by SIS;
 * this assembler sends structure, never rendered strings.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class MonologueRequestAssembler {

    private final PersonaBundleAssembler personaBundleAssembler;
    private final SnapshotAssembler snapshotAssembler;
    private final InferenceConfig inferenceConfig;
    private final RehearsedDialogueConfig rehearsedDialogueConfig;
    private final EpisodicEntryAssembler episodicEntryAssembler;

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
     * One batch covers all villagers needing refreshed packs, keeping backend round trips bounded
     * at one per sweep regardless of village size.
     */
    public MonologueBatchRequest assembleForVillagers(@Nonnull Map<BaseVillager, Collection<Occasion>> villagersWithOccasions) {
        MonologueBatchRequest.MonologueBatchRequestBuilder builder = MonologueBatchRequest.builder()
                .locale(this.inferenceConfig.locale());
        for (Map.Entry<BaseVillager, Collection<Occasion>> entry : villagersWithOccasions.entrySet()) {
            builder.villager(this.buildVillagerRequest(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private VillagerMonologueRequest buildVillagerRequest(@Nonnull BaseVillager villager,
                                                          @Nonnull Collection<Occasion> occasions) {
        List<OccasionBucketSpec> buckets = occasions.stream()
                .map(occasion -> OccasionBucketSpec.builder()
                        .occasion(occasion)
                        .lineCount(this.rehearsedDialogueConfig.packLinesPerVillager())
                        .build())
                .toList();

        // Read the snapshot during synchronous server-thread assembly: SensedSiteReader does
        // lazy-expiry mutation and is server-thread-only, so it must never run in the gateway's
        // async callback. An empty read still serializes as {"sites":{}}, which SIS requires.
        Snapshot snapshot = this.snapshotAssembler.assemble(villager);

        // Assemble episodic entries on the server thread alongside the snapshot read
        UUID villagerId = villager.getUUID();
        long currentTick = villager.level().getGameTime();
        List<EpisodicEntryDTO> episodic = this.episodicEntryAssembler.assemble(villagerId, villager.getKnowledgeStore(), currentTick);

        VillagerMonologueRequest.VillagerMonologueRequestBuilder requestBuilder = VillagerMonologueRequest.builder()
                .villagerId(villagerId)
                .persona(this.personaBundleAssembler.assemble(villager))
                .snapshot(snapshot);

        buckets.forEach(requestBuilder::bucket);
        episodic.forEach(requestBuilder::episodic);
        return requestBuilder.build();
    }

}
