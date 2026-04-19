package dev.breezes.settlements.domain.generation.history;

import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@CustomLog
public class HistoryEventEngine {

    private static final int MAX_DRAWS = 32;

    private final HistoryEventRegistry eventRegistry;
    private final TraitRegistry traitRegistry;

    public HistoryEventEngine(@Nonnull HistoryEventRegistry eventRegistry, @Nonnull TraitRegistry traitRegistry) {
        this.eventRegistry = eventRegistry;
        this.traitRegistry = traitRegistry;
    }

    public HistoryEventResult roll(@Nonnull SettlementProfile profile,
                                   @Nonnull SiteReport report,
                                   @Nonnull Random random) {
        List<HistoryEventDefinition> eligible = this.eventRegistry.allEvents().stream()
                .filter(event -> event.preconditions().isSatisfiedBy(profile, report))
                .toList();

        if (eligible.isEmpty()) {
            return passthrough(profile);
        }

        List<HistoryEventDefinition> drawn = weightedSampleWithoutReplacement(eligible, Math.min(MAX_DRAWS, eligible.size()), random);
        List<RolledHistoryEvent> rolled = drawn.stream()
                .map(event -> new RolledHistoryEvent(event, rollTimeHorizon(event, random)))
                .sorted(Comparator.comparingInt(RolledHistoryEvent::timeHorizon)
                        .thenComparing(rolledEvent -> rolledEvent.definition().id()))
                .toList();

        List<HistoryEventDefinition> survivors = filterExclusiveTags(rolled).stream()
                .limit(profile.scaleTier().maxHistoryEvents())
                .toList();

        Map<TraitId, Float> modifiedWeights = applyModifiers(profile, survivors);
        List<String> eventIds = survivors.stream().map(HistoryEventDefinition::id).toList();
        VisualMarkerSet visualMarkers = new VisualMarkerSet(survivors.stream()
                .flatMap(event -> event.visualMarkers().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        log.info("Rolled history events produced visual markers: {}", visualMarkers.markers());

        return new HistoryEventResult(eventIds, modifiedWeights, visualMarkers);
    }

    private HistoryEventResult passthrough(SettlementProfile profile) {
        return new HistoryEventResult(List.of(), profile.adjustedWeights(), VisualMarkerSet.EMPTY);
    }

    private static int rollTimeHorizon(HistoryEventDefinition event, Random random) {
        if (event.timeHorizonMin() == event.timeHorizonMax()) {
            return event.timeHorizonMin();
        }
        return random.nextInt(event.timeHorizonMin(), event.timeHorizonMax() + 1);
    }

    private static List<HistoryEventDefinition> weightedSampleWithoutReplacement(List<HistoryEventDefinition> candidates,
                                                                                 int count,
                                                                                 Random random) {
        List<HistoryEventDefinition> pool = new ArrayList<>(candidates);
        List<HistoryEventDefinition> selected = new ArrayList<>(count);

        while (selected.size() < count && !pool.isEmpty()) {
            float totalWeight = 0.0f;
            for (HistoryEventDefinition event : pool) {
                totalWeight += event.probabilityWeight();
            }

            float roll = random.nextFloat() * totalWeight;
            float cumulative = 0.0f;
            int selectedIndex = pool.size() - 1;
            for (int i = 0; i < pool.size(); i++) {
                cumulative += pool.get(i).probabilityWeight();
                if (roll < cumulative) {
                    selectedIndex = i;
                    break;
                }
            }

            HistoryEventDefinition chosen = pool.get(selectedIndex);
            int lastIndex = pool.size() - 1;
            if (selectedIndex != lastIndex) {
                // Pool order does not matter during sampling, so swap-remove avoids O(N) element shifting.
                pool.set(selectedIndex, pool.get(lastIndex));
            }
            pool.remove(lastIndex);
            selected.add(chosen);
        }

        return selected;
    }

    private static List<HistoryEventDefinition> filterExclusiveTags(List<RolledHistoryEvent> rolledEvents) {
        Set<String> claimedTags = new HashSet<>();
        List<HistoryEventDefinition> survivors = new ArrayList<>();

        for (RolledHistoryEvent rolledEvent : rolledEvents) {
            HistoryEventDefinition definition = rolledEvent.definition();
            boolean conflicts = definition.exclusiveTags().stream().anyMatch(claimedTags::contains);
            if (conflicts) {
                continue;
            }
            survivors.add(definition);
            claimedTags.addAll(definition.exclusiveTags());
        }

        return survivors;
    }

    private Map<TraitId, Float> applyModifiers(SettlementProfile profile, List<HistoryEventDefinition> survivors) {
        Map<TraitId, Float> modifiedWeights = new LinkedHashMap<>(profile.adjustedWeights());
        Set<TraitId> knownTraits = this.traitRegistry.allTraitIds();

        for (HistoryEventDefinition event : survivors) {
            for (Map.Entry<TraitId, Float> modifier : event.traitModifiers().entrySet()) {
                TraitId traitId = modifier.getKey();
                if (!knownTraits.contains(traitId)) {
                    log.warn("Skipping unknown trait modifier '{}' referenced by history event '{}'", traitId, event.id());
                    continue;
                }

                float current = modifiedWeights.getOrDefault(traitId, 0.0f);
                float updated = clamp(current + modifier.getValue(), 0.0f, 1.0f);
                modifiedWeights.put(traitId, updated);
            }
        }

        return modifiedWeights;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RolledHistoryEvent(HistoryEventDefinition definition, int timeHorizon) {
    }

}
