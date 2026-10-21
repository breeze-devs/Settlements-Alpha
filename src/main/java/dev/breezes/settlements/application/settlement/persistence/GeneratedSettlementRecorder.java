package dev.breezes.settlements.application.settlement.persistence;

import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.application.settlement.naming.SettlementNamer;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.generation.history.DrawnHistoryEvent;
import dev.breezes.settlements.domain.generation.history.HistoryEventEngine;
import dev.breezes.settlements.domain.generation.history.SettlementFoundingKind;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Creates the records of settlements generated with their structures.
 */
@ServerSide
@ServerScope
@CustomLog(topic = "settlement")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class GeneratedSettlementRecorder {

    private final SettlementStore settlementStore;
    private final SettlementNamer settlementNamer;
    private final SettlementLifecyclePublisher lifecyclePublisher;

    /**
     * Records a generated settlement in the level under a name generated from its profile's seed, unless the level
     * already stores one under its id or has tombstoned its id. Records nothing when no name can be generated. A stored
     * record is reported to the {@link SettlementLifecyclePublisher}.
     * <p>
     * Must be called on the server thread.
     */
    public void record(@Nonnull ServerLevel level, @Nonnull GeneratedSettlement generated) {
        if (this.settlementStore.isTombstoned(level, generated.id())
                || this.settlementStore.find(level, generated.id()).isPresent()) {
            return;
        }

        Optional<String> name = this.settlementNamer.generatedName(level, generated.profile().seed());
        if (name.isEmpty()) {
            // Nothing is stored, so the start's next load tries again, by which time a datapack reload may have filled
            // the pools
            log.error("Generated settlement {} in {} is not recorded: the settlement name pools are empty",
                    generated.id(), level.dimension().location());
            return;
        }

        CivilDateTime recorded = new CivilDateTime(level.getDayTime());
        // Seeded from the profile, so a record rebuilt from the same start gets the same dates relative to its start
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(generated.history(), recorded,
                SettlementFoundingKind.ESTABLISHED, new Random(generated.profile().seed()));
        Settlement settlement = Settlement.builder()
                .id(generated.id())
                .name(name.get())
                .origin(SettlementOrigin.GENERATED)
                .bounds(generated.bounds())
                .founding(new SettlementFounding(history.founded(), recorded))
                .profile(generated.profile())
                .history(history.events())
                .roster(SettlementRoster.EMPTY)
                .gatherings(SettlementGatherings.NONE)
                .log(SettlementLog.EMPTY)
                .build();
        Optional<Settlement> stored = this.settlementStore.add(level, settlement);
        if (stored.isEmpty()) {
            // The id and its tombstone were checked above, so another settlement claims the center chunk
            log.debug("Generated settlement {} in {} is not recorded: another settlement claims its center chunk",
                    settlement.id(), level.dimension().location());
            return;
        }
        log.debug("Recorded generated settlement {} in {}, claiming {} of its {} chunks", settlement.id(),
                level.dimension().location(), stored.get().bounds().chunks().size(),
                generated.bounds().chunks().size());
        this.lifecyclePublisher.recorded(level, stored.get());
    }

    /**
     * A settlement generated with its structure, as the structure carries it until its record exists.
     *
     * @param id      the id its structure pieces carry
     * @param bounds  the chunks its structure claims
     * @param profile the profile generation rolled for it
     * @param history the history events generation drew for it, earliest era first
     */
    @Builder
    public record GeneratedSettlement(
            UUID id,
            SettlementBounds bounds,
            SettlementProfile profile,
            List<DrawnHistoryEvent> history
    ) {
    }

}
