package dev.breezes.settlements.application.ai.perception;

import dev.breezes.settlements.application.ai.memory.MemoryImportanceGate;
import dev.breezes.settlements.application.ai.socialcue.SocialCueRuntimeState;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.knowledge.AdmitResult;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.VillagerKnowledgeStore;
import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationBuffer;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.perception.ObservationFactory;
import dev.breezes.settlements.domain.ai.perception.PerceptionGate;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventBus;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.core.SectionPos;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Application-layer pipeline that activates the per-villager observation scaffolding.
 * <p>
 * Consumer of the {@link WorldEventBus} cursor. On each call to {@link #tick} it:
 * <ol>
 *   <li>Drains the delta from the bus using the villager's {@code lastSeenSeq} cursor.</li>
 *   <li>Runs each new event through {@link PerceptionGate} (namespace + Manhattan-distance
 *       rejection). Events whose source chunk is too far away are silently discarded.</li>
 *   <li>Converts admitted events to {@link Observation}s via {@link ObservationFactory}
 *       and writes them into the per-villager {@link ObservationBuffer}.</li>
 *   <li>Drains the buffer, scores each observation through {@link MemoryImportanceGate},
 *       and promotes qualifying observations into the villager's {@link VillagerKnowledgeStore}
 *       as first-hand {@link KnowledgeEntry} records.</li>
 * </ol>
 * <p>
 * The hard anti-telepathy rule is enforced here: long-range knowledge is only possible
 * via villager-to-villager gossip, never by direct bus admission.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PerceptionPipeline {

    private static final ObservationType[] OBSERVATION_TYPES = ObservationType.values();

    private final WorldEventBus worldEventBus;
    private final MemoryImportanceGate importanceGate;

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
            // Skip events emitted by this villager itself
            if (villager.getUUID().equals(event.getActorId())) {
                return;
            }

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

        // Drain the buffer and run observations through the importance gate.
        VillagerProfessionKey professionKey = villager.getProfession();
        GeneticsProfile genetics = villager.getGenetics();
        List<Observation> observations = buffer.drain();
        int[] observationTypeFrequencies = countObservationTypes(observations);
        VillagerKnowledgeStore knowledgeStore = villager.getKnowledgeStore();
        for (Observation observation : observations) {
            int similarPeerCount = observationTypeFrequencies[observation.type().ordinal()] - 1;

            float score = this.importanceGate.score(observation, professionKey, genetics, similarPeerCount);
            if (this.importanceGate.shouldPromote(score)) {
                // Promote into the per-villager knowledge store. Direct observations are first-hand (hop=0, hearsay=false).
                Map<String, String> metadata = ObservationFactory.metadataFor(observation);
                KnowledgeEntry entry = KnowledgeEntry.fromDirectObservation(observation.id(), observation.content(),
                        observation.type(), observation.timestampTick(), observation.timestampTick(),
                        observation.relatedEntity(), metadata, score);
                AdmitResult admitted = knowledgeStore.admit(entry);
                if (admitted == AdmitResult.NEW_ENTRY) {
                    log.debug("PerceptionPipeline: promoted observation '{}' (score={}) for villager {}",
                            observation.content(), score, villager.getUUID());
                }
            }
        }
    }

    private static int[] countObservationTypes(List<Observation> observations) {
        int[] frequencies = new int[OBSERVATION_TYPES.length];
        for (Observation observation : observations) {
            frequencies[observation.type().ordinal()]++;
        }
        return frequencies;
    }

}
