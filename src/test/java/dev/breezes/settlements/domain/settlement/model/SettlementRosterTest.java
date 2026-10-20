package dev.breezes.settlements.domain.settlement.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.FIRST_MEMBER;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.SECOND_MEMBER;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.firstMember;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.rosterEntry;
import static dev.breezes.settlements.domain.settlement.model.SettlementFixtures.secondMember;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementRosterTest {

    private static final UUID THIRD_MEMBER = new UUID(0L, 103L);

    @Test
    void rejectsTwoEntriesForOneMember() {
        // Arrange — a second row for the same villager, differing only in what it records
        SettlementRosterEntry original = firstMember();
        SettlementRosterEntry repeat = SettlementRosterEntry.builder()
                .memberId(FIRST_MEMBER)
                .profession(original.profession())
                .adult(original.adult())
                .housed(original.housed())
                .gatheringCountAtLastCheckIn(original.gatheringCountAtLastCheckIn() + 1)
                .build();
        List<SettlementRosterEntry> entries = List.of(original, repeat);

        // Act, Assert
        assertThrows(IllegalArgumentException.class, () -> new SettlementRoster(entries));
    }

    @Test
    void with_addsAnUnlistedMemberLast() {
        // Arrange
        SettlementRoster roster = new SettlementRoster(List.of(firstMember()));

        // Act
        SettlementRoster added = roster.with(secondMember());

        // Assert
        assertEquals(List.of(firstMember(), secondMember()), added.entries());
        assertEquals(Optional.of(secondMember()), added.find(SECOND_MEMBER));
    }

    @Test
    void with_replacesAListedMembersEntryWhereItStands() {
        // Arrange — the first of three members checks in again, so its entry changes
        SettlementRosterEntry third = rosterEntry(THIRD_MEMBER).build();
        SettlementRoster roster = new SettlementRoster(List.of(firstMember(), secondMember(), third));
        SettlementRosterEntry checkedIn = rosterEntry(FIRST_MEMBER)
                .profession(firstMember().profession())
                .gatheringCountAtLastCheckIn(firstMember().gatheringCountAtLastCheckIn() + 1)
                .build();

        // Act
        SettlementRoster replaced = roster.with(checkedIn);

        // Assert — the member keeps its place instead of moving to the end
        assertEquals(List.of(checkedIn, secondMember(), third), replaced.entries());
        assertEquals(Optional.of(checkedIn), replaced.find(FIRST_MEMBER));
    }

    @Test
    void with_returnsTheSameRoster_whenTheMembersEntryIsUnchanged() {
        // Arrange — an equal entry built anew, not the instance the roster holds
        SettlementRoster roster = new SettlementRoster(List.of(firstMember(), secondMember()));

        // Act
        SettlementRoster unchanged = roster.with(firstMember());

        // Assert
        assertSame(roster, unchanged);
    }

    @Test
    void without_removesTheMember_keepingTheOthersInOrder() {
        // Arrange
        SettlementRosterEntry third = rosterEntry(THIRD_MEMBER).build();
        SettlementRoster roster = new SettlementRoster(List.of(firstMember(), secondMember(), third));

        // Act
        SettlementRoster removed = roster.without(SECOND_MEMBER);

        // Assert
        assertEquals(List.of(firstMember(), third), removed.entries());
        assertEquals(Optional.empty(), removed.find(SECOND_MEMBER));
    }

    @Test
    void without_returnsTheSameRoster_whenTheMemberIsNotListed() {
        // Arrange
        SettlementRoster roster = new SettlementRoster(List.of(firstMember()));

        // Act
        SettlementRoster unchanged = roster.without(SECOND_MEMBER);

        // Assert
        assertSame(roster, unchanged);
    }

    @Test
    void isNotEqual_toARosterListingTheSameEntriesInAnotherOrder() {
        // Arrange
        SettlementRoster roster = new SettlementRoster(List.of(firstMember(), secondMember()));
        SettlementRoster reordered = new SettlementRoster(List.of(secondMember(), firstMember()));

        // Act, Assert
        assertNotEquals(roster, reordered);
    }

}
