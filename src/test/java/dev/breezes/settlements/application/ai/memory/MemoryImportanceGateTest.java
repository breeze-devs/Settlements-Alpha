package dev.breezes.settlements.application.ai.memory;

import dev.breezes.settlements.domain.ai.observation.Observation;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryImportanceGateTest {

    private final MemoryImportanceGate gate = new MemoryImportanceGate();

    @Test
    void score_scoresThreatObservationsHigherThanRoutineOnes() {
        GeneticsProfile genetics = genetics(0.5, 0.5, 0.5);
        Observation threat = observation(ObservationType.THREAT, "zombie near the village", 3.0F);
        Observation routine = observation(ObservationType.ENVIRONMENT, "clear weather", 1.0F);

        float threatScore = this.gate.score(threat, VillagerProfessionKey.FARMER, genetics);
        float routineScore = this.gate.score(routine, VillagerProfessionKey.FARMER, genetics);

        assertTrue(threatScore > routineScore);
        assertTrue(this.gate.shouldPromote(threatScore));
        assertFalse(this.gate.shouldPromote(routineScore));
    }

    @Test
    void score_reducesRepetitiveObservationNovelty() {
        GeneticsProfile genetics = genetics(0.5, 0.5, 0.5);
        Observation observation = observation(ObservationType.ENVIRONMENT, "clear weather", 1.5F);
        // Peers = other observations of the same type (not including the observation itself)
        List<Observation> peers = List.of(observation, observation, observation);

        float novelScore = this.gate.score(observation, VillagerProfessionKey.FARMER, genetics, List.of());
        float repeatedScore = this.gate.score(observation, VillagerProfessionKey.FARMER, genetics, peers);

        assertTrue(novelScore > repeatedScore);
    }

    /**
     * Pins the corrected novelty behavior: a lone observation of a unique type should score
     * novelty=1.5 (the "nothing like this" tier), not 1.0 (the "seen one before" tier).
     * <p>
     * The pre-P5 bug passed the full drained batch (including the observation itself) as the
     * recentContext list, so the count of same-type observations was always ≥1 even for a
     * lone unique observation, yielding novelty=1.0 instead of 1.5 (~33% systematic dampening).
     * The fix: pass only the *other* observations in the batch (exclude self) as the peer list.
     */
    @Test
    void score_uniqueObservationScoredAgainstEmptyPeersGetsMaxNovelty() {
        // Arrange – a single SOCIAL observation with no peers of the same type
        GeneticsProfile genetics = genetics(0.5, 0.5, 0.5);
        Observation unique = observation(ObservationType.SOCIAL, "greeted a neighbor", 1.0F);

        // Act – peers list is empty (correct: no other observations in this batch)
        float scoreWithNoPeers = this.gate.score(unique, VillagerProfessionKey.FARMER, genetics, List.of());

        // Act – peers list contains self (the old buggy behavior)
        float scoreWithSelfInPeers = this.gate.score(unique, VillagerProfessionKey.FARMER, genetics, List.of(unique));

        // Assert – excluding self yields the 1.5 novelty tier (unique-type lone observation)
        // while including self yields the lower 1.0 tier
        assertTrue(scoreWithNoPeers > scoreWithSelfInPeers,
                "Excluding self from peers should yield higher novelty score than including self");
    }

    @Test
    void score_withSimilarPeerCountMatchesListBasedNovelty() {
        // Arrange
        GeneticsProfile genetics = genetics(0.5, 0.5, 0.5);
        Observation observation = observation(ObservationType.ENVIRONMENT, "clear weather", 1.5F);
        List<Observation> peers = List.of(
                observation(ObservationType.ENVIRONMENT, "clouds", 1.0F),
                observation(ObservationType.ENVIRONMENT, "wind", 1.0F),
                observation(ObservationType.SOCIAL, "hello", 1.0F));

        // Act
        float listScore = this.gate.score(observation, VillagerProfessionKey.FARMER, genetics, peers);
        float countScore = this.gate.score(observation, VillagerProfessionKey.FARMER, genetics, 2);

        // Assert
        assertEquals(listScore, countScore, 0.001f);
    }

    @Test
    void score_socialObservationsBenefitFromHighCharisma() {
        Observation social = observation(ObservationType.SOCIAL, "talked with a neighbor", 2.0F);

        float lowCharismaScore = this.gate.score(social, VillagerProfessionKey.FARMER, genetics(0.5, 0.5, 0.1));
        float highCharismaScore = this.gate.score(social, VillagerProfessionKey.FARMER, genetics(0.5, 0.5, 0.9));

        assertTrue(highCharismaScore > lowCharismaScore);
    }

    private static Observation observation(ObservationType type, String content, float importance) {
        return Observation.builder()
                .id(UUID.randomUUID())
                .timestampTick(1L)
                .type(type)
                .eventType(WorldEventType.CROP_HARVESTED)
                .content(content)
                .baseImportance(importance)
                .build();
    }

    private static GeneticsProfile genetics(double intelligence, double will, double charisma) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        genes.put(GeneType.INTELLIGENCE, new Gene(intelligence));
        genes.put(GeneType.WILL, new Gene(will));
        genes.put(GeneType.CHARISMA, new Gene(charisma));
        return new GeneticsProfile(genes);
    }

}
