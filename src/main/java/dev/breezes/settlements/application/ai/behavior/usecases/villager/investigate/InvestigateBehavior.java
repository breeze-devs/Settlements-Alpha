package dev.breezes.settlements.application.ai.behavior.usecases.villager.investigate;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetableLocation;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.planning.InvestigateTipSelector;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.behavior.contracts.ConfirmableOverride;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.credibility.ClaimPredicate;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeEntry;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.ReputationUtil;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Investigates a hearsay tip by navigating to the claimed location and evaluating the
 * machine-checkable {@link ClaimPredicate} on arrival.
 * <p>
 * Contract:
 * <ul>
 *   <li>Navigate within the sensor radius of the tip position.</li>
 *   <li>Force one explicit sensor scan on arrival so normal sensor cadence does not
 *       make the postcondition ambiguous — the scan happens deterministically on the
 *       same tick as arrival rather than waiting for the next natural cadence.</li>
 *   <li>Evaluate the claim predicate and resolve the hearsay entry to CONFIRMED or REFUTED.</li>
 *   <li>Update credibility for the source villager via {@link ReputationUtil}.</li>
 *   <li>On CONFIRMED: the forced scan has already populated memories, so normal planning
 *       takes over and relevant behaviors' preconditions will pass on the next tick.</li>
 *   <li>On REFUTED: a shrug — charming, not a bug. Credibility dips slightly for the source.</li>
 * </ul>
 * <p>
 * The behavior is configured per-instance via {@link #configure(KnowledgeEntry, ClaimPredicate)}
 * before the planner or override lane calls {@code start()}.
 */
@CustomLog
public final class InvestigateBehavior extends VillagerStateMachineBehavior implements ConfirmableOverride {

    /**
     * How close the villager must be to the tip location before the scan fires.
     * Chosen to match a typical block-resource sensor radius so the predicate
     * evaluates the same area that sensors would normally cover.
     */
    private static final double ARRIVAL_DISTANCE = 8.0;
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(30).getTicksAsInt();

    /**
     * Cooldown applied after a nav-timeout: ~90 seconds at 20 tps.
     * Long enough to let the world change (another villager may clear the path)
     * without completely abandoning the tip.
     */
    private static final long TIMEOUT_RETRY_COOLDOWN_TICKS = ClockTicks.seconds(90).getTicks();

    /**
     * Maximum nav-timeout attempts before the tip is soft-refuted and removed from the
     * pending pool. Three attempts over ~4.5 minutes is a generous window before giving up.
     */
    private static final int ATTEMPT_CAP = 3;

    private enum Stage implements StageKey {
        NAVIGATE, SCAN_AND_EVALUATE, HANDLE_TIMEOUT, END
    }

    private final InvestigateConfig config;
    private final ReputationUtil reputationUtil;
    private final ReputationQuery reputationQuery;

    /**
     * The hearsay tip being investigated. Injected per-instance before {@code start()}.
     */
    @Nullable
    private KnowledgeEntry tipEntry;
    /**
     * The machine-checkable predicate that determines CONFIRMED vs REFUTED. Injected alongside {@link #tipEntry}.
     */
    @Nullable
    private ClaimPredicate claimPredicate;

    /**
     * Resolution reached on the most recent scan, or {@code null} if the behavior ended without
     * evaluating (e.g. navigation timed out before arrival). Read by the override lane to decide
     * whether a confirmed tip warrants an onConfirm follow-up (plan regeneration).
     */
    @Nullable
    private KnowledgeResolution lastResolution;

    public InvestigateBehavior(@Nonnull InvestigateConfig config,
                               @Nonnull HungerConfig hungerConfig,
                               @Nonnull ReputationUtil reputationUtil,
                               @Nonnull ReputationQuery reputationQuery) {
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                hungerConfig,
                config.experienceReward());
        this.config = config;
        this.reputationUtil = reputationUtil;
        this.reputationQuery = reputationQuery;

        // Self-bind: on each precondition check, select the best eligible tip from the villager's
        // store. This removes the need for PlanRunner to reach in via instanceof and configure()
        // before the precondition check fires. An absent tip (all resolved or all on cooldown)
        // fails the precondition cleanly, which causes the plan slot to be skipped.
        this.preconditions.add(ICondition.named("HasTipToInvestigate", villager -> {
            long nowTick = villager.level().getGameTime();
            KnowledgeEntry selectedTip = InvestigateTipSelector.select(villager.getKnowledgeStore(), villager.getUUID(), this.reputationQuery, nowTick);
            if (selectedTip == null) {
                return false;
            }

            this.tipEntry = selectedTip;
            this.claimPredicate = DefaultClaimPredicates.forObservationType(selectedTip.getType());
            this.lastResolution = null;
            return true;
        }));

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    /**
     * Returns the resolution reached on the most recent scan, or {@code null} if the behavior
     * ended without evaluating (navigation timeout). The override lane uses this to trigger an
     * onConfirm follow-up only when a tip was actually CONFIRMED.
     */
    @Nullable
    public KnowledgeResolution getLastResolution() {
        return this.lastResolution;
    }

    /**
     * {@inheritDoc}
     * Returns {@code true} only when investigation reached the site and confirmed the claim.
     * Navigation timeouts and refutations both return {@code false} — only confirmed discoveries
     * justify regenerating the day plan.
     */
    @Override
    public boolean didConfirm() {
        return this.lastResolution == KnowledgeResolution.CONFIRMED;
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.NAVIGATE, this.createNavigateStep());
        stageMap.put(Stage.SCAN_AND_EVALUATE, this.createScanAndEvaluateStep());
        stageMap.put(Stage.HANDLE_TIMEOUT, this.createHandleTimeoutStep());

        return StagedStep.<BaseVillager>builder()
                .name("InvestigateBehavior")
                .initialStage(Stage.NAVIGATE)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createNavigateStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(ARRIVAL_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, (int) ARRIVAL_DISTANCE))
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("InvestigateArrived")
                        .action(ctx -> StepResult.transition(Stage.SCAN_AND_EVALUATE))
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                // On timeout the site is unreachable: soft-refute after ATTEMPT_CAP retries
                // to break the thrash loop where the same unreachable tip is re-selected every day.
                .timeoutTransition(Stage.HANDLE_TIMEOUT)
                .build();
    }

    /**
     * Handles a navigation timeout: increments the attempt counter and, once the cap is reached,
     * soft-refutes the tip so it exits the pending pool permanently.
     * <p>
     * Below the cap, a cooldown is imposed via {@link KnowledgeEntry#recordNavigationTimeout}
     * so the same entry is not re-selected on every subsequent plan cycle while the location
     * remains unreachable. Above the cap we treat the tip like a confirmed-false: the source's
     * credibility dips (mildly, per refutation asymmetry) and the entry is resolved REFUTED.
     */
    private BehaviorStep<BaseVillager> createHandleTimeoutStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("InvestigateHandleTimeout")
                .action(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    KnowledgeEntry entry = this.tipEntry;
                    if (entry == null) {
                        return StepResult.transition(Stage.END);
                    }

                    long nowTick = villager.level().getGameTime();
                    entry.recordNavigationTimeout(nowTick, TIMEOUT_RETRY_COOLDOWN_TICKS);

                    if (entry.getInvestigationAttempts() >= ATTEMPT_CAP) {
                        // Treat persistent nav failure as evidence the tip is stale or the location is permanently unreachable.
                        villager.getKnowledgeStore()
                                .findByOriginId(entry.getOriginObservationId())
                                .ifPresent(stored -> stored.resolve(KnowledgeResolution.REFUTED));

                        if (entry.getSource() != null) {
                            this.reputationUtil.recordResolution(
                                    villager.getUUID(), entry.getSource(),
                                    KnowledgeResolution.REFUTED,
                                    nowTick, entry.getOriginTimestampTick());
                        }

                        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TIP_REFUTED, null);
                        outcome.recordSocialOutcome(null, null, EventOutcome.SUCCESS, null, null);
                        ctx.setState(BehaviorStateType.BEHAVIOR_OUTCOME, outcome);
                        log.behaviorStatus("InvestigateBehavior: soft-refuted tip {} after {} nav timeouts for villager {}",
                                entry.getOriginObservationId(), entry.getInvestigationAttempts(), villager.getUUID());
                    } else {
                        log.behaviorTrace("InvestigateBehavior: nav timeout #{} for tip {} on villager {}, cooldown set",
                                entry.getInvestigationAttempts(), entry.getOriginObservationId(), villager.getUUID());
                    }

                    return StepResult.transition(Stage.END);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createScanAndEvaluateStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("InvestigateScanAndEvaluate")
                .action(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    Level world = villager.level();
                    villager.getSettlementsBrain().forceSensorScan(world);

                    KnowledgeEntry entry = this.tipEntry;
                    ClaimPredicate predicate = this.claimPredicate;
                    if (entry == null || predicate == null) {
                        log.behaviorWarn("InvestigateBehavior: tip or predicate missing for villager {} at scan step", villager.getUUID());
                        return StepResult.transition(Stage.END);
                    }

                    Vec3 tipPos = extractTipPosition(entry);
                    boolean confirmed = predicate.test((ServerLevel) world, tipPos);
                    KnowledgeResolution resolution = confirmed ? KnowledgeResolution.CONFIRMED : KnowledgeResolution.REFUTED;
                    this.lastResolution = resolution;

                    villager.getKnowledgeStore()
                            .findByOriginId(entry.getOriginObservationId())
                            .ifPresent(stored -> stored.resolve(resolution));

                    // Update credibility only for hearsay entries
                    if (entry.getSource() != null) {
                        this.reputationUtil.recordResolution(villager.getUUID(), entry.getSource(), resolution,
                                world.getGameTime(), entry.getOriginTimestampTick());
                    }

                    WorldEventType deedType = confirmed ? WorldEventType.TIP_CONFIRMED : WorldEventType.TIP_REFUTED;
                    BehaviorOutcome outcome = BehaviorOutcome.forDeed(deedType, null);
                    outcome.recordSocialOutcome(null, null, EventOutcome.SUCCESS, null, null);
                    ctx.setState(BehaviorStateType.BEHAVIOR_OUTCOME, outcome);

                    log.behaviorStatus("Investigate '{}' for villager {}: origin={} source={}",
                            resolution, villager.getUUID(), entry.getOriginObservationId(), entry.getSource());

                    return StepResult.transition(Stage.END);
                })
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.setState(BehaviorStateType.BEHAVIOR_OUTCOME, BehaviorOutcome.blank());
        // Place the tip position as the navigation target so StayCloseStep has something to navigate toward.
        if (this.tipEntry != null) {
            Vec3 tipPos = extractTipPosition(this.tipEntry);
            Location tipLocation = Location.of(tipPos.x, tipPos.y, tipPos.z, world);
            context.setState(BehaviorStateType.TARGET, TargetState.of(new TargetableLocation(tipLocation)));
        }
    }

    /**
     * Reads the world position from the knowledge entry's metadata map.
     * Position keys ("pos_x", "pos_y", "pos_z") are written by the observation factory
     * when the world event is first admitted in Phase 4. Falls back to Vec3.ZERO when
     * the keys are missing — degenerate case that should not occur in production but
     * avoids an NPE so the behavior fails gracefully rather than crashing.
     */
    private static Vec3 extractTipPosition(@Nonnull KnowledgeEntry entry) {
        try {
            Map<String, String> meta = entry.getMetadata();
            double x = Double.parseDouble(meta.getOrDefault("pos_x", "0"));
            double y = Double.parseDouble(meta.getOrDefault("pos_y", "64"));
            double z = Double.parseDouble(meta.getOrDefault("pos_z", "0"));
            return new Vec3(x, y, z);
        } catch (NumberFormatException e) {
            log.behaviorWarn("InvestigateBehavior: malformed position metadata in entry {}: {}",
                    entry.getOriginObservationId(), e.getMessage());
            return Vec3.ZERO;
        }
    }

}
