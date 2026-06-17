package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.domain.ai.observation.ObservationBuffer;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-villager mutable state for the SocialCue lane. Mirrors {@code PlanRuntimeState}
 * but is intentionally much smaller: there is no planner, no futures, no queued arrivals.
 * <p>
 * Stores:
 * <ul>
 *   <li>The currently executing {@link SocialCue} and its progress counters.</li>
 *   <li>A global per-cue-key cooldown to prevent the same cue from firing back-to-back.</li>
 *   <li>A per-target cooldown so a lingering player is not waved at every cycle.</li>
 *   <li>A gaze slot consulted by {@code LookQueries} as the middle tier in the resolution order.</li>
 *   <li>A {@link #lastSeenSeq} cursor for the {@code WorldEventBus} — seeded on load. It is the
 *       per-villager read position the Phase 4 perception gate will drain (no consumer drains it
 *       yet; the override lane reacts off the registries, not the bus).</li>
 * </ul>
 */
@Getter
public final class SocialCueRuntimeState {

    @Nullable
    private SocialCue activeCue;
    /**
     * The catalog entry that produced {@link #activeCue}.
     * Retained so the arbiter can invoke {@link SocialCueCatalogEntry#fireOnComplete}
     * when the cue finishes without re-scanning the catalog.
     */
    @Nullable
    private SocialCueCatalogEntry activeCueEntry;
    private int nextStepIndex;
    private long cueStartGameTime;

    /**
     * Gaze target set by the active cue's {@link CueStep.Gaze} step.
     * Null when no cue is active or the cue has not yet reached a gaze step.
     */
    @Nullable
    private Location gazeLookTarget;

    /**
     * Absolute game-tick at which each cue key may next be admitted.
     * Key = cue key string; value = earliest admissible game-tick.
     */
    private final Map<String, Long> cueCooldowns;

    /**
     * Absolute game-tick at which each entity target (by UUID) may next be greeted.
     * Prevents wave-spam at a player standing still next to a villager.
     */
    private final Map<UUID, Long> targetCooldowns;

    /**
     * Absolute game-tick at which this villager may next run a catalog admission scan.
     * The arbiter throttles {@code tryAdmit} to roughly 1 Hz because evaluating every cue's
     * trigger each tick (entity and knowledge-store scans) is wasted work while idle. Active-cue
     * dispatch is never gated by this — only the admission scan. Zero means "scan on the next tick".
     */
    private long nextAdmissionScanTick;

    /**
     * WorldEventBus cursor. Stores the seq of the last event processed by this villager;
     * a consumer drains only events with {@code seq > lastSeenSeq}.
     * Initialized to 0; {@link #seedCursor} skips pre-load history from {@code WorldEventBus.currentSeq()}.
     */
    private long lastSeenSeq;

    /**
     * Per-villager ring buffer for observations produced during the perception gate pass.
     * Populated by {@code PerceptionPipeline} from admitted world-events; drained by
     * the importance gate for memory promotion. Kept here rather than on a separate domain
     * record so all per-villager transient AI state is co-located.
     */
    private final ObservationBuffer observationBuffer;

    public SocialCueRuntimeState() {
        this(ObservationBuffer.DEFAULT_CAPACITY);
    }

    public SocialCueRuntimeState(int observationBufferCapacity) {
        this.cueCooldowns = new HashMap<>();
        this.targetCooldowns = new HashMap<>();
        this.observationBuffer = new ObservationBuffer(observationBufferCapacity);
        this.reset();
    }


    public boolean isCueActive() {
        return this.activeCue != null;
    }

    public void start(@Nonnull SocialCue cue, @Nullable SocialCueCatalogEntry entry, long gameTime) {
        this.activeCue = cue;
        this.activeCueEntry = entry;
        this.nextStepIndex = 0;
        this.cueStartGameTime = gameTime;
        this.gazeLookTarget = null;
    }

    public void advance() {
        this.nextStepIndex++;
    }

    public void setGazeLookTarget(@Nullable Location location) {
        this.gazeLookTarget = location;
    }

    /**
     * Returns the catalog entry that produced the active cue, if any.
     */
    @Nullable
    public SocialCueCatalogEntry getActiveCueEntry() {
        return this.activeCueEntry;
    }

    /**
     * Finishes the active cue, records the per-key cooldown, and clears transient state.
     */
    public void finish(long gameTime) {
        if (this.activeCue != null) {
            long cooldownExpiry = gameTime + this.activeCue.getCooldown().getTicks();
            this.cueCooldowns.put(this.activeCue.getKey(), cooldownExpiry);
        }

        this.activeCue = null;
        this.activeCueEntry = null;
        this.nextStepIndex = 0;
        this.gazeLookTarget = null;

        this.targetCooldowns.entrySet().removeIf(e -> e.getValue() <= gameTime);
    }


    public boolean isCueOnCooldown(String cueKey, long gameTime) {
        Long expiry = this.cueCooldowns.get(cueKey);
        return expiry != null && gameTime < expiry;
    }

    public boolean isTargetOnCooldown(UUID targetId, long gameTime) {
        Long expiry = this.targetCooldowns.get(targetId);
        return expiry != null && gameTime < expiry;
    }

    public void recordTargetGreeted(UUID targetId, long gameTime, long cooldownTicks) {
        this.targetCooldowns.put(targetId, gameTime + cooldownTicks);
    }

    /**
     * Records the absolute game-tick at which the next catalog admission scan becomes due.
     * Called by the arbiter immediately before it runs a scan.
     */
    public void scheduleNextAdmissionScan(long gameTick) {
        this.nextAdmissionScanTick = gameTick;
    }


    /**
     * Seeds the cursor to the bus's current high-water mark.
     * Call this when a villager first loads so it does not replay the entire
     * pre-existing event history once the cursor gains a consumer.
     */
    public void seedCursor(long currentBusSeq) {
        this.lastSeenSeq = currentBusSeq;
    }

    /**
     * Advances the cursor to {@code newSeq} after the perception pipeline has
     * processed its delta. Always called with the highest seq observed in the
     * current delta so the next tick's delta starts cleanly after it.
     */
    public void advanceCursor(long newSeq) {
        this.lastSeenSeq = newSeq;
    }

    public void reset() {
        this.activeCue = null;
        this.activeCueEntry = null;
        this.nextStepIndex = 0;
        this.cueStartGameTime = 0L;
        this.gazeLookTarget = null;
        this.cueCooldowns.clear();
        this.targetCooldowns.clear();
        this.nextAdmissionScanTick = 0L;
        this.lastSeenSeq = 0L;
    }

}
