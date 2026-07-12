package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Persistence helper for {@link VillagerKnowledgeStore}.
 * Converts between the domain store and its flat NBT attachment representation.
 * Follows the same pattern as {@link VillagerGeneticsAttachment}.
 * <p>
 * {@code type} and {@code weight} are not persisted (see {@link KnowledgeEntryState}) —
 * {@link #loadInto} reconstructs them from the fields that are.
 */
@CustomLog
public final class VillagerKnowledgeAttachment {

    /**
     * Writes the villager's entire knowledge store into the entity attachment.
     * Called from {@link BaseVillager#addAdditionalSaveData}.
     */
    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull VillagerKnowledgeStore store) {
        Collection<KnowledgeEntry> entries = store.entriesView();
        List<KnowledgeEntryState> states = new ArrayList<>(entries.size());
        for (KnowledgeEntry entry : entries) {
            states.add(KnowledgeEntryState.builder()
                    .originObservationId(entry.getOriginObservationId())
                    .originTimestampTick(entry.getOriginTimestampTick())
                    .admittedAtTick(entry.getAdmittedAtTick())
                    .relatedEntity(entry.getRelatedEntity())
                    .metadata(KnowledgeMetadataSanitizer.sanitize(entry.getMetadata()))
                    .packedPos(entry.getPackedPos())
                    .source(entry.getSource())
                    .hop(entry.getHop())
                    .originalWeight(entry.getOriginalWeight())
                    .corroborationCount(entry.getCorroborationCount())
                    .build());
        }
        villager.setData(AttachmentRegistry.VILLAGER_KNOWLEDGE, VillagerKnowledgeAttachmentState.of(states));
    }

    /**
     * Reads the attachment back into the provided {@link VillagerKnowledgeStore}.
     * Called from {@link BaseVillager#load}.
     * <p>
     * Entries whose {@code event_type} metadata cannot be resolved to a known
     * {@link WorldEventType} are dropped rather than admitted with a fallback type — this
     * mirrors {@link dev.breezes.settlements.application.ai.inference.monologue.EpisodicEntryAssembler}'s
     * tolerant parse and prunes data that can no longer be classified (e.g. a retired event type).
     *
     * @return {@code true} if persisted data was found and loaded; {@code false} on a fresh spawn
     */
    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull VillagerKnowledgeStore store) {
        VillagerKnowledgeAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_KNOWLEDGE);
        if (!state.initialized()) {
            return false;
        }

        for (KnowledgeEntryState entryState : state.entries()) {
            Map<String, String> metadata = entryState.metadata();
            ObservationType type = resolveObservationType(metadata);
            if (type == null) {
                continue;
            }

            float weight = KnowledgeEntry.recomputeWeight(entryState.originalWeight(),
                    entryState.corroborationCount(), VillagerKnowledgeStore.CORROBORATION_BUMP);

            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .originObservationId(entryState.originObservationId())
                    .type(type)
                    .originTimestampTick(entryState.originTimestampTick())
                    .admittedAtTick(entryState.admittedAtTick())
                    .relatedEntity(entryState.relatedEntity())
                    .metadata(metadata)
                    .packedPos(entryState.packedPos())
                    .source(entryState.source())
                    .hop(entryState.hop())
                    .weight(weight)
                    .originalWeight(entryState.originalWeight())
                    .corroborationCount(entryState.corroborationCount())
                    .build();
            store.admit(entry);
        }
        return true;
    }

    /**
     * Derives the {@link ObservationType} from the persisted {@code event_type} metadata key.
     * Returns null when the key is missing, blank, or no longer maps to a known
     * {@link WorldEventType} — callers must drop the entry in that case.
     */
    @Nullable
    static ObservationType resolveObservationType(Map<String, String> metadata) {
        String rawType = metadata.get(ObservationMetadataKeys.EVENT_TYPE);
        if (rawType == null || rawType.isBlank()) {
            log.warn("Dropping knowledge entry with missing event_type metadata on load");
            return null;
        }

        try {
            return WorldEventType.valueOf(rawType).getObservationType();
        } catch (IllegalArgumentException e) {
            log.warn("Dropping knowledge entry with unrecognized event_type on load: {}", rawType);
            return null;
        }
    }

}
