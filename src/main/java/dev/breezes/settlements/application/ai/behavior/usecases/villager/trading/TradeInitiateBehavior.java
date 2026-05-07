package dev.breezes.settlements.application.ai.behavior.usecases.villager.trading;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.ai.trading.CloseReason;
import dev.breezes.settlements.application.ai.trading.NegotiationEngine;
import dev.breezes.settlements.application.ai.trading.TradeExecutor;
import dev.breezes.settlements.application.ai.trading.TradeSession;
import dev.breezes.settlements.application.ai.trading.TradeSessionPhase;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.catalog.TradePriceResolver;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public final class TradeInitiateBehavior extends StateMachineBehavior {

    public enum Stage implements StageKey {
        SCAN_AND_INVITE,
        WAIT_FOR_ACCEPT,
        APPROACH,
        OPENING_OFFER,
        NEGOTIATE,
        DEAL,
        WALK_AWAY,
        CLOSED,
    }

    private static final int DEAL_PRICE_EPSILON = 1;
    private static final double APPROACH_DISTANCE_SQUARED = 9.0D;
    private static final double TRADE_CLOSE_ENOUGH_DISTANCE = 3.0D;

    private final TradingConfig config;
    private final TradeSessionRegistry sessionRegistry;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final TradePriceResolver tradePriceResolver;
    private final DemandSignalService demandSignalService;
    private final VillagerWallet villagerWallet;
    private final TradeExecutor tradeExecutor;
    private final TradeSessionPresenter tradeSessionPresenter;
    private final DemandEvaluator demandEvaluator;
    private final PartnerScanner partnerScanner;
    private final NegotiationEngine negotiationEngine;

    @Nullable
    private PartnerScanner.TradeCandidate pendingCandidate;
    @Nullable
    private ActiveDemand activeDemand;
    @Nullable
    private UUID activeSessionId;
    private boolean tradeExecuted;

    public TradeInitiateBehavior(@Nonnull TradingConfig config,
                                 @Nonnull HungerConfig hungerConfig,
                                 @Nonnull TradeSessionRegistry sessionRegistry,
                                 @Nonnull TradeCatalogRegistry tradeCatalogRegistry,
                                 @Nonnull TradePriceResolver tradePriceResolver,
                                 @Nonnull DemandSignalService demandSignalService,
                                 @Nonnull VillagerWallet villagerWallet,
                                 @Nonnull TradeExecutor tradeExecutor,
                                 @Nonnull TradeSessionPresenter tradeSessionPresenter,
                                 @Nonnull DemandEvaluator demandEvaluator,
                                 @Nonnull PartnerScanner partnerScanner,
                                 @Nonnull NegotiationEngine negotiationEngine) {
        super(log,
                Ticks.seconds(config.initiatePreconditionCooldownSeconds()).asTickable(),
                RandomRangeTickable.of(
                        Ticks.seconds(config.initiateBehaviorCooldownSecondsMax()),
                        Ticks.seconds(config.initiateBehaviorCooldownSecondsMin())),
                hungerConfig);
        this.config = config;

        this.sessionRegistry = sessionRegistry;
        this.tradeCatalogRegistry = tradeCatalogRegistry;
        this.tradePriceResolver = tradePriceResolver;
        this.demandSignalService = demandSignalService;
        this.villagerWallet = villagerWallet;
        this.tradeExecutor = tradeExecutor;
        this.tradeSessionPresenter = tradeSessionPresenter;
        this.demandEvaluator = demandEvaluator;
        this.partnerScanner = partnerScanner;
        this.negotiationEngine = negotiationEngine;

        this.preconditions.add(this.canInitiateTrade());
        this.initializeStateMachine(this.createControlStep(), Stage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext context) {
        this.activeDemand = null;
        this.activeSessionId = null;
        this.tradeExecuted = false;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();

        this.sessionRegistry.getActiveSession(villager.getUUID()).ifPresent(session -> {
            TradeSessionPhase phase = session.getPhase();
            if (phase != TradeSessionPhase.DEAL && phase != TradeSessionPhase.WALK_AWAY) {
                log.warn("Closing trade session after external interruption: sessionId={}, phase={}",
                        session.getSessionId(), phase);
                this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.EXTERNAL_CANCEL);
            }
        });

        this.pendingCandidate = null;
        this.activeDemand = null;
        this.activeSessionId = null;
        this.tradeExecuted = false;
    }

    private ICondition<BaseVillager> canInitiateTrade() {
        return ICondition.named("can initiate villager trade", villager -> {
            this.pendingCandidate = null;
            if (this.sessionRegistry.getActiveSession(villager.getUUID()).isPresent()) {
                log.behaviorStatus("Trade initiation blocked: villager {} already has an active session", villager.getUUID());
                return false;
            }
            if (this.villagerWallet.getBalance(villager) <= 0) {
                log.behaviorStatus("Trade initiation blocked: villager {} has no emerald balance", villager.getUUID());
                return false;
            }

            List<ActiveDemand> activeDemands = this.demandEvaluator.resolve(villager);
            if (activeDemands.isEmpty()) {
                log.behaviorStatus("Trade initiation blocked: villager {} has no active demands", villager.getUUID());
                return false;
            }

            log.behaviorStatus("Trade initiation evaluating {} active demands for villager {}: {}",
                    activeDemands.size(), villager.getUUID(), activeDemands.stream().map(demand -> demand.match().asDebugString()).toList());

            this.pendingCandidate = this.partnerScanner.findPartner(villager, activeDemands, this.config.scanRadiusBlocks())
                    .orElse(null);
            if (this.pendingCandidate == null) {
                log.behaviorStatus("Trade initiation blocked: villager {} found no partner candidate", villager.getUUID());
                return false;
            }

            log.behaviorStatus("Trade initiation selected partner {} for villager {} with item {}",
                    this.pendingCandidate.partner().getUUID(), villager.getUUID(), this.pendingCandidate.matchedItem());

            return true;
        });
    }

    private StagedStep createControlStep() {
        return StagedStep.builder()
                .name("TradeInitiateBehavior")
                .initialStage(Stage.SCAN_AND_INVITE)
                .stageStepMap(Map.of(
                        Stage.SCAN_AND_INVITE, this::scanAndInvite,
                        Stage.WAIT_FOR_ACCEPT, this::waitForAccept,
                        Stage.APPROACH, this::approach,
                        Stage.OPENING_OFFER, this::openingOffer,
                        Stage.NEGOTIATE, StayCloseStep.builder()
                                .closeEnoughDistance(TRADE_CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(new NavigateToTargetStep(0.5f, 2))
                                .actionStep(this::negotiate)
                                .build(),
                        Stage.DEAL, this::deal,
                        Stage.WALK_AWAY, this::walkAway
                ))
                .nextStage(Stage.CLOSED)
                .build();
    }

    private StepResult scanAndInvite(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        if (this.pendingCandidate == null) {
            return StepResult.complete();
        }

        // Capture execution-phase data from the scan before discarding the candidate.
        // pendingCandidate is a precondition-time artifact and must not be read after this stage.
        this.activeDemand = this.pendingCandidate.activeDemand();
        int priceJitter = this.pendingCandidate.offerEntry().priceJitter();

        long currentGameTime = buyer.level().getGameTime();
        TradeSession session = this.sessionRegistry.sendInvite(
                buyer,
                this.pendingCandidate.partner(),
                buyer.getUUID(),
                this.pendingCandidate.partner().getUUID(),
                this.pendingCandidate.matchedItem(),
                this.pendingCandidate.offerEntry().bundleSize(),
                priceJitter,
                this.pendingCandidate.buyerOffer(),
                this.pendingCandidate.sellerAsk(),
                this.config.maxNegotiationRounds(),
                currentGameTime);
        this.pendingCandidate = null;
        this.activeSessionId = session.getSessionId();
        return StepResult.transition(Stage.WAIT_FOR_ACCEPT);
    }

    private StepResult waitForAccept(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        Optional<TradeSession> session = this.currentSession(buyer);
        if (session.isEmpty()) {
            return StepResult.complete();
        }

        if (session.get().getPhase() != TradeSessionPhase.ESTABLISHED) {
            return StepResult.noOp();
        }

        session.get().transitionTo(TradeSessionPhase.APPROACH, buyer.level().getGameTime());
        return StepResult.transition(Stage.APPROACH);
    }

    private StepResult approach(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.fail("Trade failed due to missing session");
        }

        BaseVillager seller = this.resolveParticipant(buyer.level(), session.getSellerId()).orElse(null);
        if (seller == null) {
            this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.EXTERNAL_CANCEL);
            return StepResult.fail("Trade failed due to missing seller");
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(seller)));

        buyer.getNavigationManager().stop();
        session.transitionTo(TradeSessionPhase.OPENING_OFFER, buyer.level().getGameTime());
        this.tradeSessionPresenter.presentOpeningOffer(session, buyer);
        return StepResult.transition(Stage.OPENING_OFFER);
    }

    private StepResult openingOffer(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.openingOfferDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        if (this.hasDeal(session)) {
            return this.enterDeal(buyer, session);
        }

        session.transitionTo(TradeSessionPhase.NEGOTIATING, buyer.level().getGameTime());
        return StepResult.transition(Stage.NEGOTIATE);
    }

    private StepResult negotiate(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        BaseVillager seller = this.resolveParticipant(buyer.level(), session.getSellerId()).orElse(null);
        if (seller == null) {
            this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.EXTERNAL_CANCEL);
            return StepResult.fail("seller-disappeared-during-negotiation");
        }

        this.lockMutualAttention(buyer, seller);
        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.negotiationRoundDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        if (this.hasDeal(session)) {
            return this.enterDeal(buyer, session);
        }

        if (session.getNegotiationRoundsRemaining() <= 0) {
            session.transitionTo(TradeSessionPhase.WALK_AWAY, buyer.level().getGameTime());
            this.presentWalkAwayToBoth(session, buyer, seller);
            return StepResult.transition(Stage.WALK_AWAY);
        }

        if (session.getBuyerOffer() >= session.getSellerAsk()) {
            // If consensus is reached, deal
            return this.enterDeal(buyer, session);
        }

        NegotiationEngine.NegotiationRoundResult result = this.negotiationEngine.advanceRound(
                session.getBuyerOffer(),
                session.getSellerAsk(),
                session.getPriceJitter());
        session.setBuyerOffer(result.buyerOffer());
        session.setSellerAsk(result.sellerAsk());
        session.setNegotiationRoundsRemaining(session.getNegotiationRoundsRemaining() - 1);
        session.transitionTo(TradeSessionPhase.NEGOTIATING, buyer.level().getGameTime());

        this.tradeSessionPresenter.presentNegotiationUpdate(session, buyer);
        if (this.hasDeal(session)) {
            return this.enterDeal(buyer, session);
        }
        return StepResult.noOp();
    }

    private StepResult deal(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.dealDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.DEAL);
        return StepResult.complete();
    }

    private StepResult walkAway(@Nonnull BehaviorContext context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.walkawayDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.WALK_AWAY);
        return StepResult.complete();
    }

    private StepResult enterDeal(@Nonnull BaseVillager buyer, @Nonnull TradeSession session) {
        BaseVillager seller = this.resolveParticipant(buyer.level(), session.getSellerId()).orElse(null);
        if (seller == null) {
            this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.EXTERNAL_CANCEL);
            return StepResult.fail("seller-disappeared-before-deal");
        }

        if (!this.tradeExecuted) {
            if (!this.villagerWallet.canAfford(buyer, session.getBuyerOffer())) {
                // If buyer cannot afford the deal, walk away
                log.behaviorStatus("Buyer cannot afford this deal, walking away");
                session.transitionTo(TradeSessionPhase.WALK_AWAY, buyer.level().getGameTime());
                this.presentWalkAwayToBoth(session, buyer, seller);
                return StepResult.transition(Stage.WALK_AWAY);
            }

            // The session remains open for a short DEAL phase so both villagers can render the final
            // state, but the economic mutation must happen exactly once on the transition into DEAL.
            try {
                this.tradeExecutor.execute(session, buyer, seller);
            } catch (RuntimeException exception) {
                log.warn("Trade execution failed, converting to walk-away: sessionId={}, buyer={}, seller={}",
                        session.getSessionId(), buyer.getUUID(), seller.getUUID(), exception);
                session.transitionTo(TradeSessionPhase.WALK_AWAY, buyer.level().getGameTime());
                this.presentWalkAwayToBoth(session, buyer, seller);
                return StepResult.transition(Stage.WALK_AWAY);
            }
            if (this.activeDemand != null
                    && this.activeDemand.origin() != ActiveDemand.Origin.STATIC_SHORTFALL) {
                this.demandSignalService.remove(buyer, this.activeDemand.match());
            }
            this.tradeExecuted = true;
        }

        session.transitionTo(TradeSessionPhase.DEAL, buyer.level().getGameTime());
        this.presentDealToBoth(session, buyer, seller);
        return StepResult.transition(Stage.DEAL);
    }

    private Optional<TradeSession> currentSession(@Nonnull BaseVillager villager) {
        return this.sessionRegistry.getActiveSession(villager.getUUID())
                .filter(session -> this.activeSessionId == null || this.activeSessionId.equals(session.getSessionId()));
    }

    private Optional<BaseVillager> resolveParticipant(@Nonnull Level level, @Nonnull UUID participantId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return Optional.ofNullable(serverLevel.getEntity(participantId))
                .filter(BaseVillager.class::isInstance)
                .map(BaseVillager.class::cast);
    }

    private void presentDealToBoth(@Nonnull TradeSession session,
                                   @Nonnull BaseVillager buyer,
                                   @Nonnull BaseVillager seller) {
        this.tradeSessionPresenter.presentDeal(session, buyer);
        this.tradeSessionPresenter.presentDeal(session, seller);
    }

    private void presentWalkAwayToBoth(@Nonnull TradeSession session,
                                       @Nonnull BaseVillager buyer,
                                       @Nonnull BaseVillager seller) {
        this.tradeSessionPresenter.presentWalkAway(session, buyer);
        this.tradeSessionPresenter.presentWalkAway(session, seller);
    }

    private void lockMutualAttention(@Nonnull BaseVillager buyer, @Nonnull BaseVillager seller) {
        buyer.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(seller, true));
        seller.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(buyer, true));
    }

    private boolean hasDeal(@Nonnull TradeSession session) {
        int spread = session.getSellerAsk() - session.getBuyerOffer();
        return spread <= DEAL_PRICE_EPSILON;
    }

    private boolean phaseElapsed(@Nonnull TradeSession session, long currentGameTime, int durationTicks) {
        return (currentGameTime - session.getPhaseEnteredGameTime()) >= durationTicks;
    }

}
