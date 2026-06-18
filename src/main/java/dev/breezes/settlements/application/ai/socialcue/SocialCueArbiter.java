package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.planning.PlanRuntimeState;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.eventlane.EventLaneConfig;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tick-level controller for the SocialCue lane.
 * <p>
 * On each call to {@link #tick} the arbiter:
 * <ol>
 *   <li>Advances the active cue's script (dispatching any steps whose start-tick has arrived).</li>
 *   <li>If no cue is active, scans the catalog to find and admit the first eligible cue. This
 *       admission scan is throttled to ~1 Hz; active-cue dispatch still runs every tick.</li>
 * </ol>
 * <p>
 * Admission rule: a cue's channel set must be disjoint from the currently running
 * behavior's required channels. A {@code null} behavior descriptor means no behavior
 * is running — all channels are free.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SocialCueArbiter {

    /**
     * Cadence for the catalog admission scan. Each scan walks every cue entry and evaluates its
     * trigger — several triggers scan perceived entities and the knowledge store — so running it
     * every tick while idle is pure overhead. Cue cooldowns are all stored as absolute game-ticks,
     * so throttling the scan only delays <em>noticing</em> a trigger; it never drifts cue timing.
     */
    private static final ClockTicks ADMISSION_SCAN_INTERVAL = ClockTicks.seconds(1);

    /**
     * First-scan spread: each villager delays its first admission scan by a random offset in
     * [0, this) so a freshly-loaded crowd does not all act on the same tick.
     */
    private static final ClockTicks INITIAL_ADMISSION_SPREAD = ClockTicks.seconds(4);

    private final Set<SocialCueCatalogEntry> catalog;
    private final SocialCuePresenter presenter;
    private final EventLaneConfig eventLaneConfig;

    /**
     * Should be called from AI step
     */
    public void tick(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        // Suppress the entire social cue lane when the villager is unavailable (e.g. sleeping).
        // Cancelling any in-flight cue here prevents a villager that falls asleep mid-cue from
        // freezing with a stale gaze or gesture locked until the cue's natural finish tick.
        if (villager.isSociallyUnavailable()) {
            if (runtimeState.isCueActive()) {
                runtimeState.cancelActiveCue();
            }
            return;
        }

        // Active-cue dispatch must run every tick so gaze/gesture/bubble steps stay smooth.
        if (runtimeState.isCueActive()) {
            tickActiveCue(villager, runtimeState, gameTime);
            return;
        }

        // First idle visit: establish a per-villager random scan phase so a freshly-loaded crowd
        // does not all scan — and therefore act — on the same tick.
        if (!runtimeState.isAdmissionScanInitialized()) {
            runtimeState.markAdmissionScanInitialized();
            long phase = SocialCueCadencePolicy.initialScanPhaseTicks(
                    INITIAL_ADMISSION_SPREAD.getTicks(), villager.getRandom().nextDouble());
            runtimeState.scheduleNextAdmissionScan(gameTime + phase);
            return;
        }

        // Idle: throttle the catalog admission scan to ADMISSION_SCAN_INTERVAL.
        if (gameTime < runtimeState.getNextAdmissionScanTick()) {
            return;
        }
        runtimeState.scheduleNextAdmissionScan(gameTime + ADMISSION_SCAN_INTERVAL.getTicks());
        tryAdmit(villager, runtimeState, gameTime);
    }

    private void tickActiveCue(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        SocialCue cue = runtimeState.getActiveCue();
        SocialCueScript script = cue.getScript();

        // Dispatch all steps whose scheduled start-tick has arrived this tick.
        while (runtimeState.getNextStepIndex() < script.stepCount()) {
            int index = runtimeState.getNextStepIndex();
            if (!SocialCueTimingPolicy.isStepDue(script, index, runtimeState.getCueStartGameTime(), gameTime)) {
                // Still waiting for this step — nothing to do this tick.
                break;
            }

            CueStep step = script.stepAt(index);
            this.presenter.dispatch(step, villager, runtimeState);
            runtimeState.advance();
        }

        // Finish only once every step has been dispatched AND the full script duration has elapsed
        if (SocialCueTimingPolicy.isReadyToFinish(runtimeState.getNextStepIndex(), script.stepCount(),
                runtimeState.getCueStartGameTime(), script.getTotalDuration().getTicks(), gameTime)) {
            SocialCueCatalogEntry entry = runtimeState.getActiveCueEntry();
            if (entry != null) {
                entry.fireOnComplete(villager, cue.getContextKey());
            }

            long cooldownTicks = SocialCueCadencePolicy.cooldownTicks(cue.getCooldown().getTicks(),
                    villager.getGenetics().getGeneValue(GeneType.CHARISMA),
                    this.eventLaneConfig.socialCueLowCharismaCooldownMultiplier(),
                    this.eventLaneConfig.socialCueHighCharismaCooldownMultiplier(),
                    SocialCueCooldownScaling.fromConfig(this.eventLaneConfig.socialCueCharismaCooldownScaling()),
                    this.eventLaneConfig.socialCueCooldownJitterFraction(),
                    villager.getRandom().nextDouble());
            runtimeState.finish(gameTime, cooldownTicks);
        }
    }

    private void tryAdmit(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        Set<BehaviorChannel> occupiedChannels = occupiedChannels(villager);

        for (SocialCueCatalogEntry entry : this.catalog) {
            // Skip if this cue key is on per-key cooldown.
            if (runtimeState.isCueOnCooldown(entry.getKey(), gameTime)) {
                continue;
            }

            // Channel conflict: cue cannot run while an incompatible behavior is active.
            if (!Collections.disjoint(entry.getChannels(), occupiedChannels)) {
                continue;
            }

            // Check the trigger: did the catalog entry find something to react to?
            Optional<String> contextKey = entry.getTrigger().apply(villager);
            if (contextKey.isEmpty()) {
                continue;
            }

            // Per-target cooldown: do not greet the same entity on every cycle.
            if (runtimeState.isTargetOnCooldown(uuidFromKey(contextKey.get()), gameTime)) {
                continue;
            }

            // Build the cue — invoking the script factory exactly once — then validate the script
            // duration against the hard cap. Building before the check keeps the factory single-
            // invocation, which matters for factories with side effects (the dialogue chatter cue
            // draws exactly one utterance line per build).
            String resolvedContextKey = contextKey.get();
            SocialCue cue = entry.buildCue(villager, resolvedContextKey);
            if (cue.getScript().getTotalDuration().getTicks() > SocialCueScript.MAX_DURATION.getTicks()) {
                log.warn("SocialCue '{}' script exceeds max duration ({} ticks) — skipping",
                        entry.getKey(), cue.getScript().getTotalDuration().getTicks());
                continue;
            }

            runtimeState.start(cue, entry, gameTime);

            // Fire onAdmit exactly once, after start() — all bail-out checks are behind us.
            // Registry mutations (e.g. sendInvite) live in onAdmit so they never run on a
            // candidate that the per-target cooldown or duration cap would have rejected.
            entry.fireOnAdmit(villager, resolvedContextKey);

            // Record per-target cooldown so the same target is not immediately re-greeted.
            runtimeState.recordTargetGreeted(uuidFromKey(resolvedContextKey), gameTime,
                    entry.getPerTargetCooldown().getTicks());

            log.debug("Admitted SocialCue '{}' for villager {}", cue.getKey(), villager.getUUID());
            return;
        }
    }

    /**
     * Returns the channels locked by the currently running behavior, or an empty set
     * when no behavior is active (all channels free).
     */
    private Set<BehaviorChannel> occupiedChannels(BaseVillager villager) {
        PlanRuntimeState planState = villager.getPlanRuntimeState();

        BehaviorPlanningMetadata descriptor = planState.getCurrentDescriptor();
        if (descriptor == null) {
            return Collections.emptySet();
        }

        return descriptor.getRequiredChannels();
    }

    /**
     * The context key for player/entity triggers is the UUID as a string.
     * This converts it back for the per-target cooldown map.
     */
    private UUID uuidFromKey(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            // Non-UUID keys (e.g. future string-keyed triggers) get a deterministic UUID.
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
    }

}
