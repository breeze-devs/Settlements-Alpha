package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class DepositSurplusBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;

    private enum DepositSurplusStage implements StageKey {
        DEPOSIT_SURPLUS,
        END
    }

    private final ChestWithSpaceForSurplusCondition chestCondition;

    @Nullable
    private ChestWithSpaceForSurplusCondition.Resolution resolution;

    public DepositSurplusBehavior(@Nonnull DepositSurplusConfig config,
                                  @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);

        this.chestCondition = new ChestWithSpaceForSurplusCondition(support.getSupplyEvaluator(), NAVIGATION_COMPLETION_DISTANCE);
        this.preconditions.add(this.chestCondition);
        this.resolution = null;

        this.initializeStateMachine(this.createControlStep(), DepositSurplusStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("DepositSurplusBehavior")
                .initialStage(DepositSurplusStage.DEPOSIT_SURPLUS)
                .stageStepMap(Map.of(DepositSurplusStage.DEPOSIT_SURPLUS, this.createDepositStep()))
                .nextStage(DepositSurplusStage.END)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.chestCondition.getResolution().isEmpty()) {
            this.requestStop("No nearby chests with space found");
            return;
        }

        this.resolution = this.chestCondition.getResolution().get();
        BlockPos chestBlockPos = this.resolution.chestPos().pos();
        PhysicalBlock block = PhysicalBlock.of(Location.of(chestBlockPos, world), world.getBlockState(chestBlockPos));
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(block)));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.getNavigationManager().stop();
        this.resolution = null;
    }

    private BehaviorStep<BaseVillager> createDepositStep() {
        TimeBasedStep<BaseVillager> interactStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for interaction");
                    }
                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    villager.triggerMotion(AnimationArchetype.INTERACT);
                    villager.setHeldItem(this.resolution.supply().representative().copyWithCount(1));
                    SoundRegistry.DEPOSIT_TO_CHEST.playGlobally(Location.of(this.resolution.chestPos().pos(), villager.level()), SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for interaction");
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    Level level = villager.level();
                    if (ChestWaxService.isWaxed(level, this.resolution.chestPos().pos())) {
                        return StepResult.fail("Chest waxed during interaction");
                    }

                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.resolution.chestPos().pos(), null);
                    if (handler == null) {
                        return StepResult.fail("Chest removed during interaction");
                    }

                    int targetCount = Math.min(this.resolution.acceptedCount(), this.resolution.supply().representative().getMaxStackSize());
                    ItemStack targetStack = this.resolution.supply().representative().copyWithCount(targetCount);
                    int acceptedCount = ChestWithSpaceForSurplusCondition.simulateAcceptedCount(handler, targetStack);
                    if (acceptedCount <= 0) {
                        return StepResult.fail("Selected chest no longer has space for surplus");
                    }

                    ItemStack consumedStack = this.resolution.supply().representative().copyWithCount(acceptedCount);
                    int consumed = villager.getSettlementsInventory().consume(consumedStack, acceptedCount);
                    if (consumed <= 0) {
                        return StepResult.fail("Surplus item no longer available for deposit");
                    }

                    ItemStack toInsert = this.resolution.supply().representative().copyWithCount(consumed);
                    ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
                    if (!remainder.isEmpty()) {
                        villager.getSettlementsInventory().add(remainder);
                    }

                    int inserted = consumed - remainder.getCount();
                    if (inserted <= 0) {
                        return StepResult.fail("Chest insertion failed");
                    }

                    BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.ITEMS_STORED, "items");
                    outcome.recordYield(inserted);
                    outcome.recordDeedDetail(toInsert.getItem().toString());
                    ctx.declarePrimaryDeed(outcome);

                    return StepResult.noOp();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(interactStep)
                .build();
    }

}
