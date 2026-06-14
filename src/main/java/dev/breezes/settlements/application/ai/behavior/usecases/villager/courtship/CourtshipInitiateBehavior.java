package dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.ReleaseHomePoiObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.TemporaryArtifactHandle;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.courtship.BedReservationService;
import dev.breezes.settlements.application.ai.courtship.ChoreographyTimeline;
import dev.breezes.settlements.application.ai.courtship.CourtshipChoreographyLibrary;
import dev.breezes.settlements.application.ai.courtship.CourtshipCloseReason;
import dev.breezes.settlements.application.ai.courtship.CourtshipConstants;
import dev.breezes.settlements.application.ai.courtship.CourtshipPhase;
import dev.breezes.settlements.application.ai.courtship.CourtshipSession;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.memory.MemoryModuleTypeRegistry;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public final class CourtshipInitiateBehavior extends VillagerStateMachineBehavior {

    public enum Stage implements StageKey {
        SCAN_AND_INVITE,
        WAIT_FOR_ACCEPT,
        APPROACH,
        COURTSHIP,
        BIRTH,
        CLOSED,
    }

    private final CourtshipInitiateConfig config;
    private final CourtshipSessionRegistry sessionRegistry;
    private final BedReservationService bedReservationService;
    private final CourtshipPresenter courtshipPresenter;
    private final CourtshipChoreographyLibrary choreographyLibrary;

    @Nullable
    private UUID activeSessionId;

    public CourtshipInitiateBehavior(@Nonnull CourtshipInitiateConfig config,
                                     @Nonnull HungerConfig hungerConfig,
                                     @Nonnull CourtshipSessionRegistry sessionRegistry,
                                     @Nonnull BedReservationService bedReservationService,
                                     @Nonnull CourtshipPresenter courtshipPresenter,
                                     @Nonnull CourtshipChoreographyLibrary choreographyLibrary) {
        super(log,
                config.createPreconditionCheckCooldownTickable(),
                config.createBehaviorCooldownTickable(),
                hungerConfig);
        this.config = config;
        this.sessionRegistry = sessionRegistry;
        this.bedReservationService = bedReservationService;
        this.courtshipPresenter = courtshipPresenter;
        this.choreographyLibrary = choreographyLibrary;

        this.preconditions.add(this.canInitiateCourtship());
        this.initializeStateMachine(this.createControlStep(), Stage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.activeSessionId = null;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();

        if (this.activeSessionId != null) {
            courtshipPresenter.presentEnd(villager, this.activeSessionId, world.getGameTime());

            this.sessionRegistry.getActiveSession(villager.getUUID()).ifPresent(session -> {
                CourtshipPhase phase = session.getPhase();
                if (phase != CourtshipPhase.COMPLETED && phase != CourtshipPhase.ABORTED) {
                    this.sessionRegistry.closeSession(session.getSessionId(), CourtshipCloseReason.EXTERNAL_CANCEL);
                }
            });
        }

        this.activeSessionId = null;
    }

    private ICondition<BaseVillager> canInitiateCourtship() {
        return ICondition.named("can initiate courtship", villager -> selectCandidate(villager).isPresent());
    }

    /**
     * Extracts the partner selection stream so the precondition and scanAndInvite both use
     * the same filtering logic without duplicating the filter chain.
     */
    private Optional<UUID> selectCandidate(@Nonnull BaseVillager villager) {
        if (this.sessionRegistry.getActiveSession(villager.getUUID()).isPresent()) {
            return Optional.empty();
        }
        if (!villager.canBreed()) {
            return Optional.empty();
        }

        List<UUID> candidates = villager.getBrain()
                .getMemory(MemoryModuleTypeRegistry.WILLING_COURTSHIP_PARTNERS.get())
                .orElse(List.of());

        return candidates.stream()
                // Strict < 0 matches CourtshipRole.of so only the lower-UUID side initiates.
                .filter(candidateId -> villager.getUUID().compareTo(candidateId) < 0)
                .filter(candidateId -> this.sessionRegistry.getActiveSession(candidateId).isEmpty())
                .filter(candidateId -> !this.sessionRegistry.hasInviteFor(candidateId))
                .filter(candidateId -> {
                    // Re-check canBreed on the live entity since the sensor snapshot may be stale.
                    if (!(villager.level() instanceof ServerLevel serverLevel)) {
                        return false;
                    }
                    BaseVillager candidate = resolveParticipant(serverLevel, candidateId).orElse(null);
                    return candidate != null && candidate.canBreed();
                })
                .findFirst();
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("CourtshipInitiateBehavior")
                .initialStage(Stage.SCAN_AND_INVITE)
                .stageStepMap(Map.of(
                        Stage.SCAN_AND_INVITE, this::scanAndInvite,
                        Stage.WAIT_FOR_ACCEPT, this::waitForAccept,
                        Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(CourtshipConstants.CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                .actionStep(this::onWithinApproachRange)
                                .build(),
                        Stage.COURTSHIP, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(CourtshipConstants.DRIFT_DISTANCE)
                                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                .actionStep(this::tickCourtship)
                                .build(),
                        Stage.BIRTH, this::birth
                ))
                .nextStage(Stage.CLOSED)
                .build();
    }

    private StepResult scanAndInvite(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return StepResult.fail("not-server-level");
        }

        UUID partnerId = selectCandidate(self).orElse(null);
        if (partnerId == null) {
            return StepResult.complete();
        }

        BaseVillager partner = resolveParticipant(serverLevel, partnerId).orElse(null);
        if (partner == null) {
            return StepResult.complete();
        }

        // Duration will be overwritten by the timeline when courtship begins; use a placeholder.
        int placeholderDuration = 300;
        CourtshipSession session = this.sessionRegistry.sendInvite(self, partner, placeholderDuration, self.level().getGameTime());
        this.activeSessionId = session.getSessionId();

        return StepResult.transition(Stage.WAIT_FOR_ACCEPT);
    }

    private StepResult waitForAccept(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        CourtshipSession session = currentSession(self).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        CourtshipPhase phase = session.getPhase();
        if (phase != CourtshipPhase.ACCEPTED) {
            // Locally enforce invite timeout so we don't wait forever for a receiver that never accepted.
            long now = self.level().getGameTime();
            long inviteTimeoutTicks = ClockTicks.seconds(CourtshipSessionRegistry.INVITE_TIMEOUT_SECONDS).getTicks();
            if ((now - session.getPhaseEnteredGameTime()) > inviteTimeoutTicks) {
                return doAbort(self, session, CourtshipCloseReason.TIMEOUT);
            }
            return StepResult.noOp();
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return doAbort(self, session, CourtshipCloseReason.EXTERNAL_CANCEL);
        }

        BaseVillager partner = resolveParticipant(serverLevel, session.getReceiverId()).orElse(null);
        if (partner == null) {
            return doAbort(self, session, CourtshipCloseReason.ABORTED_PARTNER_GONE);
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(partner)));
        session.transitionTo(CourtshipPhase.APPROACH, self.level().getGameTime());
        return StepResult.transition(Stage.APPROACH);
    }

    private StepResult onWithinApproachRange(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        CourtshipSession session = currentSession(self).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        // Select choreography and store it so the receiver can mirror the same routine.
        int choreographyId = this.choreographyLibrary.selectRandom(self.getRandom());
        session.setChoreographyId(choreographyId);

        ChoreographyTimeline timeline = this.choreographyLibrary.get(choreographyId);
        long now = self.level().getGameTime();
        session.beginCourtship(now, (int) timeline.totalDurationTicks());

        courtshipPresenter.presentApproach(session, self);
        return StepResult.transition(Stage.COURTSHIP);
    }

    private StepResult tickCourtship(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        CourtshipSession session = currentSession(self).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!self.canBreed()) {
            return doAbort(self, session, CourtshipCloseReason.ABORTED_WILLINGNESS);
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return doAbort(self, session, CourtshipCloseReason.EXTERNAL_CANCEL);
        }

        BaseVillager partner = resolveParticipant(serverLevel, session.getReceiverId()).orElse(null);
        if (partner == null || !partner.canBreed()) {
            return doAbort(self, session, CourtshipCloseReason.ABORTED_PARTNER_GONE);
        }

        ChoreographyTimeline timeline = this.choreographyLibrary.get(session.getChoreographyId());
        long elapsed = self.level().getGameTime() - session.getCourtshipStartGameTime();

        // Derive which beat we are in: start tick of beat i = i * ticksPerBeat, so beat = elapsed / ticksPerBeat.
        // Use beatStartTick to find the boundary: largest i where beatStartTick(i, 0) <= elapsed.
        int targetBeat = 0;
        for (int i = timeline.beatCount() - 1; i >= 0; i--) {
            if (timeline.beatStartTick(i, 0) <= elapsed) {
                targetBeat = i;
                break;
            }
        }

        if (targetBeat > session.getCurrentBeatIndex()) {
            session.advanceBeat(targetBeat);
            courtshipPresenter.presentBeat(session, self, targetBeat);
        }

        if (elapsed >= timeline.totalDurationTicks()) {
            session.transitionTo(CourtshipPhase.BIRTH, self.level().getGameTime());
            return StepResult.transition(Stage.BIRTH);
        }

        return StepResult.noOp();
    }

    private StepResult birth(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        CourtshipSession session = currentSession(self).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (session.isBirthCompleted()) {
            return StepResult.complete();
        }

        if (!(self.level() instanceof ServerLevel serverLevel)) {
            return doAbort(self, session, CourtshipCloseReason.EXTERNAL_CANCEL);
        }

        BaseVillager partner = resolveParticipant(serverLevel, session.getReceiverId()).orElse(null);
        if (partner == null) {
            return doAbort(self, session, CourtshipCloseReason.ABORTED_PARTNER_GONE);
        }

        BlockPos primaryBed = this.bedReservationService.tryClaimVacantHome(serverLevel, self).orElse(null);
        if (primaryBed == null) {
            return doAbort(self, session, CourtshipCloseReason.ABORTED_NO_BED);
        }

        // Track for teardown; canceled below if a child successfully claims this bed.
        TemporaryArtifactHandle primaryHandle = context.getTeardownScope().track(new ReleaseHomePoiObligation(primaryBed));

        // Twins: independent roll + second bed claim. The second take() skips the already-claimed bed
        // (ticket exhausted) and finds a distinct one, if any.
        @Nullable BlockPos secondaryBed = null;
        @Nullable TemporaryArtifactHandle secondaryHandle = null;
        if (self.getRandom().nextFloat() < config.twinsChancePercent() / 100.0F) {
            secondaryBed = this.bedReservationService.tryClaimVacantHome(serverLevel, self).orElse(null);
            if (secondaryBed != null) {
                secondaryHandle = context.getTeardownScope().track(new ReleaseHomePoiObligation(secondaryBed));
            }
        }

        BaseVillager primaryChild = spawnChild(serverLevel, self, partner, primaryBed);
        if (primaryChild == null) {
            // Leave handles tracked so teardown releases the reserved beds.
            return doAbort(self, session, CourtshipCloseReason.ABORTED_SPAWN_FAILED);
        }

        // Child owns the primary bed now — cancel teardown so the release obligation is dropped.
        this.bedReservationService.assignHome(primaryChild, serverLevel, primaryBed);
        primaryHandle.cancel();

        if (secondaryBed != null) {
            BaseVillager secondaryChild = spawnChild(serverLevel, self, partner, secondaryBed);
            if (secondaryChild != null) {
                this.bedReservationService.assignHome(secondaryChild, serverLevel, secondaryBed);
                secondaryHandle.cancel();
            }
            // If secondary spawn failed, secondaryHandle stays tracked and teardown releases the unused bed.
        }

        self.setAge(CourtshipConstants.BREED_COOLDOWN_TICKS);
        partner.setAge(CourtshipConstants.BREED_COOLDOWN_TICKS);

        session.markBirthCompleted();
        this.courtshipPresenter.presentBirth(session, self, partner);
        this.sessionRegistry.closeSession(session.getSessionId(), CourtshipCloseReason.COMPLETED);

        return StepResult.complete();
    }

    @Nullable
    private BaseVillager spawnChild(@Nonnull ServerLevel level,
                                    @Nonnull BaseVillager parent,
                                    @Nonnull AgeableMob other,
                                    @Nonnull BlockPos bed) {
        BaseVillager child = parent.getBreedOffspring(level, other);
        if (child == null) {
            return null;
        }
        child.moveTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
        child.setAge(-24000);

        level.addFreshEntityWithPassengers(child);
        return child;
    }

    private StepResult doAbort(@Nonnull BaseVillager self,
                               @Nonnull CourtshipSession session,
                               @Nonnull CourtshipCloseReason reason) {
        this.courtshipPresenter.presentAbort(session, self, reason);
        this.sessionRegistry.closeSession(session.getSessionId(), reason);
        return StepResult.complete();
    }

    private Optional<CourtshipSession> currentSession(@Nonnull BaseVillager villager) {
        return this.sessionRegistry.getActiveSession(villager.getUUID())
                .filter(s -> this.activeSessionId == null || this.activeSessionId.equals(s.getSessionId()));
    }

    private static Optional<BaseVillager> resolveParticipant(@Nonnull Level level, @Nonnull UUID id) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return Optional.ofNullable(serverLevel.getEntity(id))
                .filter(BaseVillager.class::isInstance)
                .map(BaseVillager.class::cast);
    }

}
