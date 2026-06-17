package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Persistence helper for {@link VillagerKnowledgeStore}.
 * Converts between the domain store and its flat NBT attachment representation.
 * Follows the same pattern as {@link VillagerGeneticsAttachment}.
 */
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
                    .content(entry.getContent())
                    .type(entry.getType())
                    .originTimestampTick(entry.getOriginTimestampTick())
                    .admittedAtTick(entry.getAdmittedAtTick())
                    .relatedEntity(entry.getRelatedEntity())
                    .metadata(KnowledgeMetadataSanitizer.sanitize(entry.getMetadata()))
                    .source(entry.getSource())
                    .hop(entry.getHop())
                    .weight(entry.getWeight())
                    .originalWeight(entry.getOriginalWeight())
                    .resolution(entry.getResolution())
                    .corroborationCount(entry.getCorroborationCount())
                    .investigationAttempts(entry.getInvestigationAttempts())
                    .nextEligibleTick(entry.getNextEligibleTick())
                    .build());
        }
        villager.setData(AttachmentRegistry.VILLAGER_KNOWLEDGE, VillagerKnowledgeAttachmentState.of(states));
    }

    /**
     * Reads the attachment back into the provided {@link VillagerKnowledgeStore}.
     * Called from {@link BaseVillager#load}.
     *
     * @return {@code true} if persisted data was found and loaded; {@code false} on a fresh spawn
     */
    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull VillagerKnowledgeStore store) {
        VillagerKnowledgeAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_KNOWLEDGE);
        if (!state.initialized()) {
            return false;
        }

        for (KnowledgeEntryState entryState : state.entries()) {
            KnowledgeEntry entry = KnowledgeEntry.builder()
                    .originObservationId(entryState.originObservationId())
                    .content(entryState.content())
                    .type(entryState.type())
                    .originTimestampTick(entryState.originTimestampTick())
                    .admittedAtTick(entryState.admittedAtTick())
                    .relatedEntity(entryState.relatedEntity())
                    .metadata(entryState.metadata())
                    .source(entryState.source())
                    .hop(entryState.hop())
                    .weight(entryState.weight())
                    .originalWeight(entryState.originalWeight())
                    .resolution(entryState.resolution())
                    .corroborationCount(entryState.corroborationCount())
                    .investigationAttempts(entryState.investigationAttempts())
                    .nextEligibleTick(entryState.nextEligibleTick())
                    .build();
            store.admit(entry);
        }
        return true;
    }

}
