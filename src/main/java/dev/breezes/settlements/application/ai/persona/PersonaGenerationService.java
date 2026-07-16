package dev.breezes.settlements.application.ai.persona;

import dev.breezes.settlements.application.ai.inference.InferenceConfig;
import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.persona.PersonaBatchRequest;
import dev.breezes.settlements.application.ai.inference.persona.PersonaGateway;
import dev.breezes.settlements.application.ai.inference.persona.PersonaRequestAssembler;
import dev.breezes.settlements.application.ai.inference.persona.PersonaVillagerResult;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.personality.PersonalityStatus;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerPersonalityAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Coordinates the async persona-generation pipeline: sweeps loaded villagers for a {@code PENDING} persona,
 * batches and dispatches them to SIS, and marshals results back onto the server tick thread for installation.
 * <p>
 * This is birth-once generation over immutable inputs (genes, origin, and nitwit-ness never change
 * after spawn). Unlike RehearsedDialogueProvider's evening resweep, there is no reason to ever supersede
 * an in-flight request for a villager with a newer one -- so this class deliberately does NOT implement
 * an epoch/rotate-and-cancel scheme. Safety instead comes from three things: an in-flight set (never
 * re-send a villager whose request is outstanding), a drain-time PENDING re-check (SIS legitimately omits
 * some villagers from its response -- those simply stay {@code PENDING} and get re-swept later), and
 * cancellation reserved for server shutdown only (to free GPU resources, never to preempt a running batch).
 */
@ServerScope
@CustomLog
public final class PersonaGenerationService {

    private final PersonaRequestAssembler assembler;
    private final PersonaGateway gateway;
    private final PersonaConfig config;
    private final InferenceConfig inferenceConfig;

    /**
     * Villager UUIDs whose PERSONA request is currently outstanding. Membership is released only
     * when the whole batch completes (never per-result) -- releasing per-result would let a
     * villager SIS silently dropped from its response leak into this set forever, permanently
     * blocking it from ever being re-swept.
     */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    /**
     * Results that arrived from the gateway callback (HTTP executor thread) but are not yet
     * installed onto their villager's attachment (server tick thread).
     */
    private final ConcurrentLinkedQueue<PersonaVillagerResult> arrivals = new ConcurrentLinkedQueue<>();

    /**
     * Serializes dispatch to one in-flight batch at a time.
     * Newborns that miss the current batch simply wait for the next sweep once this clears.
     */
    private final AtomicBoolean batchInFlight = new AtomicBoolean(false);

    /**
     * Handle for the current in-flight batch, if any
     */
    private final AtomicReference<InferenceStreamHandle> currentHandle = new AtomicReference<>(InferenceStreamHandle.noOp());

    /**
     * Current exponential backoff duration in ticks; {@code 0} means "not backed off". Doubles
     * from the configured floor each time a batch returns zero results, capped at the configured
     * ceiling; resets to zero the moment any batch returns at least one result.
     */
    private final AtomicInteger currentBackoffTicks = new AtomicInteger(0);

    /**
     * Ticks remaining before the next sweep may dispatch a batch.
     */
    private final AtomicInteger cooldownRemainingTicks = new AtomicInteger(0);

    @Inject
    public PersonaGenerationService(@Nonnull PersonaRequestAssembler assembler,
                                    @Nonnull PersonaGateway gateway,
                                    @Nonnull PersonaConfig config,
                                    @Nonnull InferenceConfig inferenceConfig) {
        this.assembler = assembler;
        this.gateway = gateway;
        this.config = config;
        this.inferenceConfig = inferenceConfig;
    }

    /**
     * Scans the given loaded villagers for a {@code PENDING} persona and dispatches one batch if
     * eligible. Must be called on the server tick thread: every villager/gene/origin read for the
     * batch happens synchronously here, before the async gateway hand-off.
     * <p>
     * Early-outs (no endpoint configured, a batch already in flight, or still cooling down after a
     * zero-result batch) all leave every villager exactly as {@code PENDING} as before -- there is
     * nothing to undo.
     * <p>
     * The loaded-villager set is passed as a supplier so the whole-world entity scan is only paid for
     * once the early-outs pass — an endpoint-less or backed-off server never walks every level.
     */
    public void sweep(@Nonnull Supplier<? extends Collection<BaseVillager>> villagerSupplier) {
        if (!this.inferenceConfig.hasEndpoint() || this.batchInFlight.get() || this.isInBackoffCooldown()) {
            return;
        }

        Collection<BaseVillager> villagers = villagerSupplier.get();
        List<BaseVillager> due = new ArrayList<>();
        for (BaseVillager villager : villagers) {
            if (due.size() >= this.config.maxBatchSize()) {
                break;
            }
            if (this.inFlight.contains(villager.getUUID())) {
                continue;
            }
            if (VillagerPersonalityAttachment.read(villager).status() != PersonalityStatus.PENDING) {
                continue;
            }
            due.add(villager);
        }

        if (due.isEmpty()) {
            return;
        }

        this.dispatchBatch(due);
    }

