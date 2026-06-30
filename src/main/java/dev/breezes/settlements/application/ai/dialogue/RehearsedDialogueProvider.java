package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link DialogueProvider} implementation for {@link DialogueMode#REHEARSED}.
 * <p>
 * The evening sweep generates per-villager, occasion-keyed utterance packs by calling the
 * inference backend once per evening. Packs are stored here and sampled during the next day —
 * zero inference calls during normal gameplay.
 * <p>
 * The sweep runs within a configurable wall-clock budget ({@link DialogueConfig#packSweepDeadlineSeconds}).
 * Packs arrive progressively (one per villager as the backend streams) and are installed as they
 * arrive, so a slow villager never delays the rest. A sweep superseded by a new one (e.g.
 * the player advances the clock multiple times) is canceled at the transport level so the backend
 * can free GPU resources; any result that slips through the cancel race is discarded by an epoch
 * guard at the install site. Failure is always cosmetic — villagers without a pack fall back to the
 * injected scripted floor.
 * <p>
 * Thread safety: {@link #packs} is a {@link ConcurrentHashMap} because the streaming install
 * callback ({@link #installOnePack}) writes from the HTTP client's executor thread while the
 * server tick thread reads during daytime. Each villager's inner map is swapped atomically via
 * {@code ConcurrentHashMap.put} — the tick thread never sees a partially-populated occasion map.
 * Draw and depletion happen on the tick thread only.
 */
@CustomLog
public final class RehearsedDialogueProvider implements DialogueProvider {

    private final DialogueProvider fallback;
    private final MonologueRequestService monologueRequestService;

    /**
     * Outer key = villager UUID; inner key = occasion.
     * The entire inner map for a villager is replaced atomically on install;
     * individual occasion packs are only drawn on the tick thread.
     */
    private final ConcurrentHashMap<UUID, Map<Occasion, UtterancePack>> packs;

    /**
     * Monotonically increasing sweep counter. Incremented each time a new evening sweep starts;
     * each install callback captures the epoch value at sweep start and drops its result if the
     * epoch has since advanced (meaning a newer sweep has taken over).
     */
    private final AtomicLong sweepEpoch;

    /**
     * The currently-registered in-flight sweep handle. Replaced atomically when a new sweep
     * starts; the previous handle is canceled so the backend can free GPU resources.
     */
    private final AtomicReference<InferenceStreamHandle> inflightHandle;

    RehearsedDialogueProvider(DialogueProvider fallback, MonologueRequestService monologueRequestService) {
        this.fallback = fallback;
        this.monologueRequestService = monologueRequestService;
        this.packs = new ConcurrentHashMap<>();
        this.sweepEpoch = new AtomicLong(0L);
        this.inflightHandle = new AtomicReference<>(InferenceStreamHandle.noOp());
    }

    @Override
    public Optional<DialogueLine> sampleAmbientLine(@Nonnull UUID villagerUuid, @Nonnull DialogueContext context) {
        Map<Occasion, UtterancePack> villagerPacks = this.packs.get(villagerUuid);
        if (villagerPacks == null) {
            return this.fallback.sampleAmbientLine(villagerUuid, context);
        }

        UtterancePack pack = villagerPacks.get(context.getOccasion());
        if (pack == null || pack.isEmpty()) {
            // No pack for this occasion or it has been depleted — fall back gracefully.
            return this.fallback.sampleAmbientLine(villagerUuid, context);
        }

        return pack.drawLine().map(DialogueLine::literal)
                .or(() -> this.fallback.sampleAmbientLine(villagerUuid, context));
    }

    /**
     * Filters to villagers that need fresh packs, then dispatches the backend request off-thread.
     * The server tick thread returns immediately; packs install progressively as the stream arrives.
     * <p>
     * If a prior sweep is still in-flight (e.g. the player advanced the clock faster than SIS
     * responded), it is cancelled before the new one starts. The epoch guard in
     * {@link #installOnePack} discards any stale result that slips through the cancel race.
     */
    @Override
    public void runEveningPackSweep(Collection<BaseVillager> villagers) {
        List<BaseVillager> due = villagers.stream()
                .filter(v -> this.needsRefresh(v.getUUID()))
                .toList();

        if (due.isEmpty()) {
            log.debug("Evening sweep: all {} villager(s) already have full packs, skipping backend call", villagers.size());
            return;
        }

        // Advance the epoch first so the install callback captures the correct guard value.
        // Then cancel the previous handle before starting the new request.
        long epoch = this.rotateSweepHandle(InferenceStreamHandle.noOp());
        log.debug("Evening sweep: requesting packs for {} villager(s) (epoch {})", due.size(), epoch);

        InferenceStreamHandle handle = this.monologueRequestService.requestPacksStreaming(
                due, pack -> this.installOnePack(epoch, pack));
        this.inflightHandle.set(handle);

        handle.completion().thenRun(() -> {
            this.inflightHandle.compareAndSet(handle, InferenceStreamHandle.noOp());
            log.debug("Evening sweep epoch {} completed", epoch);
        });
    }

    /**
     * Installs a single villager's pack if the epoch still matches the current sweep.
     * <p>
     * An epoch mismatch means this result arrived from a superseded sweep; the pack is dropped to
     * ensure the newest generation always wins. Called from the HTTP client's executor thread.
     */
    void installOnePack(long epoch, VillagerPack pack) {
        // Discard results from superseded sweeps — newest epoch wins.
        if (epoch != this.sweepEpoch.get()) {
            return;
        }

        Map<Occasion, UtterancePack> occasionPacks = new HashMap<>();
        for (Map.Entry<Occasion, List<String>> entry : pack.getLinesByOccasion().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                occasionPacks.put(entry.getKey(), new UtterancePack(entry.getValue(), RandomUtil.RANDOM));
            }
        }

        if (!occasionPacks.isEmpty()) {
            this.packs.put(pack.getVillagerId(), occasionPacks);
        }
    }

    /**
     * Drops this villager's rehearsed packs when it leaves the loaded set
     */
    @Override
    public void evict(@Nonnull UUID villagerUuid) {
        this.packs.remove(villagerUuid);
        this.fallback.evict(villagerUuid);
    }

    /**
     * Returns true when a villager needs a fresh sweep.
     * <p>
     * A refresh is needed when the villager has no installed pack map at all, or when any
     * currently-installed occasion pack has been depleted. Villagers whose installed packs are
     * all still full are skipped to avoid wasting token budget when the village is idle.
     * <p>
     * This is deliberately a dumb query over whatever was installed — the provider knows nothing
     * about which occasions a villager <em>should</em> receive. That policy lives in
     * {@link OccasionSetResolver} on the service side, so a nitwit that legitimately has no WORK
     * pack is treated as full (not stale). A single depleted occasion forces a full re-request the
     * next evening, which self-heals any partially-returned set.
     */
    boolean needsRefresh(UUID villagerUuid) {
        Map<Occasion, UtterancePack> villagerPacks = this.packs.get(villagerUuid);
        if (villagerPacks == null) {
            return true;
        }

        for (UtterancePack pack : villagerPacks.values()) {
            if (pack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Advances the sweep epoch, atomically replaces the current in-flight handle with
     * {@code newHandle}, cancels the previous handle, and returns the new epoch value.
     *
     * @param newHandle the handle to register as the new in-flight sweep
     * @return the new monotonic epoch value captured for use in install callbacks
     */
    long rotateSweepHandle(InferenceStreamHandle newHandle) {
        long epoch = this.sweepEpoch.incrementAndGet();
        InferenceStreamHandle previous = this.inflightHandle.getAndSet(newHandle);

        // Cancel the previous handle so the backend can free GPU resources.
        previous.cancel();

        return epoch;
    }

    @Override
    public boolean isEnabled() {
        return this.fallback.isEnabled() || !this.packs.isEmpty();
    }

    @Override
    public boolean supportsRehearsedDialogSweep() {
        return true;
    }

}
