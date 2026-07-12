package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.dialogue.MonologueRequestService;
import dev.breezes.settlements.application.ai.dialogue.RehearsedDialogueProvider;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.plan.PlanBatchRequest;
import dev.breezes.settlements.application.ai.inference.plan.PlanGateway;
import dev.breezes.settlements.application.ai.inference.plan.PlanInferenceConfig;
import dev.breezes.settlements.application.ai.inference.plan.PlanInferenceMode;
import dev.breezes.settlements.application.ai.inference.plan.VillagerPlanResult;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PlanArrival;
import dev.breezes.settlements.domain.ai.planning.PlanAuthor;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the LLM PLAN overlay's backend round-trip, on an always-on, per-villager basis: chunked
 * context assembly, sub-batched streaming requests, and direct-offer installation of results into
 * each villager's own {@code pendingArrivals} queue.
 * <p>
 * There is no periodic trigger. Instead, {@code PlanRunner#submitNextPlanAsync} calls
 * {@link #enqueueForOverlay} every time a villager's plan is exhausted and its heuristic successor
 * is submitted — one villager at a time, additively. The heuristic plan remains the immediate
 * floor (it lands first via the async path); the LLM overlay rides the existing arbitration in
 * {@code PlanRunner#arbitrate}/{@code adoptPendingIfReady}, superseding same-day and adopting at
 * the next slot boundary. Forward time-jumps (player sleep, {@code /time set}) also route through
 * {@code submitNextPlanAsync} via the calendar-mismatch/overdue branches, so a single hook covers
 * both normal day-end and time-jumps.
 * <p>
 * Modeled on {@link MonologueRequestService} (the backend round-trip) crossed with
 * {@link RehearsedDialogueProvider}'s epoch/cancel supersede pattern (generalized here to a SET of
 * in-flight handles, since sub-batching means many enqueues can be in flight as distinct streaming
 * requests at once). The key departure from the monologue pack: plan-context assembly reads the
 * villager brain and is too heavy to do inline on enqueue, so assembly is chunked across ticks via
 * {@link #pumpAssembly(long)} rather than done all at once in {@link #enqueueForOverlay}.
 * <p>
 * Threading: {@link #enqueueForOverlay} and {@link #pumpAssembly(long)} run on the server tick
 * thread only — they read live {@link BaseVillager} state (day plan, brain-derived context). The
 * gateway callback ({@link #onVillager}) runs on the HTTP client's executor thread and touches
 * nothing but the captured, already-immutable {@link PlanGenerationContext} and the concurrent
 * {@code pendingArrivals} queue on the captured {@link PlanRuntimeState} — exactly the same
 * off-thread contract {@code PlanRunner#submitNextPlanAsync} already relies on.
 */
@CustomLog
@ServerScope
public final class PlanRequestService {

    /**
     * Max number of villagers whose {@link PlanGenerationContext} is assembled per server tick.
     */
    private static final int MAX_VILLAGERS_TO_PROCESS_PER_TICK = 8;

    /**
     * Sentinel for {@link #bufferOpenedNanos} when the sub-batch buffer holds no villagers.
     */
    private static final long BUFFER_EMPTY = -1L;

    private final PlanRequestAssembler assembler;
    private final PlanGenerationContextFactory contextFactory;
    private final LlmOverlayPlanGenerator overlayGenerator;
    private final PlanGateway gateway;
    private final PlanInferenceConfig config;

    // Constructor is hand-rolled (rather than @AllArgsConstructor) so the mutable runtime state below
    // can carry inline initializers without being pulled into the injected constructor signature.
    @Inject
    PlanRequestService(PlanRequestAssembler assembler,
                       PlanGenerationContextFactory contextFactory,
                       LlmOverlayPlanGenerator overlayGenerator,
                       PlanGateway gateway,
                       PlanInferenceConfig config) {
        this.assembler = assembler;
        this.contextFactory = contextFactory;
        this.overlayGenerator = overlayGenerator;
        this.gateway = gateway;
        this.config = config;
    }

    /**
     * One villager's request to be assembled and sent, carrying the target wake tick the enqueue
     * site ({@code PlanRunner#submitNextPlanAsync}) already resolved. Different villagers can be
     * enqueued for different target days at different times, so the wake tick must travel with the
     * request rather than being derived from a single shared value.
     */
    private record OverlayRequest(BaseVillager villager, long wakeAtAbsoluteTick) {
    }

    /**
     * Drain cursor of villagers awaiting context assembly. Additive — {@link #enqueueForOverlay}
     * only ever appends. Server-thread-only: mutated exclusively by {@link #enqueueForOverlay} and
     * {@link #pumpAssembly(long)}.
     */
    private final ArrayDeque<OverlayRequest> drainCursor = new ArrayDeque<>();

    /**
     * Villagers assembled but not yet flushed into a sub-batch POST. Server-thread-only, same as
     * {@link #drainCursor}.
     */
    private final List<PlanRequestAssembler.VillagerContext> subBatchBuffer = new ArrayList<>();

    /**
     * The monotonic {@link System#nanoTime()} reading at which {@link #subBatchBuffer} received its
     * first villager, or {@link #BUFFER_EMPTY} while the buffer is empty. The time arm of
     * {@link #shouldFlush} measures the buffer's real-world age against this so a partial batch is
     * never held past its window.
     * <p>
     * A plain {@code long}, not an atomic: like {@link #subBatchBuffer} and {@link #drainCursor} it
     * is only ever touched on the server thread.
     */
    private long bufferOpenedNanos = BUFFER_EMPTY;

    /**
     * Captured (runtime, context) per villager, keyed by UUID, held only until that villager's
     * result lands (or its batch's handle completes without one — see {@link #registerInflightHandle}).
     * Also doubles as the in-flight dedupe set consulted by {@link #enqueueForOverlay}: a UUID
     * present here already has a request staged or outstanding. Written on the server thread during
     * assembly, read and removed off-thread by {@link #onVillager} and the completion callback, so
     * this must be concurrent.
     */
    private final ConcurrentHashMap<UUID, CapturedVillager> captured = new ConcurrentHashMap<>();

    /**
     * Monotonic source of per-capture ids stamped onto each {@link CapturedVillager}. Lets the
     * handle-completion sweep in {@link #registerInflightHandle} evict only the exact capture its
     * batch staged, never a newer one a re-enqueue installed under the same UUID while the batch was
     * still in flight. Server-thread-only (captures happen during assembly), but kept atomic to
     * mirror {@link #overlayEpoch} and stay robust if a capture ever moves off-thread.
     */
    private final AtomicLong captureSequence = new AtomicLong(0L);

    /**
     * In-flight streaming handles. Sub-batching means many concurrent enqueues can produce many
     * handles (one per {@link #flush}) — {@link #cancelAll()} cancels every one of them, and
     * completed ones self-remove from either thread.
     */
    private final Set<InferenceStreamHandle> inflightHandles = ConcurrentHashMap.newKeySet();

    /**
     * Monotonically increasing epoch counter, advanced only by {@link #cancelAll()}. A gateway
     * callback captures the epoch at flush time and drops its result if the epoch has since
     * advanced — see {@link #onVillager}. In the always-on model this is dormant (never advances)
     * except around a full {@link #cancelAll()}, kept as a supersede/cancel safety net rather than
     * a per-enqueue mechanism. Atomic because it is read off-thread by the gateway callback.
     */
    private final AtomicLong overlayEpoch = new AtomicLong(0L);

    /**
     * A villager's plan-relevant state captured at assembly time: a per-capture {@code captureId}
     * that uniquely identifies THIS capture (so a batch's completion sweep can tell its own entry
     * apart from a newer re-enqueue's), the runtime POJO (never the {@link BaseVillager} itself —
     * the villager may unload before the result lands), and the immutable context the request was
     * built from.
     */
    private record CapturedVillager(long captureId, PlanRuntimeState runtime, PlanGenerationContext context) {
    }

    /**
     * Additively stages one villager for an LLM PLAN overlay targeting {@code wakeAtAbsoluteTick}.
     * Called from {@code PlanRunner#submitNextPlanAsync} every time a villager's heuristic
     * successor is submitted, so this is the sole trigger for the overlay — there is no periodic
     * sweep.
     * <p>
     * No-ops when the mode is not {@link PlanInferenceMode#LLM} (the gate lives here so callers
     * don't need the config); when a forward time-jump has left {@code dayTime} already past
     * {@link PlanInferenceConfig#overlayCutoffCivilTick()} within the target plan's own day
     * ({@link #isTargetDayPastCutoff}) — the day is too far along to be worth an LLM plan, so the
     * heuristic floor stands and the villager picks up an overlay at its next natural exhaustion; or
     * when this villager already has a request staged or in flight ({@link #captured} dedupe) —
     * re-triggering before a prior overlay lands must never spawn a second concurrent request for the
     * same villager.
     */
    public void enqueueForOverlay(@Nonnull BaseVillager villager, long dayTime, long wakeAtAbsoluteTick) {
        if (this.config.resolvedMode() != PlanInferenceMode.LLM) {
            return;
        }
        if (isTargetDayPastCutoff(dayTime, wakeAtAbsoluteTick, this.config.overlayCutoffCivilTick())) {
            return;
        }
        if (this.captured.containsKey(villager.getUUID())) {
            return;
        }

        this.drainCursor.add(new OverlayRequest(villager, wakeAtAbsoluteTick));
    }

    /**
     * True when {@code dayTime} already sits past {@code cutoffCivilTick} within the target plan's own
     * calendar day — the day is too far along for an LLM overlay to be worth requesting. Only ever
     * true after a forward time-jump: a normal next-day overlay targets a day that has not started
     * yet, so its {@link WorldCalendar#civilOffsetWithin} reads negative and stays well below the cutoff.
     */
    @VisibleForTesting
    static boolean isTargetDayPastCutoff(long dayTime, long wakeAtAbsoluteTick, int cutoffCivilTick) {
        long targetCalendarDay = WorldCalendar.calendarDayOf(wakeAtAbsoluteTick);
        return WorldCalendar.civilOffsetWithin(targetCalendarDay, dayTime) > cutoffCivilTick;
    }

    /**
     * Assembles up to {@link #MAX_VILLAGERS_TO_PROCESS_PER_TICK} villagers from the drain cursor,
     * flushing a sub-batch whenever {@link #shouldFlush} says so — the buffer reached the configured
     * size, or a partial buffer has waited out its window. Must run every tick even after the cursor
     * drains so a partial tail still flushes on its window; no-ops cheaply only when nothing is
     * pending, so it is safe to call unconditionally every tick.
     *
     * @param nowNanos the caller's current {@link System#nanoTime()} reading, used to time the
     *                 window-based flush of a partial sub-batch
     */
    public void pumpAssembly(long nowNanos) {
        if (this.drainCursor.isEmpty() && this.subBatchBuffer.isEmpty()) {
            return;
        }

        long epoch = this.overlayEpoch.get();
        int subBatchSize = this.config.subBatchSize();
        long windowNanos = TimeUnit.SECONDS.toNanos(this.config.subBatchWindowSeconds());

        for (int i = 0; i < MAX_VILLAGERS_TO_PROCESS_PER_TICK && !this.drainCursor.isEmpty(); i++) {
            this.assembleOne(this.drainCursor.poll(), nowNanos);
            if (shouldFlush(this.subBatchBuffer.size(), subBatchSize, nowNanos, this.bufferOpenedNanos, windowNanos)) {
                this.flush(epoch);
            }
        }

        if (shouldFlush(this.subBatchBuffer.size(), subBatchSize, nowNanos, this.bufferOpenedNanos, windowNanos)) {
            this.flush(epoch);
        }
    }

    /**
     * Builds one villager's {@link PlanGenerationContext} on the server thread and stages it for
     * the next flush. Re-validates liveness here (not just at {@link #enqueueForOverlay} time)
     * because a villager can unload between enqueue and its turn in the drain cursor.
     */
    private void assembleOne(@Nonnull OverlayRequest request, long nowNanos) {
        BaseVillager villager = request.villager();
        if (!villager.isAlive() || villager.isRemoved()) {
            return;
        }

        PlanGenerationContext context = this.contextFactory.create(villager, request.wakeAtAbsoluteTick());

        this.captureVillager(villager.getUUID(), villager.getPlanRuntimeState(), context);
        // Stamp the buffer's open time on the empty -> non-empty transition so its window is timed
        // from the first villager staged, not the most recent.
        if (this.subBatchBuffer.isEmpty()) {
            this.bufferOpenedNanos = nowNanos;
        }
        this.subBatchBuffer.add(new PlanRequestAssembler.VillagerContext(villager, context));
    }

    /**
     * Records a villager's (runtime, context) pair ahead of a flush and returns the unique
     * {@code captureId} stamped on it — {@link #flush} snapshots that id so the batch's completion
     * sweep can later evict this exact capture and no other. Split out from {@link #assembleOne} so
     * the capture step is exercisable without a {@link BaseVillager}, which cannot be constructed in
     * tests.
     */
    @VisibleForTesting
    long captureVillager(@Nonnull UUID villagerId, @Nonnull PlanRuntimeState runtime, @Nonnull PlanGenerationContext context) {
        long captureId = this.captureSequence.getAndIncrement();
        this.captured.put(villagerId, new CapturedVillager(captureId, runtime, context));
        return captureId;
    }

    /**
     * Sends the buffered sub-batch and registers the returned handle. A no-op on an empty buffer
     * (defensive; callers gate on {@link #shouldFlush}, which never signals on an empty buffer).
     */
    private void flush(long epoch) {
        if (this.subBatchBuffer.isEmpty()) {
            return;
        }

        List<PlanRequestAssembler.VillagerContext> batch = List.copyOf(this.subBatchBuffer);
        this.subBatchBuffer.clear();
        this.bufferOpenedNanos = BUFFER_EMPTY;

        Map<UUID, Long> ownedCaptures = new HashMap<>();
        for (PlanRequestAssembler.VillagerContext entry : batch) {
            UUID villagerId = entry.villager().getUUID();
            CapturedVillager capture = this.captured.get(villagerId);
            if (capture != null) {
                ownedCaptures.put(villagerId, capture.captureId());
            }
        }

        PlanBatchRequest request = this.assembler.assemble(batch);
        Duration deadline = Duration.ofSeconds(this.config.overlayDeadlineSeconds());
        InferenceStreamHandle handle = this.gateway.generate(request, deadline, result -> this.onVillager(epoch, result));
        this.registerInflightHandle(handle, ownedCaptures);

        log.debug("PLAN overlay sub-batch flushed (epoch {}): {} villager(s)", epoch, batch.size());
    }

    /**
     * Registers a handle as in-flight and arranges its self-removal on completion, from whichever
     * thread that fires on. {@code ownedCaptures} maps each of this batch's villagers to the
     * {@code captureId} it staged: on completion — success, failure, or cancellation,
     * {@link InferenceStreamHandle#completion()} never completes exceptionally — each of those
     * captures is evicted from {@link #captured}, but only if it is still the SAME capture (matched
     * by id), never a newer one.
     * <p>
     * This is the leak fix for the always-on model: {@link #onVillager} already removes a UUID on
     * the happy path, but a dropped result (timeout, error, or an upstream line that never arrives)
     * would otherwise leave that UUID captured forever, permanently blocking it from re-enqueueing
     * via the dedupe guard in {@link #enqueueForOverlay}. Nothing runs periodically to clear it, so
     * this per-batch eviction on handle completion is the only cleanup path for stragglers.
     * <p>
     * The id match is what keeps eviction request-scoped: a villager whose result already landed can
     * re-enqueue and be re-captured under a fresh id while this (older) batch's handle is still open.
     * A blanket remove-by-UUID here would erase that newer capture and silently drop its overlay — so
     * the sweep removes an entry only when its live {@code captureId} equals the one this batch owns.
     */
    @VisibleForTesting
    void registerInflightHandle(@Nonnull InferenceStreamHandle handle, @Nonnull Map<UUID, Long> ownedCaptures) {
        this.inflightHandles.add(handle);
        handle.completion().thenRun(() -> {
            this.inflightHandles.remove(handle);
            ownedCaptures.forEach((villagerId, ownedCaptureId) ->
                    this.captured.computeIfPresent(villagerId,
                            (id, capture) -> capture.captureId() == ownedCaptureId ? null : capture));
        });
    }

    /**
     * Converts one streamed result into a {@link PlanArrival} and offers it directly into that
     * villager's captured runtime — no second mailbox, no {@code PlanRunner} hook; the runner's
     * existing {@code drainPendingArrivals} already drains this queue every tick.
     * <p>
     * Runs on the HTTP client's executor thread. Drops silently (no throw) when the epoch has
     * advanced past the one captured at flush time (a {@link #cancelAll()} superseded this request)
     * or when the villager UUID has no captured entry (unloaded before the result arrived, or
     * already evicted by {@link #registerInflightHandle}).
     */
    @VisibleForTesting
    void onVillager(long epoch, @Nonnull VillagerPlanResult result) {
        if (epoch != this.overlayEpoch.get()) {
            return;
        }

        CapturedVillager entry = this.captured.remove(result.getVillagerId());
        if (entry == null) {
            return;
        }

        DayPlan plan = this.overlayGenerator.generate(entry.context(), result);
        entry.runtime().getPendingArrivals().offer(new PlanArrival(plan, PlanAuthor.LLM));
    }

    /**
     * Advances the epoch, cancels and clears every in-flight handle, and clears all pending
     * buffers/captures. Deliberately NOT called per-enqueue — enqueue is additive and must never
     * cancel another villager's in-flight request — so this is reserved for a full-stop cleanup
     * (server shutdown, wired from {@code ServerLifecycleEvents#onServerStopped}) or a mode flip
     * back to HEURISTIC.
     */
    public long cancelAll() {
        long epoch = this.overlayEpoch.incrementAndGet();

        for (InferenceStreamHandle handle : this.inflightHandles) {
            handle.cancel();
        }
        this.inflightHandles.clear();

        this.drainCursor.clear();
        this.subBatchBuffer.clear();
        this.bufferOpenedNanos = BUFFER_EMPTY;
        this.captured.clear();

        return epoch;
    }

    /**
     * The sub-batch flush boundary: send once the buffer reaches {@code subBatchSize} (the size arm)
     * or the window has elapsed since the buffer's first villager was staged (the time arm),
     * whichever comes first. An empty buffer never flushes.
     *
     * @param nowNanos          the caller's current {@link System#nanoTime()} reading
     * @param bufferOpenedNanos the {@code nanoTime} the buffer's first villager was staged at
     * @param windowNanos       max nanos a partial buffer may wait before the time arm fires
     */
    @VisibleForTesting
    static boolean shouldFlush(int bufferSize, int subBatchSize, long nowNanos, long bufferOpenedNanos, long windowNanos) {
        if (bufferSize <= 0) {
            return false;
        }
        if (bufferSize >= subBatchSize) {
            return true;
        }
        return nowNanos - bufferOpenedNanos >= windowNanos;
    }

}
