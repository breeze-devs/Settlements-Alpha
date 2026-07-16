package dev.breezes.settlements.application.ai.planning;

import dagger.Lazy;
import dev.breezes.settlements.application.ai.inference.InferenceGate;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.plan.PlanGateway;
import dev.breezes.settlements.application.ai.inference.plan.PlanInferenceConfig;
import dev.breezes.settlements.application.ai.inference.plan.VillagerPlanResult;
import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.planning.PlanArrival;
import dev.breezes.settlements.domain.ai.planning.PlanAuthor;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.RestDayPolicy;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import dev.breezes.settlements.domain.world.WorldCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Minecraft-free unit tests for {@link PlanRequestService}. {@code enqueueForOverlay}/
 * {@code pumpAssembly} take/read {@link dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager},
 * which cannot be constructed in tests, so these tests exercise the class through its
 * {@code BaseVillager}-free seams: {@link PlanRequestService#captureVillager}, {@link
 * PlanRequestService#onVillager}, {@link PlanRequestService#cancelAll}, {@link
 * PlanRequestService#registerInflightHandle}, and the pure {@link PlanRequestService#shouldFlush}
 * boundary predicate — the same seams a real overlay request drives internally.
 */
@ExtendWith(MockitoExtension.class)
class PlanRequestServiceTest {

    /**
     * One server tick at 20 TPS, in nanoseconds — the cadence the assembly pump advances at.
     */
    private static final long NANOS_PER_TICK = TimeUnit.MILLISECONDS.toNanos(50);
    /**
     * The 3-second sub-batch hold window ({@code sub_batch_window_seconds} default) in nanoseconds.
     */
    private static final long WINDOW_NANOS = TimeUnit.SECONDS.toNanos(3);

    @Mock
    private PlanRequestAssembler assembler;
    @Mock
    private PlanGenerationContextFactory contextFactory;
    @Mock
    private PlanGateway gateway;
    @Mock
    private InferenceGate inferenceGate;

    private LlmOverlayPlanGenerator overlayGenerator;
    private PlanRequestService service;

    @BeforeEach
    void setUp() {
        DayPlanComposer composer = new DayPlanComposer(DefaultMealAnchorTable.rows());
        this.overlayGenerator = new LlmOverlayPlanGenerator(composer);
        PlanInferenceConfig config = new PlanInferenceConfig("LLM", 300, 20, 3, 11_000);
        Lazy<PlanGateway> lazyGateway = () -> gateway;
        this.service = new PlanRequestService(assembler, contextFactory, overlayGenerator, lazyGateway, config, inferenceGate);
    }

    @Test
    void onVillager_currentEpochAndCapturedVillager_offersLlmArrivalIntoRuntime() {
        // Arrange
        long epoch = service.cancelAll();
        UUID villagerId = UUID.randomUUID();
        PlanRuntimeState runtime = new PlanRuntimeState();
        PlanGenerationContext context = context();
        service.captureVillager(villagerId, runtime, context);
        VillagerPlanResult result = VillagerPlanResult.builder().villagerId(villagerId).selections(Map.of()).build();

        // Act
        service.onVillager(epoch, result);

        // Assert
        PlanArrival arrival = runtime.getPendingArrivals().poll();
        assertNotNull(arrival, "an arrival must have been offered");
        assertEquals(PlanAuthor.LLM, arrival.author());
        assertEquals(context.calendarDay(), arrival.plan().getCalendarDay());
    }

    @Test
    void onVillager_currentEpochButUnknownUuid_dropsResultWithoutThrowing() {
        // Arrange
        long epoch = service.cancelAll();
        VillagerPlanResult result = VillagerPlanResult.builder().villagerId(UUID.randomUUID()).selections(Map.of()).build();

        // Act + Assert — never captured, must not throw
        assertDoesNotThrow(() -> service.onVillager(epoch, result));
    }

    @Test
    void onVillager_staleEpoch_dropsResultEvenWhenUuidIsCurrentlyCaptured() {
        // Arrange — a villager captured under an epoch that is then cancelled
        long staleEpoch = service.cancelAll();
        UUID villagerId = UUID.randomUUID();
        PlanRuntimeState staleRuntime = new PlanRuntimeState();
        service.captureVillager(villagerId, staleRuntime, context());

        // cancelAll() fires again (e.g. a server stop) and the same UUID is re-captured under the
        // new epoch — proves the epoch guard fires independently of whether the UUID happens to
        // still resolve.
        service.cancelAll();
        PlanRuntimeState currentRuntime = new PlanRuntimeState();
        service.captureVillager(villagerId, currentRuntime, context());

        VillagerPlanResult result = VillagerPlanResult.builder().villagerId(villagerId).selections(Map.of()).build();

        // Act — the stale epoch's callback fires late
        service.onVillager(staleEpoch, result);

        // Assert — dropped; neither runtime received an arrival
        assertTrue(staleRuntime.getPendingArrivals().isEmpty());
        assertTrue(currentRuntime.getPendingArrivals().isEmpty());
    }

    @Test
    void cancelAll_cancelsAllPriorInFlightHandles() {
        // Arrange
        InferenceStreamHandle handle1 = mock(InferenceStreamHandle.class);
        InferenceStreamHandle handle2 = mock(InferenceStreamHandle.class);
        when(handle1.completion()).thenReturn(new CompletableFuture<>());
        when(handle2.completion()).thenReturn(new CompletableFuture<>());
        service.registerInflightHandle(handle1, Map.of());
        service.registerInflightHandle(handle2, Map.of());

        // Act
        service.cancelAll();

        // Assert
        verify(handle1).cancel();
        verify(handle2).cancel();
    }

    @Test
    void registerInflightHandle_completion_selfRemovesFromInFlightSet() {
        // Arrange — a handle whose completion future we control directly
        InferenceStreamHandle handle = mock(InferenceStreamHandle.class);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        when(handle.completion()).thenReturn(completion);
        service.registerInflightHandle(handle, Map.of());

        // Act — the stream completes normally
        completion.complete(null);

        // Act — cancelAll must not attempt to cancel the already-completed, self-removed handle
        service.cancelAll();

        // Assert — cancel was never called since the handle had already self-removed
        verify(handle, never()).cancel();
    }

    @Test
    void registerInflightHandle_completion_evictsUuidWithNoResult() {
        // Arrange — captured for this batch but never resulted (e.g. the stream times out or
        // errors before this villager's NDJSON line is produced): onVillager never fires for it,
        // so only the handle-completion sweep can clean up the stale captured entry.
        UUID strandedVillagerId = UUID.randomUUID();
        PlanRuntimeState runtime = new PlanRuntimeState();
        long strandedCaptureId = service.captureVillager(strandedVillagerId, runtime, context());

        InferenceStreamHandle handle = mock(InferenceStreamHandle.class);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        when(handle.completion()).thenReturn(completion);
        service.registerInflightHandle(handle, Map.of(strandedVillagerId, strandedCaptureId));

        // Act — the stream completes (success/failure/cancellation all funnel through this same
        // never-exceptional completion() signal) without ever producing a result for this UUID
        completion.complete(null);

        // Assert — a late/stray onVillager call for this UUID at the still-current epoch now finds
        // nothing and drops silently. Had the entry survived (the leak this fix targets), onVillager
        // would have offered an arrival here instead, so this proves eviction actually happened.
        VillagerPlanResult lateResult = VillagerPlanResult.builder().villagerId(strandedVillagerId).selections(Map.of()).build();
        service.onVillager(0L, lateResult);
        assertNull(runtime.getPendingArrivals().poll(), "captured entry must have been evicted on handle completion");
    }

    @Test
    void registerInflightHandle_completion_onlyEvictsItsOwnBatchUuids() {
        // Arrange — a UUID captured by a different, still-in-flight batch must survive an unrelated
        // batch's completion; eviction must be scoped to the completing handle's own roster.
        UUID otherBatchVillagerId = UUID.randomUUID();
        PlanRuntimeState otherRuntime = new PlanRuntimeState();
        service.captureVillager(otherBatchVillagerId, otherRuntime, context());

        InferenceStreamHandle handle = mock(InferenceStreamHandle.class);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        when(handle.completion()).thenReturn(completion);
        // This handle's batch owns a different villager (and capture id) than the one under test.
        service.registerInflightHandle(handle, Map.of(UUID.randomUUID(), 999L));

        // Act
        completion.complete(null);

        // Assert — the untouched UUID is still resolvable
        VillagerPlanResult result = VillagerPlanResult.builder().villagerId(otherBatchVillagerId).selections(Map.of()).build();
        service.onVillager(0L, result);
        assertNotNull(otherRuntime.getPendingArrivals().poll(), "unrelated batch's capture must not have been evicted");
    }

    @Test
    void registerInflightHandle_completion_doesNotEvictReEnqueuedCaptureForSameVillager() {
        // The streaming-ownership race: villager V's result lands and is evicted, V re-enqueues into
        // a NEW batch (a fresh capture under the same UUID) while the FIRST batch's handle is still
        // open, then that first handle finally completes. Its sweep must evict only the capture it
        // staged — not the newer one — or V's second overlay is silently dropped.
        UUID villagerId = UUID.randomUUID();

        // First batch: capture V and register its still-open handle, owning that first capture id.
        PlanRuntimeState firstRuntime = new PlanRuntimeState();
        long firstCaptureId = service.captureVillager(villagerId, firstRuntime, context());
        InferenceStreamHandle firstHandle = mock(InferenceStreamHandle.class);
        CompletableFuture<Void> firstCompletion = new CompletableFuture<>();
        when(firstHandle.completion()).thenReturn(firstCompletion);
        service.registerInflightHandle(firstHandle, Map.of(villagerId, firstCaptureId));

        // V's first result lands (evicting the first capture), then V re-enqueues under a fresh
        // capture with a new runtime — all while the first handle is still open.
        service.onVillager(0L, VillagerPlanResult.builder().villagerId(villagerId).selections(Map.of()).build());
        PlanRuntimeState secondRuntime = new PlanRuntimeState();
        service.captureVillager(villagerId, secondRuntime, context());

        // Act — only now does the older, first batch's handle complete.
        firstCompletion.complete(null);

        // Assert — the second capture survives: its result still resolves and offers an arrival.
        // A blanket remove-by-UUID (the bug) would have erased it, leaving nothing to resolve here.
        service.onVillager(0L, VillagerPlanResult.builder().villagerId(villagerId).selections(Map.of()).build());
        assertNotNull(secondRuntime.getPendingArrivals().poll(),
                "re-enqueued capture must survive the older batch's completion sweep");
    }

    @Test
    void isTargetDayPastCutoff_targetDayNotYetStarted_returnsFalse() {
        // The normal next-day overlay: enqueued during the evening of day 4 for day 5's wake. The
        // target day has not begun, so its offset is negative — nowhere near the cutoff, always enqueue.
        long eveningOfDay4 = WorldCalendar.absoluteTickForCivil(4, 18_000);
        long wakeDay5 = WorldCalendar.absoluteTickForCivil(5, 6_000);
        assertFalse(PlanRequestService.isTargetDayPastCutoff(eveningOfDay4, wakeDay5, 11_000));
    }

    @Test
    void isTargetDayPastCutoff_forwardJumpPastCutoffOnTargetDay_returnsTrue() {
        // The case the cutoff exists for: a forward jump lands at 14:00 on the very day the overlay
        // would plan — past the 11:00 cutoff, so skip and let the heuristic stand.
        long twoPmDay5 = WorldCalendar.absoluteTickForCivil(5, 14_000);
        long wakeDay5 = WorldCalendar.absoluteTickForCivil(5, 6_000);
        assertTrue(PlanRequestService.isTargetDayPastCutoff(twoPmDay5, wakeDay5, 11_000));
    }

    @Test
    void isTargetDayPastCutoff_beforeCutoffOnTargetDay_returnsFalse() {
        // Same-day but only 09:00 — still ahead of the cutoff, so the overlay is worth requesting.
        long nineAmDay5 = WorldCalendar.absoluteTickForCivil(5, 9_000);
        long wakeDay5 = WorldCalendar.absoluteTickForCivil(5, 6_000);
        assertFalse(PlanRequestService.isTargetDayPastCutoff(nineAmDay5, wakeDay5, 11_000));
    }

    @Test
    void isTargetDayPastCutoff_exactlyAtCutoff_returnsFalse() {
        // Exactly at the cutoff is not "past" it (strict >), so it still enqueues.
        long elevenAmDay5 = WorldCalendar.absoluteTickForCivil(5, 11_000);
        long wakeDay5 = WorldCalendar.absoluteTickForCivil(5, 6_000);
        assertFalse(PlanRequestService.isTargetDayPastCutoff(elevenAmDay5, wakeDay5, 11_000));
    }

    @Test
    void shouldFlush_bufferAtSubBatchSize_returnsTrueRegardlessOfWindow() {
        // Size arm: 20 buffered at a 20 sub-batch size flushes even though no window has elapsed.
        assertTrue(PlanRequestService.shouldFlush(20, 20, 0L, 0L, WINDOW_NANOS));
    }

    @Test
    void shouldFlush_bufferBelowSizeAndWindowNotElapsed_returnsFalse() {
        // 19 buffered, opened at time 0, now only half the window in — under both arms.
        assertFalse(PlanRequestService.shouldFlush(19, 20, WINDOW_NANOS / 2, 0L, WINDOW_NANOS));
    }

    @Test
    void shouldFlush_bufferBelowSizeButWindowElapsed_returnsTrue() {
        // Time arm: only 5 buffered, but the buffer opened a full window ago — send the partial.
        assertTrue(PlanRequestService.shouldFlush(5, 20, WINDOW_NANOS, 0L, WINDOW_NANOS));
    }

    @Test
    void shouldFlush_windowElapsedExactlyAtBoundary_returnsTrue() {
        assertTrue(PlanRequestService.shouldFlush(1, 20, WINDOW_NANOS, 0L, WINDOW_NANOS));
    }

    @Test
    void shouldFlush_emptyBuffer_neverFlushesEvenPastWindow() {
        // An empty buffer must never flush — the drained cursor is no longer a flush trigger.
        assertFalse(PlanRequestService.shouldFlush(0, 20, 10 * WINDOW_NANOS, 0L, WINDOW_NANOS));
    }

    @Test
    void simulateSubBatchBoundaries_rosterExactMultipleOfSubBatch_flushesEvenSizeBatchesOnSizeArm() {
        assertEquals(List.of(20, 20), simulateFlushBatchSizes(40, 8, 20));
    }

    @Test
    void simulateSubBatchBoundaries_rosterHitsSubBatchSizeExactly_flushesSingleFullBatchNoWindowWait() {
        assertEquals(List.of(20), simulateFlushBatchSizes(20, 8, 20));
    }

    @Test
    void simulateSubBatchBoundaries_partialTail_flushesRemainderOnWindowNotOnDrain() {
        // 250 = twelve full batches of 20, then a trailing 10 that only leaves on the window.
        List<Integer> expected = new ArrayList<>(Collections.nCopies(12, 20));
        expected.add(10);
        assertEquals(expected, simulateFlushBatchSizes(250, 8, 20));
    }

    @Test
    void simulateSubBatchBoundaries_smallRosterNeverReachingSubBatchSize_flushesOnceOnWindow() {
        assertEquals(List.of(5), simulateFlushBatchSizes(5, 8, 20));
    }

    @Test
    void simulateSubBatchBoundaries_subThresholdRoster_heldUntilWindowThenFlushed() {
        assertEquals(List.of(19), simulateFlushBatchSizes(19, 8, 20));
    }

    /**
     * Drives {@link PlanRequestService#shouldFlush} the same way {@link
     * PlanRequestService#pumpAssembly(long)} does: each simulated server tick drains up to {@code
     * chunkSize} villagers into the buffer, flushing on the size arm mid-chunk and on the time arm
     * once a partial buffer has waited out {@link #WINDOW_NANOS}. The wall clock advances
     * {@link #NANOS_PER_TICK} per simulated tick, exactly as {@code System.nanoTime()} does between
     * pump calls. Returns the flushed batch sizes in order — no real {@code BaseVillager} roster
     * required.
     */
    private static List<Integer> simulateFlushBatchSizes(int rosterSize, int chunkSize, int subBatchSize) {
        List<Integer> flushes = new ArrayList<>();
        int remaining = rosterSize;
        int buffered = 0;
        long bufferOpenedNanos = -1L;
        long nowNanos = 0L;

        // Keep ticking until the roster is drained AND the buffer has been flushed — the tail may
        // outlive the drain by up to the window, so draining alone is not the loop's exit.
        while (remaining > 0 || buffered > 0) {
            int tickBudget = Math.min(chunkSize, remaining);
            for (int i = 0; i < tickBudget; i++) {
                remaining--;
                if (buffered == 0) {
                    bufferOpenedNanos = nowNanos;
                }
                buffered++;
                if (PlanRequestService.shouldFlush(buffered, subBatchSize, nowNanos, bufferOpenedNanos, WINDOW_NANOS)) {
                    flushes.add(buffered);
                    buffered = 0;
                    bufferOpenedNanos = -1L;
                }
            }
            // The trailing time-arm flush pumpAssembly performs after its chunk loop.
            if (buffered > 0 && PlanRequestService.shouldFlush(buffered, subBatchSize, nowNanos, bufferOpenedNanos, WINDOW_NANOS)) {
                flushes.add(buffered);
                buffered = 0;
                bufferOpenedNanos = -1L;
            }
            nowNanos += NANOS_PER_TICK;
        }
        return flushes;
    }

    private static PlanGenerationContext context() {
        VillagerProfessionKey profession = VillagerProfessionKey.FARMER;
        ScheduleProfile schedule = ScheduleProfile.defaultFor(profession);
        List<WeightedBehavior> behaviors = List.of(weighted(BehaviorKey.HARVEST_SUGARCANE));
        return PlanGenerationContext.builder()
                .profession(profession)
                .genetics(genetics())
                .scheduleProfile(schedule)
                .restDayPolicy(RestDayPolicy.defaultFor(profession))
                .dayType(PlanDayType.WORK_DAY)
                .availableBehaviors(behaviors)
                .wakeAtAbsoluteTick(schedule.defaultWakeTick())
                .build();
    }

    private static WeightedBehavior weighted(BehaviorKey key) {
        return new WeightedBehavior(BehaviorPlanningMetadata.builder()
                .key(key)
                .displayName(key.id())
                .description(key.id())
                .category(BehaviorCategory.WORK)
                .intensity(WorkIntensity.HEAVY)
                .estimatedDuration(GameTicks.minutes(20))
                .preconditionSummary("test")
                .build(), 1);
    }

    private static GeneticsProfile genetics() {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        return new GeneticsProfile(genes);
    }

}
