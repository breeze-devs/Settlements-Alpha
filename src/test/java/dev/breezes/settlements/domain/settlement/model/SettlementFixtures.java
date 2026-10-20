package dev.breezes.settlements.domain.settlement.model;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.domain.world.CivilDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Builders for settlement records whose every field holds a non-default value, so a field a codec forgets or swaps
 * shows up as a difference.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementFixtures {

    public static final UUID FIRST_MEMBER = new UUID(0L, 101L);
    public static final UUID SECOND_MEMBER = new UUID(0L, 102L);

    private static final TraitId FARMING = TraitId.of("settlements:settlement_traits/farming");
    private static final TraitId FISHING = TraitId.of("settlements:settlement_traits/fishing");
    private static final TraitId DEFENSE = TraitId.of("settlements:settlement_traits/defense");

    /**
     * A fully populated settlement with the given id, its roster holding firstMember() and secondMember().
     */
    public static Settlement.SettlementBuilder settlement(UUID id) {
        return Settlement.builder()
                .id(id)
                .name("Ashford")
                .origin(SettlementOrigin.FOUNDED)
                .bounds(new SettlementBounds(new ChunkPosition(6, -13), Set.of(new ChunkPosition(6, -13),
                        new ChunkPosition(7, -13), new ChunkPosition(6, -12), new ChunkPosition(7, -12))))
                .founding(new SettlementFounding(new CivilDateTime(-480_000L), new CivilDateTime(24_000L)))
                .profile(profile().build())
                .history(List.of(
                        new HistoryEvent("settlements:settlement_events/founded_by_fishermen",
                                new CivilDateTime(-480_000L)),
                        new HistoryEvent("settlements:settlement_events/flood", new CivilDateTime(-120_000L))))
                .roster(new SettlementRoster(List.of(firstMember(), secondMember())))
                .gatherings(new SettlementGatherings(4, 17L))
                .fading(new SettlementFading(ClockTicks.of(1_200L)))
                .log(new SettlementLog(List.of(
                        logEntry(SettlementLogType.FOUNDED).build(),
                        logEntry(SettlementLogType.JOINED).date(new CivilDateTime(24_100L)).subjectId(FIRST_MEMBER)
                                .subjectName("Alder").build(),
                        logEntry(SettlementLogType.ZOMBIFIED).date(new CivilDateTime(30_000L)).subjectId(SECOND_MEMBER)
                                .subjectName("Birch").detail("during a raid").build())));
    }

    /**
     * An adult member without a home, so a codec that swaps the two flags is caught.
     */
    public static SettlementRosterEntry firstMember() {
        return SettlementRosterEntry.builder()
                .memberId(FIRST_MEMBER)
                .profession(VillagerProfessionKey.FARMER)
                .adult(true)
                .housed(false)
                .gatheringCountAtLastCheckIn(3)
                .build();
    }

    public static SettlementRosterEntry secondMember() {
        return SettlementRosterEntry.builder()
                .memberId(SECOND_MEMBER)
                .profession(VillagerProfessionKey.NONE)
                .adult(false)
                .housed(true)
                .gatheringCountAtLastCheckIn(2)
                .build();
    }

    /**
     * A roster entry for the given member, every other field filler.
     */
    public static SettlementRosterEntry.SettlementRosterEntryBuilder rosterEntry(UUID memberId) {
        return SettlementRosterEntry.builder()
                .memberId(memberId)
                .profession(VillagerProfessionKey.NONE)
                .adult(true)
                .housed(true)
                .gatheringCountAtLastCheckIn(0);
    }

    public static SettlementLogEntry.SettlementLogEntryBuilder logEntry(SettlementLogType type) {
        return SettlementLogEntry.builder()
                .date(new CivilDateTime(24_000L))
                .type(type);
    }

    public static SettlementProfile.SettlementProfileBuilder profile() {
        return SettlementProfile.builder()
                .primary(FARMING)
                .secondary(List.of(FISHING))
                .flavor(List.of(DEFENSE))
                .adjustedWeights(Map.of(FARMING, 0.9f, FISHING, 0.6f, DEFENSE, 0.3f))
                .scaleTier(ScaleTier.TOWN)
                .initialPopulation(42)
                .wealthLevel(0.55f)
                .defenseLevel(DefenseLevel.LOW)
                .seed(1_234L);
    }

}
