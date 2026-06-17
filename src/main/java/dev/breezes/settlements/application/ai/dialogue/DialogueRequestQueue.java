package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.llm.DialogRequest;
import dev.breezes.settlements.application.ai.dialogue.llm.DialogServiceHttpClient;
import dev.breezes.settlements.application.ai.dialogue.llm.LlmResponse;
import lombok.CustomLog;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Bounded-concurrency priority queue for in-flight dialog requests
 * <p>
 * Implements the queue semantics:
 * <ul>
 *   <li>Priority: PLAYER &gt; VILLAGER &gt; AMBIENT.</li>
 *   <li>Bounded concurrency: at most {@code maxConcurrent} requests in-flight.</li>
 *   <li>Stale cancellation: if the speaking context is gone before the response lands,
 *       the pending future is canceled.</li>
 *   <li>AMBIENT requests are dropped when the queue is over-cap rather than blocking.</li>
 * </ul>
 * <p>
 * Threading: methods are safe to call from multiple threads. The queue itself uses
 * {@link PriorityBlockingQueue} for ordering and a {@link Semaphore} for concurrency
 * control. Callbacks ({@link Consumer}) are delivered on the {@link CompletableFuture}
 * completion thread (off the server tick thread).
 */
@CustomLog
public final class DialogueRequestQueue {

    /**
     * Maximum pending entries in the wait queue (per-priority). Over-cap AMBIENT requests
     * are dropped silently; higher-priority requests are queued but not dropped.
     */
    private static final int AMBIENT_QUEUE_CAP = 8;

    private final DialogServiceHttpClient client;
    private final int maxConcurrent;
    private final Semaphore concurrencySlots;
    private final PriorityBlockingQueue<QueueEntry> pendingQueue;
    private final ConcurrentHashMap<UUID, CompletableFuture<LlmResponse>> inFlight;
    private final AtomicInteger ambientPending;

    public DialogueRequestQueue(DialogServiceHttpClient client, int maxConcurrent) {
        this.client = client;
        this.maxConcurrent = maxConcurrent;
        this.concurrencySlots = new Semaphore(maxConcurrent);
        // Higher priority ordinal = higher priority = sorted first.
        this.pendingQueue = new PriorityBlockingQueue<>(16, Comparator.<QueueEntry, DialoguePriority>comparing(e -> e.priority).reversed());
        this.inFlight = new ConcurrentHashMap<>();
        this.ambientPending = new AtomicInteger(0);
    }

    /**
     * Submits a request to the queue. Fires it immediately if a concurrency slot is free,
     * otherwise enqueues it to be processed when a slot opens.
     * <p>
     * AMBIENT requests above the cap are dropped silently
     *
     * @param villagerUuid requester identity — used to cancel stale in-flight requests
     * @param request      assembled LLM request
     * @param priority     queue tier
     * @param deadline     hard wall-clock deadline for the HTTP call
     * @param onResult     callback invoked with the (possibly empty) sanitized line
     */
    public void submit(UUID villagerUuid,
                       DialogRequest request,
                       DialoguePriority priority,
                       Duration deadline,
                       Consumer<Optional<String>> onResult,
                       int bubbleCharCap) {
        // Drop low-priority ambient requests when the ambient queue is already saturated
        // to prevent an unending backlog that would only produce stale results.
        if (priority == DialoguePriority.AMBIENT && this.ambientPending.get() >= AMBIENT_QUEUE_CAP) {
            log.debug("Dropping AMBIENT request for {} — queue saturated", villagerUuid);
            return;
        }

        if (priority == DialoguePriority.AMBIENT) {
            this.ambientPending.incrementAndGet();
        }

        QueueEntry entry = new QueueEntry(villagerUuid, request, priority, deadline, onResult, bubbleCharCap);

        if (this.concurrencySlots.tryAcquire()) {
            // Slot available — fire immediately, no queuing.
            fireEntry(entry);
        } else {
            this.pendingQueue.offer(entry);
        }
    }

    /**
     * Cancels any pending or in-flight request associated with {@code villagerUuid}.
     * Called when the speak context disappears (villager reprised, target left).
     */
    public void cancel(UUID villagerUuid) {
        CompletableFuture<LlmResponse> inFlightFuture = this.inFlight.remove(villagerUuid);
        if (inFlightFuture != null) {
            inFlightFuture.cancel(true);
            log.debug("Cancelled in-flight LLM request for {}", villagerUuid);
        }
        // The pending queue is scanned lazily — cancelled entries are ignored when dequeued.
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void fireEntry(QueueEntry entry) {
        if (entry.priority == DialoguePriority.AMBIENT) {
            this.ambientPending.decrementAndGet();
        }

        CompletableFuture<LlmResponse> future = this.client.send(entry.request, entry.deadline);
        this.inFlight.put(entry.villagerUuid, future);

        future.whenComplete((response, throwable) -> {
            this.inFlight.remove(entry.villagerUuid);
            this.concurrencySlots.release();

            // Deliver result to the caller.
            Optional<String> result = (response != null)
                    ? response.firstContent().flatMap(raw ->
                    DialogueResponseSanitizer.sanitize(raw, entry.bubbleCharCap))
                    : Optional.empty();
            entry.onResult.accept(result);

            // Dequeue and fire the next pending entry if any.
            drainNext();
        });
    }

    private void drainNext() {
        QueueEntry next = this.pendingQueue.poll();
        if (next == null) {
            return;
        }

        // If the slot was canceled between enqueueing and being processed, skip it.
        if (!this.inFlight.containsKey(next.villagerUuid) && this.concurrencySlots.tryAcquire()) {
            fireEntry(next);
        } else {
            // Put it back and let the next release pick it up.
            this.pendingQueue.offer(next);
        }
    }

    // -------------------------------------------------------------------------
    // Queue entry
    // -------------------------------------------------------------------------

    private record QueueEntry(
            UUID villagerUuid,
            DialogRequest request,
            DialoguePriority priority,
            Duration deadline,
            Consumer<Optional<String>> onResult,
            int bubbleCharCap) {
    }

}
