package dev.breezes.settlements.domain.settlement.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A settlement's bounded event log. Older entries are dropped when the log is full.
 *
 * @param entries the logged events, oldest first; their dates need not follow this order, since setting the world's
 *                time back rewinds the clock they are dated by
 */
public record SettlementLog(List<SettlementLogEntry> entries) {

    /**
     * The most entries a log holds.
     */
    public static final int CAPACITY = 128;

    public static final SettlementLog EMPTY = new SettlementLog(List.of());

    public SettlementLog {
        int size = entries.size();
        entries = size <= CAPACITY ? List.copyOf(entries) : List.copyOf(entries.subList(size - CAPACITY, size));
    }

    /**
     * Returns this log with the given entry as its newest, dropping the oldest entry when this log is full.
     */
    public SettlementLog append(SettlementLogEntry entry) {
        List<SettlementLogEntry> next = new ArrayList<>(this.entries.size() + 1);
        next.addAll(this.entries);
        next.add(entry);
        return new SettlementLog(next);
    }

}
