package dev.breezes.settlements.domain.generation.history;

import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitDefinition;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.survey.ElevationStats;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import dev.breezes.settlements.domain.generation.trait.TraitRegistry;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.domain.world.WorldCalendar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static dev.breezes.settlements.domain.generation.history.SettlementFoundingKind.ESTABLISHED;
import static dev.breezes.settlements.domain.generation.history.SettlementFoundingKind.NEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryEventEngineTest {

    private static final TraitId LUMBER = TraitId.of("settlements:settlement_traits/lumber");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");
    private static final TraitId CRAFT = TraitId.of("settlements:settlement_traits/craft");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId ARCANE = TraitId.of("settlements:settlement_traits/arcane");
    private static final TraitId UNKNOWN = TraitId.of("settlements:settlement_traits/unknown_history_trait");
    private static final CivilDateTime RECORDED = new CivilDateTime(30_000L);

    @Test
    void sameSeed_producesDeterministicResult() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/a", 100, 100, List.of("FOUNDING"), Map.of(DEFENSE, 0.1f), List.of("a"), EventPreconditions.NONE),
                event("settlements:event/b", 210, 210, List.of(), Map.of(CRAFT, 0.1f), List.of("b"), EventPreconditions.NONE)
        );
        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(LUMBER, DEFENSE, CRAFT));

        HistoryEventResult first = engine.roll(profile(Map.of(LUMBER, 0.5f, DEFENSE, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(12345L));
        HistoryEventResult second = engine.roll(profile(Map.of(LUMBER, 0.5f, DEFENSE, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(12345L));

        assertEquals(first, second);
    }

    @Test
    void exclusiveTags_keepEarliestRolledEvent() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/founding_b", 150, 150, List.of("FOUNDING"), Map.of(DEFENSE, 0.1f), List.of("marker_b"), EventPreconditions.NONE),
                event("settlements:event/founding_a", 100, 100, List.of("FOUNDING"), Map.of(CRAFT, 0.1f), List.of("marker_a"), EventPreconditions.NONE),
                event("settlements:event/other", 220, 220, List.of(), Map.of(LUMBER, 0.05f), List.of("other"), EventPreconditions.NONE)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(LUMBER, DEFENSE, CRAFT));
        HistoryEventResult result = engine.roll(profile(Map.of(LUMBER, 0.5f, DEFENSE, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(1L));

        assertEquals(List.of("settlements:event/founding_a", "settlements:event/other"), eventIds(result));
        assertEquals(Set.of("marker_a", "other"), result.visualMarkers().markers());
    }

    @Test
    void preconditions_filterEvents() {
        EventPreconditions requiresLumber = EventPreconditions.builder()
                .minTraitWeights(Map.of(LUMBER, 0.3f))
                .build();
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/fire", 200, 200, List.of(), Map.of(), List.of(), requiresLumber)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(LUMBER));
        HistoryEventResult result = engine.roll(profile(Map.of(LUMBER, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(2L));

        assertTrue(eventIds(result).isEmpty());
        assertEquals(Map.of(LUMBER, 0.2f), result.modifiedWeights());
    }

    @Test
    void modifiersClampAndCanInsertKnownTraits() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/boost", 100, 100, List.of(), Map.of(DEFENSE, 0.9f, ARCANE, 0.3f), List.of(), EventPreconditions.NONE),
                event("settlements:event/reduce", 200, 200, List.of(), Map.of(DEFENSE, -1.5f), List.of(), EventPreconditions.NONE)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(DEFENSE, ARCANE));
        HistoryEventResult result = engine.roll(profile(Map.of(DEFENSE, 0.4f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(3L));

        assertEquals(0.0f, result.modifiedWeights().get(DEFENSE), 0.0001f);
        assertEquals(0.3f, result.modifiedWeights().get(ARCANE), 0.0001f);
    }

    @Test
    void unknownTraitModifierIsSkipped() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/unknown", 100, 100, List.of(), Map.of(UNKNOWN, 0.4f, DEFENSE, 0.1f), List.of(), EventPreconditions.NONE)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(DEFENSE));
        HistoryEventResult result = engine.roll(profile(Map.of(DEFENSE, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(4L));

        assertEquals(Map.of(DEFENSE, 0.3f), result.modifiedWeights());
    }

    @Test
    void emptyEligiblePoolReturnsPassthroughResult() {
        EventPreconditions requiresWater = EventPreconditions.builder()
                .requiredWaterFeatures(Set.of(WaterFeatureType.RIVER))
                .build();
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/water", 100, 100, List.of(), Map.of(DEFENSE, 0.1f), List.of("marker"), requiresWater)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry(DEFENSE));
        HistoryEventResult result = engine.roll(profile(Map.of(DEFENSE, 0.2f)), report(Set.of(), Set.of()), ESTABLISHED, new Random(5L));

        assertTrue(eventIds(result).isEmpty());
        assertTrue(result.visualMarkers().isEmpty());
        assertEquals(Map.of(DEFENSE, 0.2f), result.modifiedWeights());
    }

    @Test
    void truncatesAtScaleTierMaximumHistoryEvents() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/one", 100, 100, List.of(), Map.of(), List.of(), EventPreconditions.NONE),
                event("settlements:event/two", 110, 110, List.of(), Map.of(), List.of(), EventPreconditions.NONE),
                event("settlements:event/three", 120, 120, List.of(), Map.of(), List.of(), EventPreconditions.NONE)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry());
        HistoryEventResult result = engine.roll(profile(Map.of(), ScaleTier.HAMLET), report(Set.of(), Set.of()), ESTABLISHED, new Random(6L));

        assertEquals(2, eventIds(result).size());
        assertEquals(List.of("settlements:event/one", "settlements:event/two"), eventIds(result));
    }

    @Test
    void waterFeaturePreconditionUsesOrLogic() {
        EventPreconditions requiresAnyWater = EventPreconditions.builder()
                .requiredWaterFeatures(Set.of(WaterFeatureType.RIVER, WaterFeatureType.OCEAN))
                .build();
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/flood", 100, 100, List.of(), Map.of(), List.of(), requiresAnyWater)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry());
        HistoryEventResult result = engine.roll(profile(Map.of()), report(Set.of(WaterFeatureType.RIVER), Set.of()), ESTABLISHED, new Random(7L));

        assertEquals(List.of("settlements:event/flood"), eventIds(result));
    }

    @Test
    void visualMarkersAccumulateInTimeOrder() {
        HistoryEventRegistry registry = () -> List.of(
                event("settlements:event/late", 220, 220, List.of(), Map.of(), List.of("late_a", "late_b"), EventPreconditions.NONE),
                event("settlements:event/early", 120, 120, List.of(), Map.of(), List.of("early"), EventPreconditions.NONE)
        );

        HistoryEventEngine engine = new HistoryEventEngine(registry, traitRegistry());
        HistoryEventResult result = engine.roll(profile(Map.of()), report(Set.of(), Set.of()), ESTABLISHED, new Random(8L));

        assertEquals(List.of("settlements:event/early", "settlements:event/late"), eventIds(result));
        assertEquals(Set.of("early", "late_a", "late_b"), result.visualMarkers().markers());
    }

    @Test
    void roll_drawsOnlyEventsMarkedNew_forANewSettlement() {
        // Arrange
        HistoryEventEngine engine = new HistoryEventEngine(establishedAndNewEvents(), traitRegistry());

        // Act
        HistoryEventResult result = engine.roll(profile(Map.of()), report(Set.of(), Set.of()), NEW, new Random(13L));

        // Assert
        assertEquals(List.of("settlements:event/young_founding"), eventIds(result));
    }

    @Test
    void roll_neverDrawsEventsMarkedNew_forAnEstablishedSettlement() {
        // Arrange
        HistoryEventEngine engine = new HistoryEventEngine(establishedAndNewEvents(), traitRegistry());

        // Act
        HistoryEventResult result = engine.roll(profile(Map.of()), report(Set.of(), Set.of()), ESTABLISHED,
                new Random(14L));

        // Assert
        assertEquals(List.of("settlements:event/old_founding", "settlements:event/old_later"), eventIds(result));
    }

    @Test
    void date_datesFoundingEraEventsAtTheFounding() {
        // Arrange
        List<DrawnHistoryEvent> events = List.of(new DrawnHistoryEvent("settlements:event/founding", 130));

        // Act
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(events, RECORDED, ESTABLISHED, new Random(9L));

        // Assert — an established settlement predates its record, and its founding-era event happened as it was
        // founded
        assertTrue(history.founded().isBefore(RECORDED));
        assertEquals(List.of(new HistoryEvent("settlements:event/founding", history.founded())), history.events());
    }

    @Test
    void date_datesLaterEraEventsBetweenTheFoundingAndTheRecord_inEraOrder() {
        // Arrange — four later-era events, whose independently rolled dates follow the eras only if they are handed
        // out oldest first
        List<DrawnHistoryEvent> events = List.of(
                new DrawnHistoryEvent("settlements:event/founding", 110),
                new DrawnHistoryEvent("settlements:event/first", 200),
                new DrawnHistoryEvent("settlements:event/second", 220),
                new DrawnHistoryEvent("settlements:event/third", 240),
                new DrawnHistoryEvent("settlements:event/fourth", 260));

        // Act
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(events, RECORDED, ESTABLISHED, new Random(10L));

        // Assert
        assertEquals(events.stream().map(DrawnHistoryEvent::eventId).toList(),
                history.events().stream().map(HistoryEvent::eventId).toList());
        CivilDateTime previous = history.founded();
        for (HistoryEvent laterEvent : history.events().subList(1, history.events().size())) {
            assertTrue(history.founded().isBefore(laterEvent.date()) && laterEvent.date().isBefore(RECORDED),
                    laterEvent.eventId() + " must fall strictly between the founding and the record");
            assertFalse(laterEvent.date().isBefore(previous),
                    laterEvent.eventId() + " must not predate an earlier era");
            previous = laterEvent.date();
        }
    }

    @Test
    void date_stillFoundsTheSettlementBeforeItsRecord_whenNoEventWasDrawn() {
        // Act
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(List.of(), RECORDED, ESTABLISHED, new Random(11L));

        // Assert
        assertTrue(history.events().isEmpty());
        assertTrue(history.founded().isBefore(RECORDED));
    }

    @Test
    void date_givesTheSameDates_forTheSameRandomState() {
        // Arrange — a record rebuilt from its structure start must get the dates the first one got
        List<DrawnHistoryEvent> events = List.of(
                new DrawnHistoryEvent("settlements:event/founding", 110),
                new DrawnHistoryEvent("settlements:event/later", 230));

        // Act
        HistoryEventEngine.DatedHistory first = HistoryEventEngine.date(events, RECORDED, ESTABLISHED, new Random(12L));
        HistoryEventEngine.DatedHistory second = HistoryEventEngine.date(events, RECORDED, ESTABLISHED, new Random(12L));

        // Assert
        assertEquals(first, second);
    }

    @Test
    void date_foundsANewSettlementAsItsRecordBegins_andDatesEveryEventThen() {
        // Arrange — a later-era event too, which an established settlement would date before its record
        List<DrawnHistoryEvent> events = List.of(
                new DrawnHistoryEvent("settlements:event/founding", 100),
                new DrawnHistoryEvent("settlements:event/later", 230));

        // Act
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(events, RECORDED, NEW, new Random(15L));

        // Assert
        assertEquals(RECORDED, history.founded());
        assertEquals(List.of(
                new HistoryEvent("settlements:event/founding", RECORDED),
                new HistoryEvent("settlements:event/later", RECORDED)), history.events());
    }

    @Test
    void date_foundsAnEstablishedSettlementWithinTheFoundingAgeRangeBeforeItsRecord() {
        // Arrange
        CivilDateTime oldest = RECORDED.minus(GameTicks.days(
                HistoryEventEngine.MAX_FOUNDING_AGE_YEARS * WorldCalendar.DAYS_PER_YEAR));
        CivilDateTime youngest = RECORDED.minus(GameTicks.days(
                HistoryEventEngine.MIN_FOUNDING_AGE_YEARS * WorldCalendar.DAYS_PER_YEAR));

        // Seeds drawn from one generator: java.util.Random's first draw barely moves across small consecutive seeds
        for (long seed : new Random(16L).longs(50).toArray()) {
            // Act
            HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(List.of(), RECORDED, ESTABLISHED,
                    new Random(seed));

            // Assert
            assertFalse(history.founded().isBefore(oldest), "Founded too long ago for seed " + seed);
            assertFalse(youngest.isBefore(history.founded()), "Founded too recently for seed " + seed);
        }
    }

    private static HistoryEventRegistry establishedAndNewEvents() {
        // No exclusive tags, so only the founding precondition can keep an event out
        EventPreconditions newOnly = EventPreconditions.builder().founding(NEW).build();
        return () -> List.of(
                event("settlements:event/old_founding", 100, 100, List.of(), Map.of(), List.of(), EventPreconditions.NONE),
                event("settlements:event/young_founding", 100, 100, List.of(), Map.of(), List.of(), newOnly),
                event("settlements:event/old_later", 220, 220, List.of(), Map.of(), List.of(), EventPreconditions.NONE));
    }

    private static HistoryEventDefinition event(String id,
                                                int min,
                                                int max,
                                                List<String> exclusiveTags,
                                                Map<TraitId, Float> modifiers,
                                                List<String> visualMarkers,
                                                EventPreconditions preconditions) {
        return HistoryEventDefinition.builder()
                .id(id)
                .category("test")
                .timeHorizonMin(min)
                .timeHorizonMax(max)
                .exclusiveTags(exclusiveTags)
                .probabilityWeight(1.0f)
                .preconditions(preconditions)
                .traitModifiers(modifiers)
                .visualMarkers(visualMarkers)
                .narrativeText("test")
                .build();
    }

    private static List<String> eventIds(HistoryEventResult result) {
        return result.events().stream().map(DrawnHistoryEvent::eventId).toList();
    }

    private static SettlementProfile profile(Map<TraitId, Float> adjustedWeights) {
        return profile(adjustedWeights, ScaleTier.VILLAGE);
    }

    private static SettlementProfile profile(Map<TraitId, Float> adjustedWeights, ScaleTier scaleTier) {
        List<TraitId> traits = new ArrayList<>(adjustedWeights.keySet());
        TraitId primary = traits.isEmpty() ? DEFENSE : traits.getFirst();
        return SettlementProfile.builder()
                .primary(primary)
                .secondary(List.of())
                .flavor(List.of())
                .adjustedWeights(new LinkedHashMap<>(adjustedWeights))
                .scaleTier(scaleTier)
                .initialPopulation(40)
                .wealthLevel(0.5f)
                .defenseLevel(DefenseLevel.NONE)
                .seed(123L)
                .build();
    }

    private static SiteReport report(Set<WaterFeatureType> waterFeatures, Set<ResourceTag> resourceTags) {
        return new SiteReport(
                null,
                null,
                new ElevationStats(64, 64, 64, new BlockPosition(0, 64, 0)),
                Map.of(),
                Map.of(),
                waterFeatures,
                resourceTags
        );
    }

    @SafeVarargs
    private static TraitRegistry traitRegistry(TraitId... traitIds) {
        Set<TraitId> ids = Set.of(traitIds);
        return new TraitRegistry() {
            @Override
            public Set<TraitId> allTraitIds() {
                return ids;
            }

            @Override
            public Optional<TraitDefinition> byId(TraitId id) {
                return Optional.empty();
            }
        };
    }

}
