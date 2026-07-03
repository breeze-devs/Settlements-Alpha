package dev.breezes.settlements.application.ai.behavior.usecases.villager.donation;

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
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.EventOutcome;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * A solvent villager, gated by a genetics-flavored generosity roll, walks up to a nearby
 * destitute neighbor and gifts a few emeralds through the abstract wallet ledger. No real
 * emerald {@code ItemEntity} is ever spawned — the held item, bubbles, and particles are cosmetic.
 */
@CustomLog
public final class DonateEmeraldsBehavior extends VillagerStateMachineBehavior {

    public enum Stage implements StageKey {
        SELECT_RECIPIENT,
        APPROACH,
        GIFT,
        WALK_AWAY,
        CLOSED,
    }

    private static final double CLOSE_ENOUGH_DISTANCE = 2.5D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 2;
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(15).getTicksAsInt();

    private final DonationConfig config;
    private final DonationScanner scanner;
    private final DonationPresenter presenter;
    private final VillagerWallet wallet;

    @Nullable
    private BaseVillager pendingRecipient;

    public DonateEmeraldsBehavior(@Nonnull DonationConfig config,
                                  @Nonnull BehaviorSupport support,
                                  @Nonnull DonationScanner scanner,
                                  @Nonnull DonationPresenter presenter,
                                  @Nonnull VillagerWallet wallet) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);
        this.config = config;
        this.scanner = scanner;
        this.presenter = presenter;
        this.wallet = wallet;

        this.preconditions.add(ICondition.named("can donate", this::canDonate));
        this.preconditions.add(ICondition.named("generosity roll", this::rollGenerosity));

        this.initializeStateMachine(this.createControlStep(), Stage.CLOSED);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        // Stay silent unless the GIFT stage actually transfers and declares the real deed
        context.declarePrimaryDeed(BehaviorOutcome.silent());
        this.pendingRecipient = null;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        this.pendingRecipient = null;
    }

    private boolean canDonate(@Nonnull BaseVillager donor) {
        int comfort = this.computeComfort(donor);
        return this.scanner.findNeediest(donor, this.config.floor(), this.config.minDonation(), comfort).isPresent();
    }

    private boolean rollGenerosity(@Nonnull BaseVillager donor) {
        double charisma = donor.getGenetics().getGeneValue(GeneType.CHARISMA);
        double chance = DonationAmountCalculator.chance(charisma, this.config.baseChance(), this.config.chanceImpact());
        return RandomUtil.chance(chance);
    }

    private int computeComfort(@Nonnull BaseVillager donor) {
        double charisma = donor.getGenetics().getGeneValue(GeneType.CHARISMA);
        return DonationAmountCalculator.comfort(charisma, this.config.baseComfort(), this.config.comfortImpact(), this.config.floor());
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("DonateEmeraldsBehavior")
                .initialStage(Stage.SELECT_RECIPIENT)
                .stageStepMap(Map.of(
                        Stage.SELECT_RECIPIENT, this::selectRecipient,
                        Stage.APPROACH, StayCloseStep.<BaseVillager>builder()
                                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                                        .navigationType(NavigationType.WALK)
                                        .completionDistance(NAVIGATION_COMPLETION_DISTANCE)
                                        .unreachableTransition(Stage.WALK_AWAY)
                                        .build())
                                .actionStep(this::onArrivedAtRecipient)
                                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                                .timeoutTransition(Stage.WALK_AWAY)
                                .build(),
                        Stage.GIFT, this::gift,
                        Stage.WALK_AWAY, this::walkAway
                ))
                .nextStage(Stage.CLOSED)
                .build();
    }

    private StepResult selectRecipient(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager donor = context.getInitiator().getMinecraftEntity();
        int comfort = this.computeComfort(donor);

        BaseVillager recipient = this.scanner.findNeediest(donor, this.config.floor(), this.config.minDonation(), comfort)
                .orElse(null);
        if (recipient == null) {
            return StepResult.complete();
        }

        this.pendingRecipient = recipient;
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(recipient)));
        donor.setHeldItem(new ItemStack(Items.EMERALD));

        return StepResult.transition(Stage.APPROACH);
    }

    private StepResult onArrivedAtRecipient(@Nonnull BehaviorContext<BaseVillager> context) {
        return StepResult.transition(Stage.GIFT);
    }

    private StepResult gift(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager donor = context.getInitiator().getMinecraftEntity();
        BaseVillager recipient = this.pendingRecipient;
        if (recipient == null || !recipient.isAlive() || recipient.isRemoved()) {
            return StepResult.transition(Stage.WALK_AWAY);
        }

        int recipientBalance = this.wallet.getBalance(recipient);
        if (recipientBalance >= this.config.floor()) {
            // Someone beat us to the floor between approach and arrival
            return StepResult.transition(Stage.WALK_AWAY);
        }

        int comfort = this.computeComfort(donor);
        int donorBalance = this.wallet.getBalance(donor);
        int cap = DonationAmountCalculator.cap(this.config.floor(), recipientBalance, donorBalance, comfort);
        if (cap < this.config.minDonation()) {
            return StepResult.transition(Stage.WALK_AWAY);
        }

        int amount = DonationAmountCalculator.pick(cap, this.config.minDonation());
        this.wallet.transfer(donor, recipient, amount);
        this.presenter.presentGift(donor, recipient, amount, donor.level().getGameTime());

        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.EMERALDS_DONATED, null);
        outcome.recordSocialOutcome(recipient.getUUID(), null, EventOutcome.SUCCESS, amount + " emeralds", null);
        outcome.putDetailField("count", String.valueOf(amount));
        context.declarePrimaryDeed(outcome);

        return StepResult.transition(Stage.WALK_AWAY);
    }

    private StepResult walkAway(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager donor = context.getInitiator().getMinecraftEntity();
        donor.clearHeldItem();
        this.pendingRecipient = null;
        return StepResult.complete();
    }

}
