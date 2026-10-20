package dev.breezes.settlements.domain.settlement.model;

import org.apache.commons.lang3.Validate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.UUID;

/**
 * A settlement's members, holding at most one entry per member UUID, in the order the members were added.
 * <p>
 * Immutable: each change returns another roster, or this same instance when it changes nothing.
 */
public final class SettlementRoster {

    public static final SettlementRoster EMPTY = new SettlementRoster(List.of());

    /**
     * Never changed after construction.
     */
    private final SequencedMap<UUID, SettlementRosterEntry> entriesByMember;

    /**
     * @param entries the members' rows, in the order they were added
     * @throws IllegalArgumentException when two entries list the same member
     */
    public SettlementRoster(List<SettlementRosterEntry> entries) {
        LinkedHashMap<UUID, SettlementRosterEntry> byMember = LinkedHashMap.newLinkedHashMap(entries.size());
        for (SettlementRosterEntry entry : entries) {
            Validate.isTrue(byMember.putIfAbsent(entry.memberId(), entry) == null,
                    "Roster lists member %s more than once", entry.memberId());
        }
        this.entriesByMember = byMember;
    }

    private SettlementRoster(SequencedMap<UUID, SettlementRosterEntry> entriesByMember) {
        this.entriesByMember = entriesByMember;
    }

    /**
     * Returns the given member's entry.
     */
    public Optional<SettlementRosterEntry> find(UUID memberId) {
        return Optional.ofNullable(this.entriesByMember.get(memberId));
    }

    /**
     * Returns this roster with the given entry for its member: added last when the member is not listed, and
     * otherwise replacing the member's entry where it stands. Returns this same instance when the member's entry
     * already equals the given one.
     */
    public SettlementRoster with(SettlementRosterEntry entry) {
        if (entry.equals(this.entriesByMember.get(entry.memberId()))) {
            return this;
        }
        LinkedHashMap<UUID, SettlementRosterEntry> next = new LinkedHashMap<>(this.entriesByMember);
        // Putting a key the map holds keeps its position
        next.put(entry.memberId(), entry);
        return new SettlementRoster(next);
    }

    /**
     * Returns this roster without the given member, or this same instance when the member is not listed.
     */
    public SettlementRoster without(UUID memberId) {
        if (!this.entriesByMember.containsKey(memberId)) {
            return this;
        }
        LinkedHashMap<UUID, SettlementRosterEntry> next = new LinkedHashMap<>(this.entriesByMember);
        next.remove(memberId);
        return new SettlementRoster(next);
    }

    /**
     * Returns the members' rows, in the order they were added. Later changes to the roster do not affect the returned
     * list.
     */
    public List<SettlementRosterEntry> entries() {
        return List.copyOf(this.entriesByMember.values());
    }

    /**
     * Two rosters are equal when they hold equal entries in the same order.
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof SettlementRoster roster && this.entries().equals(roster.entries());
    }

    @Override
    public int hashCode() {
        return this.entries().hashCode();
    }

    @Override
    public String toString() {
        return "SettlementRoster" + this.entries();
    }

}
