package dev.breezes.settlements.application.ai.behavior.usecases.villager.trading;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
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
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CustomLog(topic = "behavior")
public final class TradeInitiateBehavior extends VillagerStateMachineBehavior {

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
    private static final double TRADE_CLOSE_ENOUGH_DISTANCE = 3.0D;

    private final TradingConfig config;
    private final TradeSessionRegistry sessionRegistry;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final TradePriceResolver tradePriceResolver;
    private final DemandSignalService demandSignalService;
    private final VillagerWallet villagerWallet;
    private final TradeExecutor tradeExecutor;
    private final TradeSessionPresenter tradeSessionPresenter;
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
                                 @Nonnull BehaviorSupport support,
                                 @Nonnull TradeSessionRegistry sessionRegistry,
                                 @Nonnull TradeCatalogRegistry tradeCatalogRegistry,
                                 @Nonnull TradePriceResolver tradePriceResolver,
                                 @Nonnull VillagerWallet villagerWallet,
                                 @Nonnull TradeExecutor tradeExecutor,
                                 @Nonnull TradeSessionPresenter tradeSessionPresenter,
                                 @Nonnull PartnerScanner partnerScanner,
                                 @Nonnull NegotiationEngine negotiationEngine) {
        super(log,
                Tickable.staggered(ClockTicks.seconds(config.partnerScanIntervalSeconds())),
                RandomRangeTickable.of(
                        ClockTicks.seconds(config.initiateBehaviorCooldownSecondsMax()),
                        ClockTicks.seconds(config.initiateBehaviorCooldownSecondsMin())),
                support);
        this.config = config;

        this.sessionRegistry = sessionRegistry;
        this.tradeCatalogRegistry = tradeCatalogRegistry;
        this.tradePriceResolver = tradePriceResolver;
        this.demandSignalService = support.getDemandSignalService();
        this.villagerWallet = villagerWallet;
        this.tradeExecutor = tradeExecutor;
        this.tradeSessionPresenter = tradeSessionPresenter;
        this.partnerScanner = partnerScanner;
        this.negotiationEngine = negotiationEngine;

        this.preconditions.add(this.canInitiateTrade());
        this.initializeStateMachine(this.createControlStep(), Stage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        context.declarePrimaryDeed(BehaviorOutcome.silent());
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
                log.trace("Closing unfinished trade session as the behavior stops: sessionId={}, phase={}",
                        session.getSessionId(), phase);
                this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.EXTERNAL_CANCEL);
            }
        });

        // The override lane builds this behavior fresh for every run, so its own cooldown never spans two attempts.
        // The memory carries the wait instead, after any outcome, so a failed attempt does not re-invite the same seller.
        ClockTicks cooldown = ClockTicks.seconds(RandomUtil.randomInt(this.config.initiateBehaviorCooldownSecondsMin(),
                this.config.initiateBehaviorCooldownSecondsMax(), true));
        villager.getSettlementsBrain().setMemory(MemoryTypeRegistry.TRADE_INITIATE_COOLDOWN, true, cooldown);

        this.pendingCandidate = null;
        this.activeDemand = null;
        this.activeSessionId = null;
        this.tradeExecuted = false;
    }

    private ICondition<BaseVillager> canInitiateTrade() {
        return ICondition.named("can initiate villager trade", villager -> {
            // Re-check only the seller the sensor chose; searching again here could settle on a different one
            this.pendingCandidate = villager.getSettlementsBrain().getMemory(MemoryTypeRegistry.DEMANDED_ITEM_SELLER)
                    .flatMap(seller -> this.partnerScanner.matchPartner(villager, seller.getMinecraftEntity()))
                    .orElse(null);
            if (this.pendingCandidate == null) {
                // Discard the stale observation so another attempt waits for a fresh sensor scan.
                villager.getSettlementsBrain().clearMemory(MemoryTypeRegistry.DEMANDED_ITEM_SELLER);
                return false;
            }

            log.debug("Trade initiation selected partner {} for villager {} with item {}",
                    this.pendingCandidate.partner().getUUID(), villager.getUUID(), this.pendingCandidate.matchedItem());

            return true;
        });
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("TradeInitiateBehavior")
                .initialStage(Stage.SCAN_AND_INVITE)
                .stageStepMap(Map.of(
                        Stage.SCAN_AND_INVITE, this::scanAndInvite,
                        Stage.WAIT_FOR_ACCEPT, this::waitForAccept,
                        Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(TRADE_CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                .actionStep(this::arriveAtSeller)
                                .timeoutTicks(TradeSessionRegistry.MAX_APPROACH_DURATION.getTicksAsInt())
                                .build(),
                        Stage.OPENING_OFFER, this::openingOffer,
                        Stage.NEGOTIATE, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(TRADE_CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 2))
                                .actionStep(this::negotiate)
                                .build(),
                        Stage.DEAL, this::deal,
                        Stage.WALK_AWAY, this::walkAway
                ))
                .nextStage(Stage.CLOSED)
                .build();
    }

    private StepResult scanAndInvite(@Nonnull BehaviorContext<BaseVillager> context) {
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

    private StepResult waitForAccept(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        Optional<TradeSession> session = this.currentSession(buyer);
        if (session.isEmpty()) {
            return StepResult.complete();
        }

        if (session.get().getPhase() != TradeSessionPhase.ESTABLISHED) {
            return StepResult.noOp();
        }

        BaseVillager seller = this.resolveParticipant(buyer.level(), session.get().getSellerId()).orElse(null);
        if (seller == null) {
            this.sessionRegistry.closeSession(session.get().getSessionId(), CloseReason.EXTERNAL_CANCEL);
            return StepResult.fail("Trade failed due to missing seller");
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(seller)));
        session.get().transitionTo(TradeSessionPhase.APPROACH, buyer.level().getGameTime());
        return StepResult.transition(Stage.APPROACH);
    }

    private StepResult arriveAtSeller(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.fail("Trade failed due to missing session");
        }

        buyer.getNavigationManager().stop();
        session.transitionTo(TradeSessionPhase.OPENING_OFFER, buyer.level().getGameTime());
        this.tradeSessionPresenter.presentOpeningOffer(session, buyer);
        return StepResult.transition(Stage.OPENING_OFFER);
    }

    private StepResult openingOffer(@Nonnull BehaviorContext<BaseVillager> context) {
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

    private StepResult negotiate(@Nonnull BehaviorContext<BaseVillager> context) {
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
            return this.enterWalkAway(buyer, seller, session);
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

    private StepResult deal(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.dealDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.DEAL);
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TRADE_COMPLETED, null);
        outcome.recordSocialOutcome(session.getResponderId(), session.getSessionId(),
                EventOutcome.SUCCESS, buildTradeDetail(session), null);
        recordTradeDetailFields(outcome, session);
        context.declarePrimaryDeed(outcome);
        return StepResult.complete();
    }

    private StepResult walkAway(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager buyer = context.getInitiator().getMinecraftEntity();
        TradeSession session = this.currentSession(buyer).orElse(null);
        if (session == null) {
            return StepResult.complete();
        }

        if (!this.phaseElapsed(session, buyer.level().getGameTime(), this.config.walkawayDuration().getTicksAsInt())) {
            return StepResult.noOp();
        }

        this.sessionRegistry.closeSession(session.getSessionId(), CloseReason.WALK_AWAY);
        // A negotiated failure is an observable social fact
        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TRADE_COMPLETED, null);
        outcome.recordSocialOutcome(session.getResponderId(), session.getSessionId(),
                EventOutcome.FAILURE, null, "negotiations fell through");
        context.declarePrimaryDeed(outcome);
        return StepResult.complete();
    }

    private StepResult enterWalkAway(@Nonnull BaseVillager buyer,
                                     @Nonnull BaseVillager seller,
                                     @Nonnull TradeSession session) {
        session.transitionTo(TradeSessionPhase.WALK_AWAY, buyer.level().getGameTime());
        this.presentWalkAwayToBoth(session, buyer, seller);
        return StepResult.transition(Stage.WALK_AWAY);
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
                log.debug("Buyer {} cannot afford the deal at {} emeralds, walking away", buyer.getUUID(),
                        session.getBuyerOffer());
                return this.enterWalkAway(buyer, seller, session);
            }
            // The seller's stock can change while the trade is negotiated
            if (seller.getSettlementsInventory().count(session.getMatchedItem()) < session.getBundleSize()) {
                log.debug("Seller {} no longer has {} {}, walking away", seller.getUUID(), session.getBundleSize(),
                        session.getMatchedItem());
                return this.enterWalkAway(buyer, seller, session);
            }

            // The session remains open for a short DEAL phase so both villagers can render the final
            // state, but the economic mutation must happen exactly once on the transition into DEAL.
            try {
                this.tradeExecutor.execute(session, buyer, seller);
            } catch (RuntimeException exception) {
                log.error("Trade execution failed, converting to walk-away: sessionId={}, buyer={}, seller={}",
                        session.getSessionId(), buyer.getUUID(), seller.getUUID(), exception);
                return this.enterWalkAway(buyer, seller, session);
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

    @Override
    protected boolean shouldLookAtActiveTarget() {
        // Buyer and seller track each other via EntityTracker; do not override with a static target look.
        return false;
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

    /**
     * Formats a human-readable detail string from the session's agreed prices.
     */
    private static String buildTradeDetail(@Nonnull TradeSession session) {
        String itemPath = BuiltInRegistries.ITEM.getKey(session.getMatchedItem()).getPath();
        int quantity = session.getBundleSize();
        int price = session.getBuyerOffer();
        String emeraldLabel = price == 1 ? "1 emerald" : price + " emeralds";
        return quantity + " " + itemPath + " for " + emeraldLabel;
    }

    /**
     * Supplements the flat trade-detail string with structured slots for monologue phrasing
     */
    private static void recordTradeDetailFields(@Nonnull BehaviorOutcome outcome,
                                                @Nonnull TradeSession session) {
        String itemPath = BuiltInRegistries.ITEM.getKey(session.getMatchedItem()).getPath();
        int price = session.getBuyerOffer();
        String emeraldLabel = price == 1 ? "1 emerald" : price + " emeralds";
        outcome.putDetailField("item", itemPath);
        outcome.putDetailField("count", String.valueOf(session.getBundleSize()));
        outcome.putDetailField("price", emeraldLabel);
    }

}
