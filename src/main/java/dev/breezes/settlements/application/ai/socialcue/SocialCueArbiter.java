package dev.breezes.settlements.application.ai.socialcue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tick-level controller for the SocialCue lane.
 * Admits and advances ambient social cues, with at most one active cue per villager.
 * <p>
 * Channel conflicts prevent admission; occupied channels are not rechecked during cue execution.
 */
@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SocialCueArbiter {

    /**
     * Limits trigger polling overhead while the lane is idle.
     */
    private static final ClockTicks ADMISSION_SCAN_INTERVAL = ClockTicks.seconds(1);

    /**
     * First-scan spread: each villager delays its first admission scan by a random offset in
     * [0, this) so a freshly-loaded crowd does not all act on the same tick.
     */
    private static final ClockTicks INITIAL_ADMISSION_SPREAD = ClockTicks.seconds(4);

    /**
     * Minimum gap enforced between spontaneous bubbles on the same villager, regardless of which cue key fires.
     */
    private static final ClockTicks LANE_REFRACTORY = ClockTicks.seconds(12);

    private final Set<SocialCueCatalogEntry> catalog;
    private final SocialCuePresenter presenter;
    private final SocialCueConfig socialCueConfig;

    /**
     * Advances an active cue or attempts admission when an idle scan is due.
     * <p>
     * Social unavailability cancels the active cue without invoking its completion callback.
     */
    public void tick(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        // Suppress the entire social cue lane when the villager is unavailable (e.g. sleeping)
        if (!villager.isSociallyAvailable()) {
            if (runtimeState.isCueActive()) {
                // Cancel cue to prevent stale gestures
                runtimeState.cancelActiveCue();
            }
            return;
        }

        // Active-cue dispatch must run every tick so gaze/gesture/bubble steps stay smooth
        if (runtimeState.isCueActive()) {
            tickActiveCue(villager, runtimeState, gameTime);
            return;
        }

        if (!runtimeState.isAdmissionScanInitialized()) {
            runtimeState.markAdmissionScanInitialized();
            long phase = SocialCueCadencePolicy.initialScanPhaseTicks(
                    INITIAL_ADMISSION_SPREAD.getTicks(), villager.getRandom().nextDouble());
            runtimeState.scheduleNextAdmissionScan(gameTime + phase);
            return;
        }

        if (gameTime < runtimeState.getNextAdmissionScanTick()) {
            return;
        }
        runtimeState.scheduleNextAdmissionScan(gameTime + ADMISSION_SCAN_INTERVAL.getTicks());
        tryAdmit(villager, runtimeState, gameTime);
    }

    private void tickActiveCue(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        SocialCue cue = runtimeState.getActiveCue();
        SocialCueScript script = cue.getScript();

        // Multiple steps can share a start time; dispatching only one per tick would stretch the script.
        while (runtimeState.getNextStepIndex() < script.stepCount()) {
            int index = runtimeState.getNextStepIndex();
            if (!SocialCueTimingPolicy.isStepDue(script, index, runtimeState.getCueStartGameTime(), gameTime)) {
                break;
            }

            CueStep step = script.stepAt(index);
            this.presenter.dispatch(step, villager, runtimeState);
            runtimeState.advance();
        }

        // The final wait can outlast the last dispatch; do not finish the cue early.
        if (SocialCueTimingPolicy.isReadyToFinish(runtimeState.getNextStepIndex(), script.stepCount(),
                runtimeState.getCueStartGameTime(), script.getTotalDuration().getTicks(), gameTime)) {
            SocialCueCatalogEntry entry = runtimeState.getActiveCueEntry();
            if (entry != null) {
                entry.fireOnComplete(villager, cue.getContextKey());
            }

            long cooldownTicks = SocialCueCadencePolicy.cooldownTicks(cue.getCooldown().getTicks(),
                    villager.getGenetics().getGeneValue(GeneType.CHARISMA),
                    this.socialCueConfig.socialCueLowCharismaCooldownMultiplier(),
                    this.socialCueConfig.socialCueHighCharismaCooldownMultiplier(),
                    SocialCueCooldownScaling.fromConfig(this.socialCueConfig.socialCueCharismaCooldownScaling()),
                    OccasionCadenceProfile.factorFor(resolveCooldownOccasion(villager)),
                    this.socialCueConfig.socialCueCooldownJitterFraction(),
                    villager.getRandom().nextDouble());
            runtimeState.finish(gameTime, cooldownTicks);
            runtimeState.markLaneQuietUntil(gameTime + LANE_REFRACTORY.getTicks());
        }
    }

    /**
     * Attempts to admit the first eligible cue in catalog iteration order.
     */
    private void tryAdmit(BaseVillager villager, SocialCueRuntimeState runtimeState, long gameTime) {
        Set<BehaviorChannel> occupiedChannels = villager.occupiedChannels();

        for (SocialCueCatalogEntry entry : this.catalog) {
            // Skip if this cue key is on per-key cooldown
            if (runtimeState.isCueOnCooldown(entry.getKey(), gameTime)) {
                continue;
            }

            // Allow reactive replies through the lane's quiet period
            if (!entry.isBypassLaneRefractory() && runtimeState.isLaneQuiet(gameTime)) {
                continue;
            }

            // Channel conflict: cue cannot run while an incompatible behavior is active
            if (!Collections.disjoint(entry.getChannels(), occupiedChannels)) {
                continue;
            }

            // Check the trigger: did the catalog entry find something to react to?
            Optional<String> contextKey = entry.getTrigger().apply(villager);
            if (contextKey.isEmpty()) {
                continue;
            }

            // Per-target cooldown: do not interact with the same entity on every cycle
            if (runtimeState.isTargetOnCooldown(uuidFromKey(contextKey.get()), gameTime)) {
                continue;
            }

            // A declined roll consumes the cue's cooldown so repeated scans do not defeat its fire chance
            if (entry.getFireChance() < 1.0 && villager.getRandom().nextDouble() >= entry.getFireChance()) {
                runtimeState.recordCueDeclined(entry.getKey(), gameTime, entry.getCooldown().getTicks());
                continue;
            }

            // Reuse the built cue for validation and admission: rebuilding could repeat factory side effects
            String resolvedContextKey = contextKey.get();
            SocialCue cue = entry.buildCue(villager, resolvedContextKey);
            if (cue.getScript().getTotalDuration().getTicks() > SocialCueScript.MAX_DURATION.getTicks()) {
                log.warn("SocialCue '{}' script exceeds max duration ({} ticks) — skipping",
                        entry.getKey(), cue.getScript().getTotalDuration().getTicks());
                continue;
            }

            runtimeState.start(cue, entry, gameTime);

            // Run admission side effects only after every rejection check has passed and the cue is active
            entry.fireOnAdmit(villager, resolvedContextKey);

            runtimeState.recordTargetGreeted(uuidFromKey(resolvedContextKey), gameTime,
                    entry.getPerTargetCooldown().getTicks());

            return;
        }
    }

    private Occasion resolveCooldownOccasion(BaseVillager villager) {
        DayPlan dayPlan = villager.getDayPlan();
        if (dayPlan != null && dayPlan.getDayType() == PlanDayType.REST_DAY) {
            return Occasion.REST_DAY;
        }

        return villager.getCurrentOccasion();
    }

    /**
     * Preserves entity UUIDs and gives other context keys a stable cooldown identity.
     */
    private UUID uuidFromKey(String key) {
        try {
            return UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
    }

}
