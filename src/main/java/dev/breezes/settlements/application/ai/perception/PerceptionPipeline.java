package dev.breezes.settlements.application.ai.perception;

import dev.breezes.settlements.application.ai.memory.MemoryAdmissionGate;
import dev.breezes.settlements.application.ai.socialcue.SocialCueRuntimeState;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.memory.PackedPos;
import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationBuffer;
import dev.breezes.settlements.domain.ai.perception.ObservationFactory;
import dev.breezes.settlements.domain.ai.perception.PerceptionGate;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.SectionPos;

import java.util.Map;

/**
 * Application-layer pipeline that activates the per-villager observation scaffolding.
 * <p>
 * Consumer of the {@link WorldEventBus} cursor, running between {@link PerceptionGate} and the
 * villager's {@link VillagerKnowledgeStore} with {@link MemoryAdmissionGate} deciding what gets
 * that far.
 * <p>
 * The hard anti-telepathy rule is enforced here: an entry reaches a villager's store only through
 * that villager's own perception of the event, never by being handed one.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PerceptionPipeline {

    private final WorldEventBus worldEventBus;
    private final MemoryAdmissionGate admissionGate;

    /**
     * Runs the full perception pass for one villager
     * Should be called from {@code BaseVillager.customServerAiStep()} after the brain tick.
     *
     * @param villager     the villager being ticked
     * @param runtimeState the villager's SocialCue runtime state (holds cursor + buffer)
     * @param gameTime     current game time in ticks
     */
    public void tick(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        long lastSeenSeq = runtimeState.getLastSeenSeq();

        int villagerChunkX = SectionPos.blockToSectionCoord((int) Math.floor(villager.getX()));
        int villagerChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(villager.getZ()));

        ObservationBuffer buffer = runtimeState.getObservationBuffer();
        long newSeq = this.worldEventBus.visitDelta(lastSeenSeq, event -> {
            if (!PerceptionGate.admits(event, villagerChunkX, villagerChunkZ)) {
                return;
            }

            buffer.add(ObservationFactory.fromEvent(event, gameTime));
        });

        if (newSeq == lastSeenSeq) {
            return;
        }

        // Advance even when every event was filtered out; otherwise rejected events would be re-read forever.
        runtimeState.advanceCursor(newSeq);

        if (buffer.isEmpty()) {
            return;
        }

        VillagerKnowledgeStore knowledgeStore = villager.getKnowledgeStore();
        for (Observation observation : buffer.drain()) {
            if (!this.admissionGate.admits(observation)) {
                continue;
            }

            Map<String, String> metadata = ObservationFactory.metadataFor(observation);
            long packedPos = PackedPos.asLong(
                    (int) Math.floor(observation.posX()),
                    (int) Math.floor(observation.posY()),
                    (int) Math.floor(observation.posZ()));
            // The event type's base importance is the only salience signal available, so it stands
            // in as the entry's weight: it ranks types against each other, which is what the
            // episodic ordering needs, even though it varies with nothing about this villager.
            KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(observation.id(),
                    observation.type(), observation.timestampTick(), observation.timestampTick(),
                    observation.relatedEntity(), metadata, observation.baseImportance(), packedPos);
            knowledgeStore.admit(entry);
        }
    }

}
