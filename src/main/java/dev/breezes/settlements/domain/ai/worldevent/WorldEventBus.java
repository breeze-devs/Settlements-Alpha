package dev.breezes.settlements.domain.ai.worldevent;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import lombok.CustomLog;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Global, append-only event log for the server simulation.
 * <p>
 * Modeled after Kafka's log-and-offsets pattern:
 * <ul>
 *   <li>O(1) append — producers never wait for consumers.</li>
 *   <li>Per-consumer cursors ({@code lastSeenSeq}) isolate each villager's
 *       read position; each villager processes only the delta since its last tick.</li>
 *   <li>Events are evicted after ~5 s (100 ticks). Eviction runs on the server tick,
 *       following the same pattern as {@link dev.breezes.settlements.bootstrap.event.CourtshipSessionReaperServerEvents}.</li>
 *   <li>Single-threaded server tick means no locking needed for append or scan.
 *       The {@code AtomicLong} on the sequence counter is a cheap defence against
 *       any future async emission paths.</li>
 * </ul>
 * <p>
 * Discipline rules:
 * <ul>
 *   <li>{@code PlanRunner} must function even if the bus is never ticked (events are
 *       optional fan-out, not required in-system flow).</li>
 *   <li>Emit only at semantic boundaries — never per-tick state.</li>
 *   <li>{@link WorldEventNamespace#SYSTEM} events bypass per-villager perception gates.</li>
 * </ul>
 */
@ServerScope
@CustomLog
public final class WorldEventBus {

    /**
     * Default TTL used when no config is available (e.g. in unit tests).
     * Events older than this many ticks are eligible for eviction. ~5 s at 20 tps.
     */
    public static final long DEFAULT_TTL_TICKS = 100L;

    /**
     * Effective TTL sourced from {@link EventLaneConfig}. Injected at construction time
     * so server operators can tune retention without recompiling.
     */
    private final long ttlTicks;

    /**
     * Sequence counter. Starts at 1 so that a per-consumer cursor initialized to 0
     * naturally picks up the very first event on its first tick.
     */
    private final AtomicLong sequenceCounter = new AtomicLong(1L);

    @Inject
    public WorldEventBus(EventLaneConfig config) {
        this.ttlTicks = config.worldEventTtlTicks();
    }

    /**
     * Test-only constructor that falls back to {@link #DEFAULT_TTL_TICKS}. Production wiring
     * uses the {@link Inject}-annotated constructor so retention stays operator-tunable.
     */
    public WorldEventBus() {
        this.ttlTicks = DEFAULT_TTL_TICKS;
    }

    /**
     * The live event log. Appended to by producers; head-trimmed by {@link #evict}.
     * Access is always on the server thread (or guarded by the caller), so no extra
     * synchronization is needed for list mutation.
     */
    private final List<WorldEvent> eventLog = new ArrayList<>();

    /**
     * Appends a fully constructed event to the log, assigning its monotonic sequence number.
     * <p>
     * The returned event is the same instance with the {@code seq} field populated.
     * Use {@link WorldEvent#fromPos} builder helpers for convenience.
     */
    public WorldEvent emit(WorldEvent.WorldEventBuilder builder, long gameTick) {
        long seq = this.sequenceCounter.getAndIncrement();
        WorldEvent event = builder.sequence(seq)
                .gameTick(gameTick)
                .build();
        this.eventLog.add(event);
        log.debug("WorldEventBus: emitted {} seq={} actor={}", event.getType(), seq, event.getActorId());
        return event;
    }

    /**
     * Visits all events newer than {@code lastSeenSeq} without copying the log slice.
     *
     * @return the highest sequence visited, or {@code lastSeenSeq} when no new events exist
     */
    public long visitDelta(long lastSeenSeq, Consumer<WorldEvent> visitor) {
        if (this.eventLog.isEmpty()) {
            return lastSeenSeq;
        }

        int firstNew = this.findFirstNewIndex(lastSeenSeq);
        if (firstNew >= this.eventLog.size()) {
            return lastSeenSeq;
        }

        long highestSeq = lastSeenSeq;
        for (int i = firstNew; i < this.eventLog.size(); i++) {
            WorldEvent event = this.eventLog.get(i);
            visitor.accept(event);
            highestSeq = event.getSequence();
        }
        return highestSeq;
    }

    /**
     * Returns the sequence number of the most recently appended event.
     * A villager calling this on its first tick and storing the result as
     * {@code lastSeenSeq} will skip all pre-existing events and only process
     * future ones — useful for "I just loaded in, don't replay history."
     * Returns {@code 0} if the log is empty.
     */
    public long currentSeq() {
        if (this.eventLog.isEmpty()) {
            return 0L;
        }
        return this.eventLog.getLast().getSequence();
    }

    /**
     * Removes events whose game-tick age exceeds the configured TTL.
     * Should be called once per server tick (or at a fixed sub-tick rate) by
     * {@link dev.breezes.settlements.bootstrap.event.WorldEventBusReaperServerEvents}.
     */
    public void evict(long currentGameTick) {
        long threshold = currentGameTick - this.ttlTicks;
        int before = this.eventLog.size();

        int firstRetained = 0;
        while (firstRetained < this.eventLog.size()
                && this.eventLog.get(firstRetained).getGameTick() < threshold) {
            firstRetained++;
        }
        if (firstRetained > 0) {
            this.eventLog.subList(0, firstRetained).clear();
        }
        int removed = before - this.eventLog.size();
        if (removed > 0) {
            log.debug("WorldEventBus: evicted {} events (log size now {})", removed, this.eventLog.size());
        }
    }

    /**
     * Returns a snapshot of the current log for debug tailing.
     * The returned list is a defensive copy; callers must not mutate it.
     */
    public List<WorldEvent> snapshotLog() {
        return List.copyOf(this.eventLog);
    }

    public int logSize() {
        return this.eventLog.size();
    }

    private int findFirstNewIndex(long lastSeenSeq) {
        int firstNew = this.eventLog.size();
        for (int i = this.eventLog.size() - 1; i >= 0; i--) {
            if (this.eventLog.get(i).getSequence() <= lastSeenSeq) {
                break;
            }
            firstNew = i;
        }
        return firstNew;
    }

}
