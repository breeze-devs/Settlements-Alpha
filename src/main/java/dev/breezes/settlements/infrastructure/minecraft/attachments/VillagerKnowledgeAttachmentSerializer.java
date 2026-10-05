package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.ObservationMetadataKeys;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The entry's semantic type is not persisted (see {@link KnowledgeEntryState}); reading derives it from the event_type
 * metadata key, and drops any entry whose key no longer resolves.
 */
@CustomLog
public final class VillagerKnowledgeAttachmentSerializer
        extends StateCodecAttachmentSerializer<List<KnowledgeEntryState>, VillagerKnowledgeStore> {

    public VillagerKnowledgeAttachmentSerializer() {
        super(VillagerKnowledgeAttachmentCodec.STATE_CODEC);
    }

    /**
     * An empty store with the configured capacity.
     */
    public static VillagerKnowledgeStore emptyStore() {
        // The root graph rather than the server graph: a default is created wherever the attachment is first read,
        // which can be the client
        return new VillagerKnowledgeStore(SettlementsDagger.component().eventLaneConfig().knowledgeStoreMaxEntries());
    }

    @Override
    List<KnowledgeEntryState> toState(@Nonnull VillagerKnowledgeStore store) {
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
                    .weight(entry.getWeight())
                    .build());
        }
        return states;
    }

    @Override
    VillagerKnowledgeStore fromState(@Nonnull List<KnowledgeEntryState> entryStates) {
        VillagerKnowledgeStore store = emptyStore();
        for (KnowledgeEntryState entryState : entryStates) {
            Map<String, String> metadata = entryState.metadata();
            ObservationType type = resolveObservationType(metadata);
            if (type == null) {
                // Dropped: data that can no longer be classified is pruned
                continue;
            }

            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .originObservationId(entryState.originObservationId())
                    .type(type)
                    .originTimestampTick(entryState.originTimestampTick())
                    .admittedAtTick(entryState.admittedAtTick())
                    .relatedEntity(entryState.relatedEntity())
                    .metadata(metadata)
                    .packedPos(entryState.packedPos())
                    .weight(entryState.weight())
                    .build();
            store.admit(entry);
        }
        return store;
    }

    /**
     * Derives the {@link ObservationType} from the persisted event_type metadata key.
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
