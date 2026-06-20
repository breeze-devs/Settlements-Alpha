package dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@CustomLog
public class TakeFromChestBehavior extends VillagerStateMachineBehavior {

    private static final int TAKE_AT_LEAST_COUNT = 8;
    private static final double CLOSE_ENOUGH_DISTANCE = 2.0D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;

    private enum TakeFromChestStage implements StageKey {
        TAKE_FROM_CHEST,
        END
    }

    private final ChestWithDemandedItemCondition chestCondition;

    @Nullable
    private ChestWithDemandedItemCondition.Resolution resolution;

    public TakeFromChestBehavior(@Nonnull TakeFromChestConfig config,
                                 @Nonnull HungerConfig hungerConfig,
                                 @Nonnull DemandEvaluator demandEvaluator) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.chestCondition = new ChestWithDemandedItemCondition(demandEvaluator);
        this.preconditions.add(this.chestCondition);

        this.resolution = null;

        this.initializeStateMachine(this.createControlStep(), TakeFromChestStage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("TakeFromChestBehavior")
                .initialStage(TakeFromChestStage.TAKE_FROM_CHEST)
                .stageStepMap(Map.of(TakeFromChestStage.TAKE_FROM_CHEST, this.createTakeStep()))
                .nextStage(TakeFromChestStage.END)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.chestCondition.getResolution().isEmpty()) {
            this.requestStop("No nearby chests found");
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

    private BehaviorStep<BaseVillager> createTakeStep() {
        TimeBasedStep<BaseVillager> interactStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for interaction");
                    }
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    Level level = ctx.getInitiator().getMinecraftEntity().level();
                    SoundRegistry.TAKE_FROM_CHEST.playGlobally(Location.of(this.resolution.chestPos().pos(), level), SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_DURATION_TICKS), ctx -> {
                    if (this.resolution == null) {
                        return StepResult.fail("No resolution available for interaction");
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    villager.setHeldItem(this.resolution.matchedStack().copyWithCount(1));
                    Level level = villager.level();

                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, this.resolution.chestPos().pos(), null);
                    if (handler == null) {
                        // Chest removed between precondition pass and interact keyframe.
                        return StepResult.fail("Chest removed during interaction");
                    }

                    // Re-validate before committing the extraction (chest may have been looted mid-walk).
                    ItemStack live = handler.getStackInSlot(this.resolution.slot());
                    if (!ItemMatches.test(this.resolution.demand().match(), live)) {
                        return StepResult.fail("Chest contents changed during interaction");
                    }

                    int takeCount = computeTakeCount(this.resolution, live);
                    if (takeCount <= 0) {
                        return StepResult.fail("Unable to take %d items from chest".formatted(this.resolution.demand().desiredCount()));
                    }

                    ItemStack extracted = handler.extractItem(this.resolution.slot(), takeCount, false);
                    if (extracted.isEmpty()) {
                        return StepResult.fail("Chest extraction failed (empty result)");
                    }

                    villager.getSettlementsInventory().add(extracted);

                    return StepResult.noOp();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(interactStep)
                .build();
    }

    private static int computeTakeCount(@Nonnull ChestWithDemandedItemCondition.Resolution resolution,
                                        @Nonnull ItemStack live) {
        return Math.clamp(resolution.demand().desiredCount(), TAKE_AT_LEAST_COUNT, live.getCount());
    }

}
