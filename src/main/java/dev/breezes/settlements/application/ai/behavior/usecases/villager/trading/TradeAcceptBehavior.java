package dev.breezes.settlements.application.ai.behavior.usecases.villager.trading;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.trading.TradeSession;
import dev.breezes.settlements.application.ai.trading.TradeSessionPhase;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@CustomLog
public final class TradeAcceptBehavior extends VillagerStateMachineBehavior {

    private static final double APPROACH_DISTANCE_SQUARED = 9.0D;
    private static final ClockTicks RESPONDER_PRESENTATION_DELAY = ClockTicks.seconds(2);
    private static final double TRADE_CLOSE_ENOUGH_DISTANCE = 3.0D;

    private final TradeSessionRegistry sessionRegistry;
    private final TradeSessionPresenter tradeSessionPresenter;

    @Nullable
    private PresentationSignature lastObservedSignature;

    public TradeAcceptBehavior(@Nonnull TradingConfig config,
                               @Nonnull HungerConfig hungerConfig,
                               @Nonnull TradeSessionRegistry sessionRegistry,
                               @Nonnull TradeSessionPresenter tradeSessionPresenter) {
        super(log,
                ClockTicks.seconds(config.acceptPreconditionCooldownSeconds()).asTickable(),
                RandomRangeTickable.of(
                        ClockTicks.seconds(config.acceptBehaviorCooldownSecondsMax()),
                        ClockTicks.seconds(config.acceptBehaviorCooldownSecondsMin())),
                hungerConfig);
        this.sessionRegistry = sessionRegistry;
        this.tradeSessionPresenter = tradeSessionPresenter;

        this.preconditions.add(this.hasTradeToMirror());
        this.initializeStateMachine(
                StagedStep.<BaseVillager>builder()
                        .name("TradeAcceptBehavior")
                        .initialStage(MirrorStage.MIRROR)
                        .stageStepMap(Map.of(MirrorStage.MIRROR, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(TRADE_CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                .actionStep(this::mirror)
                                .build()))
                        .nextStage(MirrorStage.CLOSED)
                        .build(),
                MirrorStage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        long currentGameTime = world.getGameTime();
        if (this.sessionRegistry.hasInviteFor(villager.getUUID())) {
            this.sessionRegistry.acceptInvite(villager.getUUID(), currentGameTime);
        }
        this.lastObservedSignature = null;

        if (world instanceof ServerLevel serverLevel) {
            // Set trading target
            Optional.ofNullable(serverLevel.getEntity(villager.getUUID()))
                    .map(Targetable::fromEntity)
                    .ifPresent(target -> context.setState(BehaviorStateType.TARGET, TargetState.of(target)));
        }
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world,
                                  @Nonnull BaseVillager villager) { // TODO: add behavior context to clear target
        villager.getNavigationManager().stop();
        this.lastObservedSignature = null;
    }

    private ICondition<BaseVillager> hasTradeToMirror() {
        return ICondition.named("HasTradeToMirror", villager -> this.sessionRegistry.hasInviteFor(villager.getUUID())
                || this.sessionRegistry.getActiveSession(villager.getUUID()).isPresent());
    }

    private StepResult mirror(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager self = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.sessionRegistry.getActiveSession(self.getUUID()).orElse(null);
        if (session == null) {
            log.behaviorWarn("Trade session is null, stopping the behavior");
            return StepResult.fail("Trade session is null");
        }

        PresentationSignature signature = PresentationSignature.from(session);
        if (!Objects.equals(signature, this.lastObservedSignature)
                && this.isPresentationReady(session, self.level().getGameTime())) {
            this.presentPhase(session, self);
            this.lastObservedSignature = signature;
        }

        return switch (session.getPhase()) {
            case EXTERNAL_CANCEL, CLOSED -> StepResult.complete();
            default -> StepResult.noOp();
        };
    }

    private boolean isPresentationReady(@Nonnull TradeSession session, long now) {
        return switch (session.getPhase()) {
            // Terminal phases are presented directly by the initiator on both villagers
            case DEAL, WALK_AWAY -> false;
            case EXTERNAL_CANCEL, CLOSED -> true;
            // Non-terminal phases stay gated, so the responder reacts after the initiator
            default -> (now - session.getPhaseEnteredGameTime()) >= RESPONDER_PRESENTATION_DELAY.getTicks();
        };
    }

    private void presentPhase(@Nonnull TradeSession session, @Nonnull BaseVillager self) {
        switch (session.getPhase()) {
            case OPENING_OFFER -> this.tradeSessionPresenter.presentOpeningOffer(session, self);
            case NEGOTIATING -> this.tradeSessionPresenter.presentNegotiationUpdate(session, self);
            case DEAL -> this.tradeSessionPresenter.presentDeal(session, self);
            case WALK_AWAY -> this.tradeSessionPresenter.presentWalkAway(session, self);
            default -> {
            }
        }
    }

    private enum MirrorStage implements StageKey {
        MIRROR,
        CLOSED,
    }

    private record PresentationSignature(@Nonnull TradeSessionPhase phase,
                                         int buyerOffer,
                                         int sellerAsk) {

        private static PresentationSignature from(@Nonnull TradeSession session) {
            return new PresentationSignature(session.getPhase(), session.getBuyerOffer(), session.getSellerAsk());
        }

    }

}
