package dev.breezes.settlements.application.settlement.naming;

import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.settlement.naming.SettlementNameGenerator;
import dev.breezes.settlements.domain.settlement.naming.SettlementNamePoolRegistry;
import dev.breezes.settlements.domain.settlement.naming.SettlementRename;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Names settlements so that no two stored in one dimension share a name, and is the only writer of a stored
 * settlement's name.
 * <p>
 * Must be called on the server thread. A name is free only against the settlements stored when it is picked, so a
 * caller stores the named settlement before naming another in the same level.
 */
@ServerSide
@ServerScope
@CustomLog(topic = "settlement")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementNamer {

    private final SettlementStore settlementStore;
    private final SettlementNamePoolRegistry namePools;
    private final SettlementLifecyclePublisher lifecyclePublisher;

    /**
     * Returns a name generated from the datapack word pools that no settlement stored in the level uses, or empty when
     * either pool is empty. The same seed gives the same name while the pools and the level's names are unchanged.
     */
    public Optional<String> generatedName(@Nonnull ServerLevel level, long seed) {
        return SettlementNameGenerator.generate(this.namePools.pools(), seed, this.takenNames(level));
    }

    /**
     * Returns whether generatedName can yield a name now, which it cannot while either datapack word pool is empty.
     */
    public boolean canGenerateNames() {
        return SettlementNameGenerator.canGenerate(this.namePools.pools());
    }

    /**
     * Returns the preferred name made unique among the settlements stored in the level by
     * {@link SettlementNameGenerator#numbered}.
     *
     * @throws IllegalArgumentException if preferred is blank
     */
    public String preferredName(@Nonnull ServerLevel level, @Nonnull String preferred) {
        return SettlementNameGenerator.numbered(preferred, this.takenNames(level));
    }

    /**
     * Renames the level's settlement with the given id, resolving name conflicts within that level.
     * A changed name is stored with a {@link SettlementLogType#RENAMED} entry recording the previous name, and reported
     * with the renaming player to the {@link SettlementLifecyclePublisher}. An unchanged name writes nothing.
     *
     * @param renamer the player renaming the settlement, or null when no player is
     * @return the rename outcome, or empty if the settlement does not exist in the level
     * @throws IllegalArgumentException if the settlement exists and requested is blank
     */
    public Optional<SettlementRename> rename(@Nonnull ServerLevel level, @Nonnull UUID settlementId,
                                             @Nonnull String requested, @Nullable ServerPlayer renamer) {
        Optional<Settlement> stored = this.settlementStore.find(level, settlementId);
        if (stored.isEmpty()) {
            return Optional.empty();
        }

        SettlementRename rename = SettlementRename.of(stored.get().name(), requested, this.takenNames(level));
        if (rename instanceof SettlementRename.Renamed(String previousName, String name)) {
            SettlementLogEntry entry = SettlementLogEntry.builder()
                    .date(new CivilDateTime(level.getDayTime()))
                    .type(SettlementLogType.RENAMED)
                    .detail(previousName)
                    .build();
            Optional<Settlement> renamed = this.settlementStore.update(level, settlementId,
                    current -> current.toBuilder()
                            .name(name)
                            .log(current.log().append(entry))
                            .build());
            log.info("Renamed settlement {} ({}) in {} to {}", previousName, settlementId,
                    level.dimension().location(), name);
            renamed.ifPresent(settlement -> this.lifecyclePublisher.renamed(level, settlement, previousName, renamer));
        }

        return Optional.of(rename);
    }

    private List<String> takenNames(ServerLevel level) {
        return this.settlementStore.all(level).stream()
                .map(Settlement::name)
                .toList();
    }

}
