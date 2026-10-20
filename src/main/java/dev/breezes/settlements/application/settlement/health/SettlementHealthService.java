package dev.breezes.settlements.application.settlement.health;

import dev.breezes.settlements.application.settlement.bell.SettlementBellRules;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingConfig;
import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth.BellPresence;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth.Cause;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assesses settlement health and manages fading, revival, and disbanding.
 * <p>
 * Must be called on the server thread.
 */
@ServerSide
@ServerScope
@CustomLog
public final class SettlementHealthService {

    /**
     * Duration of a health sweep and the fading increment for an already-fading settlement.
     */
    private static final ClockTicks HEALTH_INTERVAL = ClockTicks.minutes(1);

    private final SettlementStore settlementStore;
    private final SettlementLifecyclePublisher lifecyclePublisher;
    private final SettlementFoundingConfig foundingConfig;

    @Nullable
    private final ClockTicks fadingDuration;

    private final Map<ServerLevel, Sweep> sweepsByLevel = new IdentityHashMap<>();

    @Inject
    SettlementHealthService(SettlementStore settlementStore,
                            SettlementLifecyclePublisher lifecyclePublisher,
                            SettlementFoundingConfig foundingConfig,
                            SettlementDecayConfig decayConfig) {
        this.settlementStore = settlementStore;
        this.lifecyclePublisher = lifecyclePublisher;
        this.foundingConfig = foundingConfig;
        this.fadingDuration = decayConfig.fadingDays() == 0
                ? null
                : GameTicks.days(decayConfig.fadingDays()).asClockTicks();
    }

    /**
     * Assesses health from the settlement's roster and loaded claimed chunks without loading additional chunks.
     * Reports a bell as missing only when every claimed chunk is loaded.
     *
     * @param level the level storing the settlement
     */
    public SettlementHealth assess(@Nonnull ServerLevel level, @Nonnull Settlement settlement) {
        return new SettlementHealth(settlement.roster().entries().size(), this.foundingConfig.minimumResidents(),
                findBell(level, settlement.bounds()));
    }

    /**
     * Returns the accumulated fading time required for automatic disbanding, or empty when decay is disabled.
     */
    public Optional<ClockTicks> fadingDuration() {
        return Optional.ofNullable(this.fadingDuration);
    }

    /**
     * Disbands the level's settlement with the given id without changing its villagers' membership records, and reports
     * the removal to the {@link SettlementLifecyclePublisher}.
     *
     * @return the settlement as it was stored, or empty when the level stores no settlement with the given id
     */
    public Optional<Settlement> disband(@Nonnull ServerLevel level, @Nonnull UUID settlementId) {
        Optional<Settlement> disbanded = this.settlementStore.disband(level, settlementId);
        disbanded.ifPresent(settlement -> {
            log.info("Disbanded settlement {} ({}) in {}{}", settlement.name(), settlement.id(),
                    level.dimension().location(),
                    this.settlementStore.isTombstoned(level, settlementId) ? ", leaving a tombstone" : "");
            this.lifecyclePublisher.disbanded(level, settlement);
        });
        return disbanded;
    }

    /**
     * Runs one tick of periodic health checks, updating fading and disbanding settlements whose grace period expires.
     * Logs transitions into and out of fading.
     * <p>
     * Leaves settlements unchanged while their center chunk is not entity-ticking or their health is undetermined.
     * Does nothing when decay is disabled, ticks are not running normally, or the level has neither players nor forced
     * chunks.
     */
    public void tick(@Nonnull ServerLevel level) {
        if (this.fadingDuration == null || !level.tickRateManager().runsNormally() || !ticksEntities(level)) {
            return;
        }

        // Snapshot once per interval to spread visits across ticks without rescanning the store on every tick.
        Sweep sweep = this.sweepsByLevel.get(level);
        if (sweep == null || sweep.ticksRun == HEALTH_INTERVAL.getTicks()) {
            sweep = new Sweep(this.settlementStore.all(level).stream().map(Settlement::id).toList());
            this.sweepsByLevel.put(level, sweep);
        }

        sweep.ticksRun++;
        int due = (int) Math.ceilDiv(sweep.ticksRun * sweep.settlementIds.size(), HEALTH_INTERVAL.getTicks());
        while (sweep.visited < due) {
            this.visit(level, sweep.settlementIds.get(sweep.visited++), this.fadingDuration);
        }
    }

    /**
     * Revives the level's fading settlement claiming the given chunk when its living members suffice, counting a bell
     * as standing in its claim whatever the level's points of interest show yet. Logs the revival as the cadence does.
     * <p>
     * Never begins or advances fading, and does nothing when decay is disabled.
     *
     * @param bellChunk the chunk of a bell that now stands in it
     * @return whether a settlement was revived
     */
    public boolean reviveForBell(@Nonnull ServerLevel level, @Nonnull ChunkPosition bellChunk) {
        // Disabled decay leaves a fading settlement as stored, as the cadence does
        if (this.fadingDuration == null) {
            return false;
        }

        Optional<Settlement> claimant = this.settlementStore.findClaiming(level, bellChunk);
        if (claimant.isEmpty() || claimant.get().fading() == null) {
            return false;
        }

        // Counted rather than searched for: a placed bell's point of interest is registered only after its placement
        // completes. Only the health rule is applied, never a visit, which would advance fading by an interval.
        Settlement settlement = claimant.get();
        SettlementHealth health = new SettlementHealth(settlement.roster().entries().size(),
                this.foundingConfig.minimumResidents(), BellPresence.FOUND);
        if (health.status() != SettlementHealth.Status.HEALTHY) {
            return false;
        }

        this.revive(level, settlement);
        return true;
    }