    /**
     * Assembles and dispatches one batch. All villager reads (genes, origin, nitwit-ness) happen
     * here on the tick thread -- the assembled request carries plain data across the async
     * boundary, never the entity itself.
     */
    private void dispatchBatch(@Nonnull List<BaseVillager> due) {
        PersonaBatchRequest.PersonaBatchRequestBuilder batchBuilder = PersonaBatchRequest.builder();
        List<UUID> batchIds = new ArrayList<>(due.size());
        for (BaseVillager villager : due) {
            batchBuilder.villager(this.assembler.assemble(villager));
            batchIds.add(villager.getUUID());
        }
        PersonaBatchRequest batch = batchBuilder.build();

        this.inFlight.addAll(batchIds);
        this.batchInFlight.set(true);

        Duration deadline = Duration.ofSeconds(this.config.batchDeadlineSeconds());

        // Captured by the dispatch closure so the completion callback below can tell a genuinely
        // empty response apart from a full response with nothing left to install -- backoff keys
        // off this count, never off HTTP status.
        AtomicInteger resultCount = new AtomicInteger(0);
        InferenceStreamHandle handle = this.gateway.generate(batch, deadline, result -> {
            resultCount.incrementAndGet();
            this.arrivals.offer(result);
        });
        this.currentHandle.set(handle);

        log.debug("Persona batch dispatched: {} villager(s)", batchIds.size());
        handle.completion().thenRun(() -> this.onBatchCompleted(batchIds, resultCount.get()));
    }

    /**
     * Runs on the gateway's executor thread once the batch stream ends by any means. Membership is
     * released for the whole batch at once -- see the {@link #inFlight} field doc for why.
     */
    private void onBatchCompleted(@Nonnull List<UUID> batchIds, int resultCount) {
        batchIds.forEach(this.inFlight::remove);
        this.batchInFlight.set(false);

        if (resultCount > 0) {
            this.currentBackoffTicks.set(0);
            this.cooldownRemainingTicks.set(0);
            return;
        }

        // Handle zero results
        int previous = this.currentBackoffTicks.get();
        int next = previous <= 0
                ? this.config.backoffFloorTicks()
                : Math.min(previous * 2, this.config.backoffCeilingTicks());
        this.currentBackoffTicks.set(next);
        this.cooldownRemainingTicks.set(next);
        log.warn("Persona batch returned zero results for {} villager(s); backing off {} ticks", batchIds.size(), next);
    }

    /**
     * Decrements the cooldown by one sweep interval and reports whether the sweep should still stand down
     */
    private boolean isInBackoffCooldown() {
        int remaining = this.cooldownRemainingTicks.get();
        if (remaining <= 0) {
            return false;
        }
        int updated = Math.max(0, remaining - this.config.sweepIntervalTicks());
        this.cooldownRemainingTicks.set(updated);
        return updated > 0;
    }

    /**
     * Installs every arrived result whose villager is still loaded and still {@code PENDING}.
     * Driven on a fast cadence decoupled from the sweep interval (see PersonaSweepServerEvents) so
     * results install within about a second of arrival, regardless of how long the batch took.
     */
    public void drain(@Nonnull MinecraftServer server) {
        PersonaVillagerResult result;
        while ((result = this.arrivals.poll()) != null) {
            this.install(server, result);
        }
    }

    /**
     * A BaseVillager reference is never held across the async boundary -- the queue carries
     * only the UUID-keyed result, so the entity is always resolved fresh here on the tick thread.
     */
    private void install(@Nonnull MinecraftServer server, @Nonnull PersonaVillagerResult result) {
        BaseVillager villager = resolveVillager(server, result.getVillagerId());
        if (villager == null) {
            return;
        }

        // The villager may have changed state since dispatch (e.g. a duplicate/earlier result for
        // the same villager already installed) -- only a still-PENDING villager accepts the write.
        if (VillagerPersonalityAttachment.read(villager).status() != PersonalityStatus.PENDING) {
            return;
        }

        VillagerPersonality ready = VillagerPersonality.ready(result.getAdjectives(), result.getCharacterSketch(), result.getSpeechStyle());
        VillagerPersonalityAttachment.write(villager, ready);
    }

    @Nullable
    private static BaseVillager resolveVillager(@Nonnull MinecraftServer server, @Nonnull UUID villagerId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(villagerId);
            if (entity instanceof BaseVillager villager && villager.isAlive() && !villager.isRemoved()) {
                return villager;
            }
        }
        return null;
    }

    /**
     * Cancels the in-flight batch (if any) so the backend can free GPU resources, and clears all transient state
     */
    public void shutdown() {
        this.currentHandle.getAndSet(InferenceStreamHandle.noOp()).cancel();
        this.arrivals.clear();
        this.inFlight.clear();
        this.batchInFlight.set(false);
    }

}
