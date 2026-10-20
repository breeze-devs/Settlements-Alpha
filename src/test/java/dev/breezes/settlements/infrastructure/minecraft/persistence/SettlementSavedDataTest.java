package dev.breezes.settlements.infrastructure.minecraft.persistence;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.settlement.model.SettlementRosterEntry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.domain.world.CivilDateTime;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.FIRST_MEMBER;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.SECOND_MEMBER;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.firstMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.rosterEntry;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.secondMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.settlement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementSavedDataTest {

    private static final UUID ASHFORD = new UUID(0L, 1L);
    private static final UUID MILLBROOK = new UUID(0L, 2L);

    /**
     * The generated settlement the version one store holds a tombstone for.
     */
    private static final UUID DISBANDED = new UUID(0L, 3L);

    /**
     * A claim far from the fixture settlement's, so a second settlement can be stored beside it.
     */
    private static final SettlementBounds CLAIM_APART = claim(new ChunkPosition(40, 40));

    /**
     * A villager the fixture settlement's roster does not list.
     */
    private static final UUID OTHER_MEMBER = new UUID(0L, 201L);

    /**
     * A roster sharing no member with the fixture settlement's, so a second settlement can be stored beside it.
     */
    private static final SettlementRoster ROSTER_APART =
            new SettlementRoster(List.of(rosterEntry(OTHER_MEMBER).build()));

    /**
     * The store reads no registry, so a provider holding none stands in for the level's.
     */
    private static final HolderLookup.Provider REGISTRIES = HolderLookup.Provider.create(Stream.empty());

    /**
     * A settlement and a tombstone as format version 1 stores them. Changing how this decodes, or what the store
     * writes for the same content, breaks every world saved with it.
     */
    private static final String VERSION_ONE_STORE = """
            {
              format_version: 1,
              settlements: [{
                id: [I; 0, 0, 0, 1],
                name: "Ashford",
                origin: "GENERATED",
                bounds: {center_chunk_x: 6, center_chunk_z: -13, chunks: [L; -55834574842L, -55834574841L]},
                founding: {founded: -2400000L, recorded: 24000L},
                profile: {
                  primary: "settlements:settlement_traits/farming",
                  secondary: ["settlements:settlement_traits/fishing"],
                  flavor: ["settlements:settlement_traits/defense"],
                  adjusted_weights: {"settlements:settlement_traits/farming": 0.75f},
                  scale_tier: "TOWN",
                  initial_population: 42,
                  wealth: 0.5f,
                  defense: "LOW",
                  seed: 1234L
                },
                history: [{event_id: "settlements:settlement_events/flood", day_time: -1200000L}],
                roster: [{member_id: [I; 0, 0, 0, 101], profession: "minecraft:farmer", adult: 1b, housed: 0b, gathering_mark: 3}],
                gatherings: {count: 4, last_day: 17L},
                fading_ticks_elapsed: 1200L,
                log: [{day_time: 24100L, type: "JOINED", subject_id: [I; 0, 0, 0, 101], subject_name: "Alder", detail: "by the well"}]
              }],
              tombstones: [{id: [I; 0, 0, 0, 3]}]
            }""";

    @Test
    void add_makesTheSettlementFindableByIdAndMarksTheStoreDirty() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).build();

        // Act
        Optional<Settlement> added = store.add(ashford);

        // Assert
        assertEquals(Optional.of(ashford), added);
        assertEquals(Optional.of(ashford), store.find(ASHFORD));
        assertTrue(store.isDirty());
    }

    @Test
    void add_keepsTheStoredSettlement_whenItsIdIsTaken() {
        // Arrange — the second settlement claims chunks nobody claims, so only its id can refuse it
        SettlementSavedData store = new SettlementSavedData();
        Settlement stored = settlement(ASHFORD).name("Ashford").build();
        store.add(stored);
        store.setDirty(false);

        // Act
        Optional<Settlement> added = store.add(settlement(ASHFORD).name("Millbrook").bounds(CLAIM_APART).build());

        // Assert
        assertEquals(Optional.empty(), added);
        assertEquals(Optional.of(stored), store.find(ASHFORD));
        assertFalse(store.isDirty());
    }

    @Test
    void add_clipsTheClaim_toTheChunksNoStoredSettlementClaims() {
        // Arrange — Millbrook's claim runs west into the chunk Ashford claims
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition shared = new ChunkPosition(1, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), shared)).build();
        store.add(ashford);
        Settlement.SettlementBuilder millbrook = settlement(MILLBROOK)
                .bounds(claim(new ChunkPosition(2, 0), shared, new ChunkPosition(3, 0)))
                .roster(ROSTER_APART);

        // Act
        Optional<Settlement> added = store.add(millbrook.build());

        // Assert — Millbrook is stored without the shared chunk, which stays Ashford's
        Settlement clipped = millbrook.bounds(claim(new ChunkPosition(2, 0), new ChunkPosition(3, 0))).build();
        assertEquals(Optional.of(clipped), added);
        assertEquals(Optional.of(clipped), store.find(MILLBROOK));
        assertEquals(Optional.of(ashford), store.findClaiming(shared));
    }

    @Test
    void add_storesTheSettlementAsGiven_whenItsClaimMeetsNoOther() {
        // Arrange — Millbrook's claim borders Ashford's without sharing a chunk
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), new ChunkPosition(1, 0))).build());
        Settlement millbrook = settlement(MILLBROOK)
                .bounds(claim(new ChunkPosition(2, 0), new ChunkPosition(3, 0)))
                .roster(ROSTER_APART)
                .build();

        // Act
        Optional<Settlement> added = store.add(millbrook);

        // Assert
        assertTrue(added.isPresent());
        assertSame(millbrook, added.get());
    }

    @Test
    void add_refusesASettlement_whoseCenterChunkIsClaimed() {
        // Arrange — Millbrook is centered on a chunk Ashford claims
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition shared = new ChunkPosition(1, 0);
        store.add(settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), shared)).build());
        store.setDirty(false);
        ChunkPosition unclaimed = new ChunkPosition(2, 0);

        // Act
        Optional<Settlement> added = store.add(settlement(MILLBROOK)
                .bounds(claim(shared, unclaimed))
                .roster(ROSTER_APART)
                .build());

        // Assert
        assertEquals(Optional.empty(), added);
        assertEquals(Optional.empty(), store.find(MILLBROOK));
        assertEquals(Optional.empty(), store.findClaiming(unclaimed));
        assertFalse(store.isDirty());
    }

    @Test
    void findClaiming_findsTheAddedSettlement_byAnyOfItsChunks() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition east = new ChunkPosition(1, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), east)).build();

        // Act
        store.add(ashford);

        // Assert
        assertEquals(Optional.of(ashford), store.findClaiming(east));
        assertEquals(Optional.empty(), store.findClaiming(new ChunkPosition(2, 0)));
    }

    @Test
    void findClaiming_findsNothing_onceTheSettlementDisbands() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition center = new ChunkPosition(0, 0);
        store.add(settlement(ASHFORD).bounds(claim(center)).build());

        // Act
        store.disband(ASHFORD);

        // Assert
        assertEquals(Optional.empty(), store.findClaiming(center));
    }

    @Test
    void add_refusesASettlement_listingAMemberAnotherSettlementLists() {
        // Arrange — Millbrook claims chunks apart from Ashford's but lists one of Ashford's members
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember()))).build();
        store.add(ashford);
        store.setDirty(false);
        Settlement millbrook = settlement(MILLBROOK)
                .bounds(CLAIM_APART)
                .roster(new SettlementRoster(List.of(firstMember())))
                .build();

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> store.add(millbrook));
        assertEquals(List.of(ashford), store.all());
        assertEquals(Optional.of(ashford), store.findListing(FIRST_MEMBER));
        assertFalse(store.isDirty());
    }

    @Test
    void add_acceptsAMember_whoseSettlementDisbanded() {
        // Arrange — Ashford listed the member and is gone
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember()))).build());
        store.disband(ASHFORD);
        Settlement millbrook = settlement(MILLBROOK)
                .bounds(CLAIM_APART)
                .roster(new SettlementRoster(List.of(firstMember())))
                .build();

        // Act
        Optional<Settlement> added = store.add(millbrook);

        // Assert
        assertEquals(Optional.of(millbrook), added);
        assertEquals(Optional.of(millbrook), store.findListing(FIRST_MEMBER));
    }

    @Test
    void update_replacesTheSettlement_keepingItsPlaceInTheStoreOrder() {
        // Arrange — Ashford, stored first, is renamed
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).build());
        Settlement millbrook = settlement(MILLBROOK).bounds(CLAIM_APART).roster(ROSTER_APART).build();
        store.add(millbrook);
        store.setDirty(false);

        // Act
        Optional<Settlement> updated = store.update(ASHFORD, ashford -> ashford.toBuilder().name("Ashby").build());

        // Assert
        Settlement renamed = settlement(ASHFORD).name("Ashby").build();
        assertEquals(Optional.of(renamed), updated);
        assertEquals(List.of(renamed, millbrook), store.all());
        assertTrue(store.isDirty());
    }

    @Test
    void update_writesNothing_whenTheOperationReturnsTheSettlementItWasGiven() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).build();
        store.add(ashford);
        store.setDirty(false);

        // Act
        Optional<Settlement> updated = store.update(ASHFORD, settlement -> settlement);

        // Assert
        assertTrue(updated.isPresent());
        assertSame(ashford, updated.get());
        assertFalse(store.isDirty());
    }

    @Test
    void update_findsNothing_whenNoSettlementHasTheId() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).build());
        store.setDirty(false);

        // Act
        Optional<Settlement> updated = store.update(MILLBROOK,
                settlement -> settlement.toBuilder().name("Millbrook").build());

        // Assert
        assertEquals(Optional.empty(), updated);
        assertEquals(Optional.empty(), store.find(MILLBROOK));
        assertFalse(store.isDirty());
    }

    @Test
    void update_refusesAResultWithAnotherId() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).build();
        store.add(ashford);
        store.setDirty(false);

        // Act, Assert
        assertThrows(IllegalArgumentException.class,
                () -> store.update(ASHFORD, settlement -> settlement.toBuilder().id(MILLBROOK).build()));
        assertEquals(List.of(ashford), store.all());
        assertFalse(store.isDirty());
    }

    @Test
    void update_refusesAResultWithAnotherClaim() {
        // Arrange — Ashford's update would take the chunk Millbrook claims beside it
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition center = new ChunkPosition(0, 0);
        ChunkPosition millbrooksChunk = new ChunkPosition(1, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(center)).build();
        Settlement millbrook = settlement(MILLBROOK).bounds(claim(millbrooksChunk)).roster(ROSTER_APART).build();
        store.add(ashford);
        store.add(millbrook);
        store.setDirty(false);

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> store.update(ASHFORD,
                settlement -> settlement.toBuilder().bounds(claim(center, millbrooksChunk)).build()));
        assertEquals(List.of(ashford, millbrook), store.all());
        assertEquals(Optional.of(millbrook), store.findClaiming(millbrooksChunk));
        assertFalse(store.isDirty());
    }

    @Test
    void update_refusesAResult_listingAMemberAnotherSettlementLists() {
        // Arrange — Millbrook's update would list a member Ashford lists
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember()))).build();
        Settlement millbrook = settlement(MILLBROOK).bounds(CLAIM_APART).roster(ROSTER_APART).build();
        store.add(ashford);
        store.add(millbrook);
        store.setDirty(false);

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> store.update(MILLBROOK,
                settlement -> settlement.toBuilder().roster(settlement.roster().with(firstMember())).build()));
        assertEquals(List.of(ashford, millbrook), store.all());
        assertEquals(Optional.of(ashford), store.findListing(FIRST_MEMBER));
        assertFalse(store.isDirty());
    }

    @Test
    void findListing_findsTheAddedSettlement_byAnyOfItsMembers() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD)
                .roster(new SettlementRoster(List.of(firstMember(), secondMember())))
                .build();

        // Act
        store.add(ashford);

        // Assert
        assertEquals(Optional.of(ashford), store.findListing(SECOND_MEMBER));
        assertEquals(Optional.empty(), store.findListing(OTHER_MEMBER));
    }

    @Test
    void findListing_followsTheRoster_whenAnUpdateChangesIt() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember(), secondMember()))).build());

        // Act — the first member leaves and another joins, while the second stays listed
        Optional<Settlement> updated = store.update(ASHFORD, settlement -> settlement.toBuilder()
                .roster(settlement.roster().without(FIRST_MEMBER).with(rosterEntry(OTHER_MEMBER).build()))
                .build());

        // Assert
        assertTrue(updated.isPresent());
        assertEquals(Optional.empty(), store.findListing(FIRST_MEMBER));
        assertEquals(updated, store.findListing(SECOND_MEMBER));
        assertEquals(updated, store.findListing(OTHER_MEMBER));
    }

    @Test
    void load_rebuildsTheMemberIndex() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember()))).build();
        store.add(ashford);

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(store.save(new CompoundTag(), REGISTRIES), REGISTRIES);

        // Assert
        assertEquals(Optional.of(ashford), loaded.findListing(FIRST_MEMBER));
    }

    @Test
    void load_dropsAMemberFromALaterStoredSettlement_whomAnEarlierOneLists() {
        // Arrange — stored rosters sharing a member, which no add or update produces
        Settlement ashford = settlement(ASHFORD).roster(new SettlementRoster(List.of(firstMember()))).build();
        SettlementRosterEntry otherMember = rosterEntry(OTHER_MEMBER).build();
        Settlement.SettlementBuilder millbrook = settlement(MILLBROOK)
                .bounds(CLAIM_APART)
                .roster(new SettlementRoster(List.of(firstMember(), otherMember)));
        CompoundTag stored = storeOf(ashford, millbrook.build());

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert — Millbrook keeps its other member, the shared one stays Ashford's, and the repair is to be saved
        Settlement repaired = millbrook.roster(new SettlementRoster(List.of(otherMember))).build();
        assertEquals(List.of(ashford, repaired), loaded.all());
        assertEquals(Optional.of(ashford), loaded.findListing(FIRST_MEMBER));
        assertTrue(loaded.isDirty());
    }

    @Test
    void load_rebuildsTheChunkIndex() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        ChunkPosition east = new ChunkPosition(1, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), east)).build();
        store.add(ashford);

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(store.save(new CompoundTag(), REGISTRIES), REGISTRIES);

        // Assert
        assertEquals(Optional.of(ashford), loaded.findClaiming(east));
    }

    @Test
    void load_clipsALaterStoredClaim_toTheChunksEarlierClaimsLeave() {
        // Arrange — stored claims that overlap, which no add produces: Millbrook's runs west into Ashford's
        ChunkPosition shared = new ChunkPosition(1, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), shared)).build();
        Settlement.SettlementBuilder millbrook = settlement(MILLBROOK)
                .bounds(claim(new ChunkPosition(2, 0), shared, new ChunkPosition(3, 0)))
                .roster(ROSTER_APART);
        CompoundTag stored = storeOf(ashford, millbrook.build());

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert — Millbrook loads without the shared chunk, which stays Ashford's, and the repair is to be saved
        Settlement clipped = millbrook.bounds(claim(new ChunkPosition(2, 0), new ChunkPosition(3, 0))).build();
        assertEquals(List.of(ashford, clipped), loaded.all());
        assertEquals(Optional.of(ashford), loaded.findClaiming(shared));
        assertTrue(loaded.isDirty());
    }

    @Test
    void load_dropsALaterStoredSettlement_whoseCenterChunkAnEarlierOneClaims() {
        // Arrange — Millbrook is stored centered on a chunk Ashford claims
        ChunkPosition shared = new ChunkPosition(1, 0);
        ChunkPosition unclaimed = new ChunkPosition(2, 0);
        Settlement ashford = settlement(ASHFORD).bounds(claim(new ChunkPosition(0, 0), shared)).build();
        CompoundTag stored = storeOf(ashford, settlement(MILLBROOK)
                .bounds(claim(shared, unclaimed))
                .roster(ROSTER_APART)
                .build());

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert
        assertEquals(List.of(ashford), loaded.all());
        assertEquals(Optional.empty(), loaded.findClaiming(unclaimed));
        assertTrue(loaded.isDirty());
    }

    @Test
    void load_leavesTheStoreClean_whenNoStoredSettlementOrTombstoneNeedsRepair() {
        // Arrange
        CompoundTag stored = storeOf(List.of(settlement(ASHFORD).build(),
                settlement(MILLBROOK).bounds(CLAIM_APART).roster(ROSTER_APART).build()), List.of(DISBANDED));

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert
        assertFalse(loaded.isDirty());
    }

    @Test
    void disband_forgetsTheSettlementAndMarksTheStoreDirty() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).build();
        Settlement millbrook = settlement(MILLBROOK).bounds(CLAIM_APART).roster(ROSTER_APART).build();
        store.add(ashford);
        store.add(millbrook);
        store.setDirty(false);

        // Act
        Optional<Settlement> disbanded = store.disband(ASHFORD);

        // Assert
        assertEquals(Optional.of(ashford), disbanded);
        assertEquals(Optional.empty(), store.find(ASHFORD));
        assertEquals(List.of(millbrook), store.all());
        assertTrue(store.isDirty());
    }

    @Test
    void disband_tombstonesAGeneratedSettlement() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).origin(SettlementOrigin.GENERATED).build());

        // Act
        store.disband(ASHFORD);

        // Assert
        assertTrue(store.isTombstoned(ASHFORD));
    }

    @Test
    void disband_leavesNoTombstone_forAFoundedSettlement() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).origin(SettlementOrigin.FOUNDED).build();
        store.add(ashford);

        // Act
        store.disband(ASHFORD);

        // Assert — nothing else carries a founded settlement, so nothing refuses its id
        assertFalse(store.isTombstoned(ASHFORD));
        assertEquals(Optional.of(ashford), store.add(ashford));
    }

    @Test
    void disband_findsNothing_whenNoSettlementHasTheId() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).build());
        store.setDirty(false);

        // Act
        Optional<Settlement> disbanded = store.disband(MILLBROOK);

        // Assert
        assertEquals(Optional.empty(), disbanded);
        assertFalse(store.isTombstoned(MILLBROOK));
        assertFalse(store.isDirty());
    }

    @Test
    void add_refusesATombstonedId() {
        // Arrange — Ashford was generated and disbanded; its structure start offers it again, claiming free chunks
        SettlementSavedData store = new SettlementSavedData();
        Settlement.SettlementBuilder ashford = settlement(ASHFORD).origin(SettlementOrigin.GENERATED);
        store.add(ashford.build());
        store.disband(ASHFORD);
        store.setDirty(false);

        // Act
        Optional<Settlement> added = store.add(ashford.bounds(CLAIM_APART).build());

        // Assert
        assertEquals(Optional.empty(), added);
        assertEquals(Optional.empty(), store.find(ASHFORD));
        assertEquals(List.of(), store.all());
        assertFalse(store.isDirty());
    }

    @Test
    void saveThenLoad_keepsTheTombstone_whichStillRefusesItsId() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        Settlement ashford = settlement(ASHFORD).origin(SettlementOrigin.GENERATED).build();
        store.add(ashford);
        store.disband(ASHFORD);

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(store.save(new CompoundTag(), REGISTRIES), REGISTRIES);

        // Assert
        assertTrue(loaded.isTombstoned(ASHFORD));
        assertEquals(Optional.empty(), loaded.add(ashford));
    }

    @Test
    void load_keepsAStoredSettlementWhoseIdIsTombstoned_droppingTheTombstone() {
        // Arrange — a settlement stored beside its own tombstone, which no disband produces
        Settlement ashford = settlement(ASHFORD).origin(SettlementOrigin.GENERATED).build();
        CompoundTag stored = storeOf(List.of(ashford), List.of(ASHFORD));

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert — the settlement stays, and the repair is to be saved
        assertEquals(List.of(ashford), loaded.all());
        assertFalse(loaded.isTombstoned(ASHFORD));
        assertTrue(loaded.isDirty());
    }

    @Test
    void saveThenLoad_restoresEveryStoredSettlement() {
        // Arrange
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD).build());
        store.add(settlement(MILLBROOK).name("Millbrook").bounds(CLAIM_APART).roster(ROSTER_APART).build());

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(store.save(new CompoundTag(), REGISTRIES), REGISTRIES);

        // Assert
        assertEquals(store.all(), loaded.all());
    }

    @Test
    void saveThenLoad_restoresAFreshlyFoundedSettlement() {
        // Arrange — the shape a settlement is stored in when founded: nobody on its roster, nothing logged, no
        // gathering yet, and not fading
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(ASHFORD)
                .roster(SettlementRoster.EMPTY)
                .log(SettlementLog.EMPTY)
                .gatherings(SettlementGatherings.NONE)
                .fading(null)
                .build());

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(store.save(new CompoundTag(), REGISTRIES), REGISTRIES);

        // Assert
        assertEquals(store.all(), loaded.all());
    }

    @Test
    void load_readsTheVersionOneStoredForm() throws CommandSyntaxException {
        // Arrange
        CompoundTag stored = TagParser.parseTag(VERSION_ONE_STORE);

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert
        assertEquals(List.of(versionOneSettlement()), loaded.all());
        assertTrue(loaded.isTombstoned(DISBANDED));
    }

    @Test
    void save_writesTheVersionOneStoredForm() throws CommandSyntaxException {
        // Arrange — the tombstone comes from disbanding a generated settlement first
        SettlementSavedData store = new SettlementSavedData();
        store.add(settlement(DISBANDED).origin(SettlementOrigin.GENERATED).bounds(CLAIM_APART).build());
        store.disband(DISBANDED);
        store.add(versionOneSettlement());

        // Act
        CompoundTag saved = store.save(new CompoundTag(), REGISTRIES);

        // Assert
        assertEquals(TagParser.parseTag(VERSION_ONE_STORE), saved);
    }

    @Test
    void load_readsAStoreWithoutAFormatVersionAsEmpty() throws CommandSyntaxException {
        // Arrange
        CompoundTag stored = TagParser.parseTag(VERSION_ONE_STORE);
        stored.remove("format_version");

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(stored, REGISTRIES);

        // Assert
        assertEquals(List.of(), loaded.all());
    }

    @Test
    void load_keepsTheFirstOfTwoSettlementsWithOneId() {
        // Arrange — the second settlement claims chunks nobody claims, so only its id can drop it
        Settlement first = settlement(ASHFORD).name("Ashford").build();
        Settlement second = settlement(ASHFORD).name("Millbrook").bounds(CLAIM_APART).build();

        // Act
        SettlementSavedData loaded = SettlementSavedData.load(storeOf(first, second), REGISTRIES);

        // Assert
        assertEquals(List.of(first), loaded.all());
    }

    /**
     * The stored form of the given settlements, in order, with no tombstones, written without going through the
     * store's add.
     */
    private static CompoundTag storeOf(Settlement... settlements) {
        return storeOf(List.of(settlements), List.of());
    }

    /**
     * The stored form of the given settlements and tombstones, in order, written without going through the store.
     */
    private static CompoundTag storeOf(List<Settlement> settlements, List<UUID> tombstones) {
        return (CompoundTag) SettlementSavedDataCodec.STATE_CODEC.encodeStart(NbtOps.INSTANCE,
                        new SettlementSavedDataCodec.StoredState(settlements, tombstones))
                .getOrThrow(message -> new AssertionError("Expected the settlement store to encode: " + message));
    }

    /**
     * A claim of the center chunk and the given chunks, which must be joined to it by edges.
     */
    private static SettlementBounds claim(ChunkPosition center, ChunkPosition... others) {
        Set<ChunkPosition> chunks = new HashSet<>(Arrays.asList(others));
        chunks.add(center);
        return new SettlementBounds(center, chunks);
    }

    private static Settlement versionOneSettlement() {
        TraitId farming = TraitId.of("settlements:settlement_traits/farming");
        UUID member = new UUID(0L, 101L);
        return Settlement.builder()
                .id(ASHFORD)
                .name("Ashford")
                .origin(SettlementOrigin.GENERATED)
                .bounds(new SettlementBounds(new ChunkPosition(6, -13),
                        Set.of(new ChunkPosition(6, -13), new ChunkPosition(7, -13))))
                .founding(new SettlementFounding(new CivilDateTime(-2_400_000L), new CivilDateTime(24_000L)))
                .profile(SettlementProfile.builder()
                        .primary(farming)
                        .secondary(List.of(TraitId.of("settlements:settlement_traits/fishing")))
                        .flavor(List.of(TraitId.of("settlements:settlement_traits/defense")))
                        .adjustedWeights(Map.of(farming, 0.75f))
                        .scaleTier(ScaleTier.TOWN)
                        .initialPopulation(42)
                        .wealthLevel(0.5f)
                        .defenseLevel(DefenseLevel.LOW)
                        .seed(1_234L)
                        .build())
                .history(List.of(
                        new HistoryEvent("settlements:settlement_events/flood", new CivilDateTime(-1_200_000L))))
                .roster(new SettlementRoster(List.of(SettlementRosterEntry.builder()
                        .memberId(member)
                        .profession(VillagerProfessionKey.FARMER)
                        .adult(true)
                        .housed(false)
                        .gatheringCountAtLastCheckIn(3)
                        .build())))
                .gatherings(new SettlementGatherings(4, 17L))
                .fading(new SettlementFading(ClockTicks.of(1_200L)))
                .log(new SettlementLog(List.of(SettlementLogEntry.builder()
                        .date(new CivilDateTime(24_100L))
                        .type(SettlementLogType.JOINED)
                        .subjectId(member)
                        .subjectName("Alder")
                        .detail("by the well")
                        .build())))
                .build();
    }

}