    /**
     * Discards the level's in-progress health sweep.
     */
    public void forgetLevel(@Nonnull ServerLevel level) {
        this.sweepsByLevel.remove(level);
    }

    private void visit(ServerLevel level, UUID settlementId, ClockTicks duration) {
        // The roster or fading state may have changed since the sweep began; use the current record, if it still exists.
        Optional<Settlement> stored = this.settlementStore.find(level, settlementId);
        if (stored.isEmpty()) {
            return;
        }

        Settlement settlement = stored.get();
        ChunkPosition center = settlement.bounds().center();
        if (!level.isPositionEntityTicking(new ChunkPos(center.x(), center.z()).getWorldPosition())) {
            return;
        }

        SettlementHealth health = this.assess(level, settlement);
        SettlementFading fading = settlement.fading();
        switch (health.status()) {
            case HEALTHY -> {
                if (fading != null) {
                    this.revive(level, settlement);
                }
            }
            case UNHEALTHY -> {
                if (fading == null) {
                    this.beginFading(level, settlement, health.causes());
                    return;
                }

                SettlementFading advanced = fading.advance(HEALTH_INTERVAL);
                if (advanced.isSpent(duration)) {
                    log.debug("Settlement {} ({}) in {} faded for {} loaded ticks", settlement.name(), settlementId,
                            level.dimension().location(), advanced.elapsed().getTicks());
                    this.disband(level, settlementId);
                    return;
                }

                this.settlementStore.update(level, settlementId, current -> current.toBuilder().fading(advanced).build());
            }
            case UNDETERMINED -> {
                // An unloaded claimed chunk may still contain a bell, so an inconclusive check must not change fading.
            }
        }
    }

    private void beginFading(ServerLevel level, Settlement settlement, Set<Cause> causes) {
        // Persist cause identifiers so saved log details do not depend on display wording or locale.
        String detail = causes.stream()
                .map(cause -> cause.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
        SettlementLogEntry entry = logEntry(level, SettlementLogType.BEGAN_FADING, detail);
        this.settlementStore.update(level, settlement.id(), current -> current.toBuilder()
                .fading(SettlementFading.BEGUN)
                .log(current.log().append(entry))
                .build());
        log.debug("Settlement {} ({}) in {} began fading: {}", settlement.name(), settlement.id(),
                level.dimension().location(), detail);
    }

    private void revive(ServerLevel level, Settlement settlement) {
        SettlementLogEntry entry = logEntry(level, SettlementLogType.REVIVED, null);
        this.settlementStore.update(level, settlement.id(), current -> current.toBuilder()
                .fading(null)
                .log(current.log().append(entry))
                .build());
        log.debug("Settlement {} ({}) in {} revived", settlement.name(), settlement.id(), level.dimension().location());
    }

    private static boolean ticksEntities(ServerLevel level) {
        // Chunk eligibility can outlast entity simulation in an empty level. Pause decay when neither players nor forced
        // chunks remain, without waiting for the level's empty-world grace period to expire.
        return !level.players().isEmpty() || ForcedChunkManager.hasForcedChunks(level);
    }

    private static BellPresence findBell(ServerLevel level, SettlementBounds bounds) {
        PoiManager pois = level.getPoiManager();
        boolean someUnloaded = false;
        // Start at the center to avoid searching the rest of the claim when the founding bell is still present.
        for (ChunkPosition chunk : bounds.centerFirst()) {
            // Require an immediately available chunk: hasChunk can be true while loading is still pending. Querying an
            // unavailable chunk's POIs can block the server thread on disk IO. A newly generated chunk may still need a
            // first POI read even after this check passes.
            if (level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null) {
                someUnloaded = true;
                continue;
            }
            // A standing bell counts even when no villager has claimed it.
            if (pois.getInChunk(SettlementBellRules.POI_TYPE, new ChunkPos(chunk.x(), chunk.z()), PoiManager.Occupancy.ANY)
                    .findAny()
                    .isPresent()) {
                return BellPresence.FOUND;
            }
        }
        return someUnloaded ? BellPresence.NOT_KNOWABLE : BellPresence.MISSING;
    }

    private static SettlementLogEntry logEntry(ServerLevel level, SettlementLogType type, @Nullable String detail) {
        return SettlementLogEntry.builder()
                .date(new CivilDateTime(level.getDayTime()))
                .type(type)
                .detail(detail)
                .build();
    }

    /**
     * Progress through a fixed snapshot of settlement ids for one health interval.
     */
    private static final class Sweep {

        private final List<UUID> settlementIds;

        /**
         * Index of the next settlement id to visit.
         */
        private int visited;

        /**
         * Ticks processed in this sweep, up to HEALTH_INTERVAL.
         */
        private long ticksRun;

        private Sweep(List<UUID> settlementIds) {
            this.settlementIds = settlementIds;
        }

    }

}
