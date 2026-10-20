package dev.breezes.settlements.infrastructure.minecraft.attachments;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * What a client is told about the settlements of one dimension: whether settlements can be founded there, and the
 * summary of each settlement the dimension stores, by settlement id.
 * <p>
 * The server's copy is changed in place on the server thread, and remembers the settlements whose summary changed since
 * they were last broadcast. A client's copy is replaced by every sync it receives, so a client reader must look the
 * directory up again rather than keep one.
 */
public final class SettlementDirectory {

    private final boolean foundingAllowed;

    private final Map<UUID, SettlementSummary> summaries;

    /**
     * The settlements whose summary was put or removed since the last broadcast, in the order they changed.
     */
    private final Set<UUID> unsent = new LinkedHashSet<>();

    /**
     * Creates a directory holding the given summaries, with nothing left to broadcast.
     */
    public SettlementDirectory(boolean foundingAllowed, @Nonnull Map<UUID, SettlementSummary> summaries) {
        this.foundingAllowed = foundingAllowed;
        this.summaries = new LinkedHashMap<>(summaries);
    }

    /**
     * Returns whether settlements can be founded in this dimension.
     */
    public boolean foundingAllowed() {
        return this.foundingAllowed;
    }

    /**
     * Returns the summary of the settlement with the given id.
     */
    public Optional<SettlementSummary> find(@Nonnull UUID settlementId) {
        return Optional.ofNullable(this.summaries.get(settlementId));
    }

    /**
     * Returns every summary by settlement id, as an unmodifiable view of this copy.
     */
    public Map<UUID, SettlementSummary> summaries() {
        return Collections.unmodifiableMap(this.summaries);
    }

    /**
     * Sets the summary of the settlement with the given id, leaving it to broadcast unless it equals the one held.
     *
     * @return whether the summary changed
     */
    public boolean put(@Nonnull UUID settlementId, @Nonnull SettlementSummary summary) {
        if (summary.equals(this.summaries.put(settlementId, summary))) {
            return false;
        }
        this.unsent.add(settlementId);
        return true;
    }

    /**
     * Removes the summary of the settlement with the given id, leaving its removal to broadcast.
     *
     * @return whether a summary was held
     */
    public boolean remove(@Nonnull UUID settlementId) {
        if (this.summaries.remove(settlementId) == null) {
            return false;
        }
        this.unsent.add(settlementId);
        return true;
    }

    /**
     * Returns the settlements left to broadcast and forgets them.
     */
    List<UUID> takeUnsent() {
        List<UUID> taken = List.copyOf(this.unsent);
        this.unsent.clear();
        return taken;
    }

}
