package dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.courtship.ChoreographyTimeline;
import dev.breezes.settlements.application.ai.courtship.CourtshipChoreographyLibrary;
import dev.breezes.settlements.application.ai.courtship.CourtshipCloseReason;
import dev.breezes.settlements.application.ai.courtship.CourtshipConstants;
import dev.breezes.settlements.application.ai.courtship.CourtshipInvite;
import dev.breezes.settlements.application.ai.courtship.CourtshipPhase;
import dev.breezes.settlements.application.ai.courtship.CourtshipSession;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public final class CourtshipAcceptBehavior extends VillagerStateMachineBehavior {

    public enum Stage implements StageKey {
        MIRROR,
        CLOSED,
    }

    private final CourtshipSessionRegistry sessionRegistry;
    private final CourtshipPresenter courtshipPresenter;
    private final CourtshipChoreographyLibrary choreographyLibrary;

    private static final double BASE_ACCEPTANCE_CHANCE = 0.65D;
    private static final double CHARISMA_DIFFERENCE_WEIGHT = 0.35D;
    private static final double MIN_ACCEPTANCE_CHANCE = 0.15D;
    private static final double MAX_ACCEPTANCE_CHANCE = 0.95D;

    @Nullable
    private UUID activeSessionId;
    private int lastObservedBeat;

    public CourtshipAcceptBehavior(@Nonnull HungerConfig hungerConfig,
                                   @Nonnull CourtshipSessionRegistry sessionRegistry,
                                   @Nonnull CourtshipPresenter courtshipPresenter,
                                   @Nonnull CourtshipChoreographyLibrary choreographyLibrary) {
        super(log,
                ClockTicks.seconds(2).asTickable(),
                RandomRangeTickable.of(ClockTicks.seconds(30), ClockTicks.seconds(15)),
                hungerConfig);
        this.sessionRegistry = sessionRegistry;
        this.courtshipPresenter = courtshipPresenter;
        this.choreographyLibrary = choreographyLibrary;

        this.preconditions.add(this.hasCourtshipInviteOrSession());
        this.initializeStateMachine(
                StagedStep.<BaseVillager>builder()
                        .name("CourtshipAcceptBehavior")
                        .initialStage(Stage.MIRROR)
                        .stageStepMap(Map.of(
                                Stage.MIRROR, StayCloseStep.<BaseVillager>builder()
                                        .closeEnoughDistance(CourtshipConstants.DRIFT_DISTANCE)
                                        .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                        .actionStep(this::mirror)
                                        .build()
                        ))
                        .nextStage(Stage.CLOSED)
                        .build(),
                Stage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.lastObservedBeat = -1;

        long now = world.getGameTime();
        if (this.sessionRegistry.hasInviteFor(villager.getUUID())) {
            CourtshipInvite invite = this.sessionRegistry.getInvite(villager.getUUID()).orElse(null);
            if (invite == null) {
                return;
            }

            CourtshipSession pendingSession = this.sessionRegistry.getActiveSession(villager.getUUID()).orElse(null);
            if (pendingSession == null) {
                return;
            }

            if (!(world instanceof ServerLevel serverLevel)) {
                this.sessionRegistry.closeSession(pendingSession.getSessionId(), CourtshipCloseReason.EXTERNAL_CANCEL);
                return;
            }

            BaseVillager presenter = resolveParticipant(serverLevel, invite.presenterId()).orElse(null);
            if (presenter == null) {
                this.sessionRegistry.closeSession(pendingSession.getSessionId(), CourtshipCloseReason.ABORTED_PARTNER_GONE);
                return;
            }

            if (!RandomUtil.chance(charismaAcceptanceChance(presenter, villager))) {
                courtshipPresenter.presentAbort(pendingSession, villager, CourtshipCloseReason.REJECTED_CHARISMA);
                this.sessionRegistry.closeSession(pendingSession.getSessionId(), CourtshipCloseReason.REJECTED_CHARISMA);

                // The receiver is the only party that knows the charisma roll failed, so the
                // rejection is published from here. The receiver is the actor; the presenter is
                // the spurned target.
                UUID presenterId = invite.presenterId();
                UUID sessionId = pendingSession.getSessionId();
                BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.COURTSHIP_REJECTED, null);
                outcome.recordSocialOutcome(presenterId, sessionId, EventOutcome.FAILURE, null, "their charm fell short");
                context.setState(BehaviorStateType.BEHAVIOR_OUTCOME, outcome);
                return;
            }

            this.sessionRegistry.acceptInvite(villager.getUUID(), now);
        }

        CourtshipSession session = this.sessionRegistry.getActiveSession(villager.getUUID()).orElse(null);
        if (session == null) {
            return;
        }

        this.activeSessionId = session.getSessionId();
        courtshipPresenter.presentApproach(session, villager);

        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        // Navigate toward the Presenter from the start.
        resolveParticipant(serverLevel, session.getPresenterId())
                .map(Targetable::fromEntity)
                .ifPresent(target -> context.setState(BehaviorStateType.TARGET, TargetState.of(target)));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();

        if (this.activeSessionId != null) {
            courtshipPresenter.presentEnd(villager, this.activeSessionId, world.getGameTime());
        }

        this.activeSessionId = null;
        this.lastObservedBeat = -1;
    }

    private ICondition<BaseVillager> hasCourtshipInviteOrSession() {
        return ICondition.named("has courtship invite or session",
                villager -> this.sessionRegistry.hasInviteFor(villager.getUUID())
                        || this.sessionRegistry.getActiveSession(villager.getUUID()).isPresent());
    }

    private StepResult mirror(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        CourtshipSession session = currentSession(self).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        CourtshipPhase phase = session.getPhase();
        if (phase == CourtshipPhase.COMPLETED || phase == CourtshipPhase.ABORTED) {
            return StepResult.complete();
        }

        int presenterBeat = session.getCurrentBeatIndex();
        long now = self.level().getGameTime();

        // React to a new beat only after the reaction delay, so the response feels natural.
        if (presenterBeat != this.lastObservedBeat && isReactionReady(session, presenterBeat, now)) {
            courtshipPresenter.presentBeat(session, self, presenterBeat);
            this.lastObservedBeat = presenterBeat;
        }

        return StepResult.noOp();
    }

    private static double charismaAcceptanceChance(@Nonnull BaseVillager presenter, @Nonnull BaseVillager receiver) {
        double presenterCharisma = presenter.getGenetics().getGeneValue(GeneType.CHARISMA);
        double receiverCharisma = receiver.getGenetics().getGeneValue(GeneType.CHARISMA);

        double socialFit = BASE_ACCEPTANCE_CHANCE + (presenterCharisma - receiverCharisma) * CHARISMA_DIFFERENCE_WEIGHT;
        return Math.clamp(socialFit, MIN_ACCEPTANCE_CHANCE, MAX_ACCEPTANCE_CHANCE);
    }

    private boolean isReactionReady(@Nonnull CourtshipSession session, int beat, long now) {
        if (session.getCourtshipStartGameTime() < 0) {
            return false;
        }

        ChoreographyTimeline timeline = this.choreographyLibrary.get(session.getChoreographyId());
        long beatStartTick = timeline.beatStartTick(beat, session.getCourtshipStartGameTime());
        return (now - beatStartTick) >= CourtshipConstants.RECEIVER_REACTION_DELAY_TICKS;
    }

    private Optional<CourtshipSession> currentSession(@Nonnull BaseVillager villager) {
        return this.sessionRegistry.getActiveSession(villager.getUUID())
                .filter(s -> this.activeSessionId == null || this.activeSessionId.equals(s.getSessionId()));
    }

    private static Optional<BaseVillager> resolveParticipant(@Nonnull ServerLevel level, @Nonnull UUID id) {
        return Optional.ofNullable(level.getEntity(id))
                .filter(BaseVillager.class::isInstance)
                .map(BaseVillager.class::cast);
    }

}
