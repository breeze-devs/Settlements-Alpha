package dev.breezes.settlements.infrastructure.minecraft.persistence;

import dagger.Lazy;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.persistence.SettlementStoreListener;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.world.ChunkPosition;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Keeps each level's settlements in that level's {@link SettlementSavedData}, which every call resolves afresh.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SavedDataSettlementStore implements SettlementStore {

    // Lazy, because listeners read the store: a plain set would make the store depend on itself
    private final Lazy<Set<SettlementStoreListener>> listeners;

    @Override
    public Optional<Settlement> find(@Nonnull ServerLevel level, @Nonnull UUID settlementId) {
        return SettlementSavedData.get(level).find(settlementId);
    }

    @Override
    public Optional<Settlement> findClaiming(@Nonnull ServerLevel level, @Nonnull ChunkPosition chunk) {
        return SettlementSavedData.get(level).findClaiming(chunk);
    }

    @Override
    public Optional<Settlement> findListing(@Nonnull ServerLevel level, @Nonnull UUID memberId) {
        return SettlementSavedData.get(level).findListing(memberId);
    }

    @Override
    public Optional<Settlement> add(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        Optional<Settlement> added = SettlementSavedData.get(level).add(settlement);
        added.ifPresent(stored -> this.announce(level, null, stored));
        return added;
    }

    @Override
    public Optional<Settlement> update(@Nonnull ServerLevel level, @Nonnull UUID settlementId,
                                       @Nonnull UnaryOperator<Settlement> operation) {
        SettlementSavedData data = SettlementSavedData.get(level);
        Optional<Settlement> before = data.find(settlementId);
        Optional<Settlement> after = data.update(settlementId, operation);

        // The saved data hands back the very settlement it held when it wrote nothing
        if (before.isPresent() && after.isPresent() && after.get() != before.get()) {
            this.announce(level, before.get(), after.get());
        }

        return after;
    }

    @Override
    public Optional<Settlement> disband(@Nonnull ServerLevel level, @Nonnull UUID settlementId) {
        Optional<Settlement> disbanded = SettlementSavedData.get(level).disband(settlementId);
        disbanded.ifPresent(removed -> this.announce(level, removed, null));
        return disbanded;
    }

    @Override
    public boolean isTombstoned(@Nonnull ServerLevel level, @Nonnull UUID settlementId) {
        return SettlementSavedData.get(level).isTombstoned(settlementId);
    }

    @Override
    public List<Settlement> all(@Nonnull ServerLevel level) {
        return SettlementSavedData.get(level).all();
    }

    private void announce(ServerLevel level, @Nullable Settlement before, @Nullable Settlement after) {
        for (SettlementStoreListener listener : this.listeners.get()) {
            // The change is already stored, so a failing listener must not make the writer believe it was not
            try {
                listener.onSettlementChanged(level, before, after);
            } catch (RuntimeException exception) {
                log.error("Settlement store listener {} failed on a change to settlement {} in {}",
                        listener.getClass().getSimpleName(), (after != null ? after : before).id(),
                        level.dimension().location(), exception);
            }
        }
    }

}
