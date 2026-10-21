package dev.breezes.settlements.domain.generation.history;

import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
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

    // Events rolled at a time horizon below this belong to the founding era and are dated at the founding. The rest are
    // spread between the founding and the record, keeping their horizon order.
    private static final int LATER_ERA_START = 200;

    @VisibleForTesting
    static final int MIN_FOUNDING_AGE_YEARS = 20;
    @VisibleForTesting
    static final int MAX_FOUNDING_AGE_YEARS = 150;

    private final HistoryEventRegistry eventRegistry;
    private final TraitRegistry traitRegistry;

    public HistoryEventEngine(@Nonnull HistoryEventRegistry eventRegistry, @Nonnull TraitRegistry traitRegistry) {
        this.eventRegistry = eventRegistry;
        this.traitRegistry = traitRegistry;
    }

    /**
     * Draws the history events of a settlement and applies how they change its trait weights and its look. The same
     * inputs and random state always draw the same events.
     *
     * @param founding whether the settlement is established or new; only events whose preconditions require the same
     *                 kind are drawn
     */
    public HistoryEventResult roll(@Nonnull SettlementProfile profile,
                                   @Nonnull SiteReport report,
                                   @Nonnull SettlementFoundingKind founding,
                                   @Nonnull Random random) {
        List<HistoryEventDefinition> eligible = this.eventRegistry.allEvents().stream()
                .filter(event -> event.preconditions().isSatisfiedBy(profile, report, founding))
                .toList();

        if (eligible.isEmpty()) {
            return new HistoryEventResult(List.of(), profile.adjustedWeights(), VisualMarkerSet.EMPTY);
        }

        List<HistoryEventDefinition> drawn = weightedSampleWithoutReplacement(eligible, Math.min(MAX_DRAWS, eligible.size()), random);
        List<RolledEvent> rolled = drawn.stream()
                .map(event -> new RolledEvent(event, rollTimeHorizon(event, random)))
                .sorted(Comparator.comparingInt(RolledEvent::timeHorizon)
                        .thenComparing(rolledEvent -> rolledEvent.definition().id()))
                .toList();

        List<RolledEvent> survivors = filterExclusiveTags(rolled).stream()
                .limit(profile.scaleTier().maxHistoryEvents())
                .toList();

        Map<TraitId, Float> modifiedWeights = applyModifiers(profile, survivors);
        VisualMarkerSet visualMarkers = new VisualMarkerSet(survivors.stream()
                .flatMap(survivor -> survivor.definition().visualMarkers().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        log.info("Rolled history events produced visual markers: {}", visualMarkers.markers());

        List<DrawnHistoryEvent> events = survivors.stream()
                .map(survivor -> new DrawnHistoryEvent(survivor.definition().id(), survivor.timeHorizon()))
                .toList();
        return new HistoryEventResult(events, modifiedWeights, visualMarkers);
    }

    /**
     * Dates the selected history events of a settlement whose record begins at the given moment.
     * <p>
     * An established settlement is founded a random number of years before its record begins. A new one is founded as
     * its record begins, every event is dated then, and nothing is drawn from random.
     *
     * @param events   the drawn events, earliest era first
     * @param recorded the moment the settlement's record begins
     * @param founding whether the settlement is established or new
     */
    public static DatedHistory date(@Nonnull List<DrawnHistoryEvent> events,
                                    @Nonnull CivilDateTime recorded,
                                    @Nonnull SettlementFoundingKind founding,
                                    @Nonnull Random random) {
        if (founding == SettlementFoundingKind.NEW) {
            return new DatedHistory(recorded, events.stream()
                    .map(event -> new HistoryEvent(event.eventId(), recorded))
                    .toList());
        }

        int foundingAgeDays = random.nextInt(MIN_FOUNDING_AGE_YEARS * WorldCalendar.DAYS_PER_YEAR,
                MAX_FOUNDING_AGE_YEARS * WorldCalendar.DAYS_PER_YEAR + 1);
        CivilDateTime founded = recorded.minus(GameTicks.days(foundingAgeDays));

        // Later-era ages are rolled independently, strictly between the founding and the record, then handed out oldest
        // first, so the dates keep the era order the events arrive in
        long laterEraCount = events.stream().filter(event -> event.timeHorizon() >= LATER_ERA_START).count();
        List<Integer> laterEraAgesDays = random.ints(laterEraCount, 1, foundingAgeDays)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .toList();

        List<HistoryEvent> dated = new ArrayList<>(events.size());
        int nextLaterEraAge = 0;
        for (DrawnHistoryEvent event : events) {
            CivilDateTime date = event.timeHorizon() < LATER_ERA_START
                    ? founded
                    : recorded.minus(GameTicks.days(laterEraAgesDays.get(nextLaterEraAge++)));
            dated.add(new HistoryEvent(event.eventId(), date));
        }
        return new DatedHistory(founded, dated);
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

    private static List<RolledEvent> filterExclusiveTags(List<RolledEvent> rolledEvents) {
        Set<String> claimedTags = new HashSet<>();
        List<RolledEvent> survivors = new ArrayList<>();

        for (RolledEvent rolledEvent : rolledEvents) {
            HistoryEventDefinition definition = rolledEvent.definition();
            boolean conflicts = definition.exclusiveTags().stream().anyMatch(claimedTags::contains);
            if (conflicts) {
                continue;
            }
            survivors.add(rolledEvent);
            claimedTags.addAll(definition.exclusiveTags());
        }

        return survivors;
    }

    private Map<TraitId, Float> applyModifiers(SettlementProfile profile, List<RolledEvent> survivors) {
        Map<TraitId, Float> modifiedWeights = new LinkedHashMap<>(profile.adjustedWeights());
        Set<TraitId> knownTraits = this.traitRegistry.allTraitIds();

        for (RolledEvent survivor : survivors) {
            HistoryEventDefinition event = survivor.definition();
            for (Map.Entry<TraitId, Float> modifier : event.traitModifiers().entrySet()) {
                TraitId traitId = modifier.getKey();
                if (!knownTraits.contains(traitId)) {
                    log.warn("Skipping unknown trait modifier '{}' referenced by history event '{}'", traitId, event.id());
                    continue;
                }

                float current = modifiedWeights.getOrDefault(traitId, 0.0f);
                float updated = Math.clamp(current + modifier.getValue(), 0.0f, 1.0f);
                modifiedWeights.put(traitId, updated);
            }
        }

        return modifiedWeights;
    }

    /**
     * A settlement's history dated on the world's calendar.
     *
     * @param founded when the settlement was founded
     * @param events  its history events, earliest first
     */
    public record DatedHistory(CivilDateTime founded, List<HistoryEvent> events) {
    }

    private record RolledEvent(HistoryEventDefinition definition, int timeHorizon) {
    }

}
