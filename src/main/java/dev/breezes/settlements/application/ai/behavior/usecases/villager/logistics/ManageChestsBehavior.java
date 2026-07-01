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
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.supply.ActiveSupply;
import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.InteractAnimations;
import dev.breezes.settlements.domain.economy.catalog.ItemMatches;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Logistics chore that walks the village chests to satisfy active demand and dump active surplus
 */
@CustomLog
public class ManageChestsBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0D;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;
    private static final int APPROACH_TIMEOUT_TICKS = ClockTicks.seconds(20).getTicksAsInt();
    private static final int TAKE_AT_LEAST_COUNT = 8;
    private static final int MAX_CHESTS_PER_RUN = 4;

    private enum Stage implements StageKey {
        PICK_CHEST, APPROACH_CHEST, INTERACT, LOOP, END
    }

    private final ChestInteractionCondition chestCondition;
    private final DemandEvaluator demandEvaluator;
    private final SupplyEvaluator supplyEvaluator;

    private final List<BlockPos> reachableChests = new ArrayList<>();
    private final Set<BlockPos> visited = new HashSet<>();
    @Nullable
    private BlockPos currentChestPos;

    private final Map<String, Integer> takenByItem = new HashMap<>();
    private final Map<String, Integer> storedByItem = new HashMap<>();
    private int takenTotal;
    private int storedTotal;

    public ManageChestsBehavior(@Nonnull ManageChestsConfig config,
                                @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support);

        this.demandEvaluator = support.getDemandEvaluator();
        this.supplyEvaluator = support.getSupplyEvaluator();
        this.chestCondition = new ChestInteractionCondition(this.demandEvaluator, this.supplyEvaluator, NAVIGATION_COMPLETION_DISTANCE);
        this.preconditions.add(this.chestCondition);

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.PICK_CHEST, this.createPickChestStep());
        stageMap.put(Stage.APPROACH_CHEST, StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.WALK)
                        .completionDistance(NAVIGATION_COMPLETION_DISTANCE)
                        .unreachableTransition(Stage.PICK_CHEST)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("ArrivedAtChest")
                        .action(ctx -> this.onArrivedAtChest())
                        .build())
                .timeoutTicks(APPROACH_TIMEOUT_TICKS)
                .timeoutTransition(Stage.PICK_CHEST)
                .build());
        stageMap.put(Stage.INTERACT, this.createInteractStep());
        stageMap.put(Stage.LOOP, LoopBackStep.<BaseVillager>builder()
                .name("ManageChestsLoopBack")
                .loopBackTo(Stage.PICK_CHEST)
                .completionTransition(Stage.END)
                .maxIterationsResolver(ctx -> MAX_CHESTS_PER_RUN)
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("ManageChestsBehavior")
                .initialStage(Stage.PICK_CHEST)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createPickChestStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("PickNextChest")
                .action(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    Level level = villager.level();

                    // Re-evaluate live: prior legs moved items, so the demands and surplus changed
                    List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
                    List<ActiveSupply> supplies = this.supplyEvaluator.resolve(villager);

                    for (BlockPos chestPos : this.reachableChests) {
                        if (this.visited.contains(chestPos)) {
                            continue;
                        }
                        if (ChestWaxService.isWaxed(level, chestPos)) {
                            // A chest waxed mid-run is excluded from logistics for the rest of the run.
                            this.visited.add(chestPos);
                            continue;
                        }

                        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, null);
                        if (handler == null) {
                            this.visited.add(chestPos);
                            continue;
                        }
                        if (!ChestInteractionCondition.chestHasWork(handler, demands, supplies)) {
                            // No work now; demands/supplies only shrink, so it will not gain work later.
                            continue;
                        }

                        // Mark on selection
                        this.visited.add(chestPos);
                        this.currentChestPos = chestPos;
                        PhysicalBlock block = PhysicalBlock.of(Location.of(chestPos, level), level.getBlockState(chestPos));
                        ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(block)));
                        return StepResult.transition(Stage.APPROACH_CHEST);
                    }

                    return StepResult.transition(Stage.END);
                })
                .build();
    }

    private StepResult onArrivedAtChest() {
        if (this.currentChestPos == null) {
            return StepResult.transition(Stage.PICK_CHEST);
        }
        return StepResult.transition(Stage.INTERACT);
    }

    /**
     * One interaction at the current chest: take demands first, deposit after
     */
    private BehaviorStep<BaseVillager> createInteractStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("ManageChestInteract")
                .withTickable(ClockTicks.seconds(1).asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(InteractAnimations.INTERACT_PEAK_TICK), ctx -> {
                    BlockPos chestPos = this.currentChestPos;
                    if (chestPos == null) {
                        return StepResult.noOp();
                    }

                    BaseVillager villager = ctx.getInitiator();
                    Level level = villager.level();

                    // The chest was vetted at pick time, but the world can change during the approach
                    // It is already marked visited, so bailing here simply falls through to LOOP
                    if (ChestWaxService.isWaxed(level, chestPos)) {
                        return StepResult.noOp();
                    }

                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, chestPos, null);
                    if (handler == null) {
                        return StepResult.noOp();
                    }

                    int taken = this.takeDemandedItems(villager, handler, chestPos);
                    int stored = this.depositSurplus(villager, handler, chestPos);

                    int moved = taken + stored;
                    if (moved > 0) {
                        this.recordMove(ctx, moved);
                        SoundRegistry.MANAGE_CHEST.playGlobally(Location.of(chestPos, villager.level()), SoundSource.BLOCKS);
                    }

                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    ctx.getInitiator().setMotion(AnimationArchetype.IDLE);
                    return StepResult.transition(Stage.LOOP);
                })
                .build();
    }

    /**
     * Takes every demanded item this chest holds, up to the per-demand target count.
     * Returns the total number of items taken this leg.
     */
    private int takeDemandedItems(@Nonnull BaseVillager villager, @Nonnull IItemHandler handler, @Nonnull BlockPos chestPos) {
        List<ActiveDemand> demands = this.demandEvaluator.resolve(villager);
        int takenHere = 0;

        for (ActiveDemand demand : demands) {
            int extractedForDemand = 0;
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stackInSlot = handler.getStackInSlot(slot);
                if (stackInSlot.isEmpty() || !ItemMatches.test(demand.match(), stackInSlot)) {
                    continue;
                }

                int targetTakeCount = calculateTargetTakeCount(stackInSlot, demand.desiredCount());
                int needed = targetTakeCount - extractedForDemand;
                if (needed <= 0) {
                    break;
                }

                ItemStack extracted = handler.extractItem(slot, needed, false);
                if (extracted.isEmpty()) {
                    continue;
                }

                extractedForDemand += extracted.getCount();
                takenHere += extracted.getCount();
                villager.getSettlementsInventory().add(extracted);
                villager.setHeldItem(extracted.copyWithCount(1));
                this.recordTaken(extracted.getItem().toString(), extracted.getCount());

                if (extractedForDemand >= targetTakeCount) {
                    break;
                }
            }
        }

        return takenHere;
    }

    /**
     * Deposits every dumpable surplus into this chest, dumping only what the chest accepts.
     * Returns the total number of items stored this leg.
     */
    private int depositSurplus(@Nonnull BaseVillager villager, @Nonnull IItemHandler handler, @Nonnull BlockPos chestPos) {
        List<ActiveSupply> supplies = this.supplyEvaluator.resolve(villager);
        int storedHere = 0;

        for (ActiveSupply supply : supplies) {
            int targetCount = ChestInteractionCondition.computeDepositTargetCount(supply);
            if (targetCount <= 0) {
                continue;
            }

            ItemStack candidate = supply.representative().copyWithCount(targetCount);
            int acceptedCount = ChestInteractionCondition.simulateAcceptedCount(handler, candidate);
            if (acceptedCount <= 0) {
                continue;
            }

            ItemStack consumedStack = supply.representative().copyWithCount(acceptedCount);
            int consumed = villager.getSettlementsInventory().consume(consumedStack, acceptedCount);
            if (consumed <= 0) {
                continue;
            }

            ItemStack toInsert = supply.representative().copyWithCount(consumed);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
            if (!remainder.isEmpty()) {
                villager.getSettlementsInventory().add(remainder);
            }

            int inserted = consumed - remainder.getCount();
            if (inserted <= 0) {
                continue;
            }

            storedHere += inserted;
            villager.setHeldItem(toInsert.copyWithCount(1));
            this.recordStored(toInsert.getItem().toString(), inserted);
        }

        return storedHere;
    }

    private void recordTaken(@Nonnull String itemDescription, int count) {
        this.takenByItem.merge(itemDescription, count, Integer::sum);
        this.takenTotal += count;
    }

    private void recordStored(@Nonnull String itemDescription, int count) {
        this.storedByItem.merge(itemDescription, count, Integer::sum);
        this.storedTotal += count;
    }

    /**
     * Folds the latest move into the single {@code CHEST_MANAGED} deed, declaring it on first use.
     * The detail slots are rewritten from the running tallies each time, so the published event
     * always reflects the whole run's per-item breakdown per side.
     */
    private void recordMove(@Nonnull BehaviorContext<BaseVillager> ctx, int movedThisLeg) {
        BehaviorOutcome outcome = ctx.primaryDeed().orElseGet(() ->
                ctx.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.CHEST_MANAGED, null)));
        this.writeDetailFields(outcome);
        outcome.recordYield(movedThisLeg);
    }

    private void writeDetailFields(@Nonnull BehaviorOutcome outcome) {
        if (this.takenTotal > 0) {
            outcome.putDetailField("takenItems", formatItemList(this.takenByItem));
        }
        if (this.storedTotal > 0) {
            outcome.putDetailField("storedItems", formatItemList(this.storedByItem));
        }
    }

    /**
     * Compact {@code "<count> <itemId>"} list, most-moved first, delimited by {@code "; "}
     * (e.g. {@code "4 minecraft:carrot; 3 minecraft:potato"})
     */
    private static String formatItemList(@Nonnull Map<String, Integer> byItem) {
        return byItem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining("; "));
    }

    private static int calculateTargetTakeCount(@Nonnull ItemStack stack, int desiredCount) {
        int itemAwareMinimum = Math.min(TAKE_AT_LEAST_COUNT, stack.getMaxStackSize());
        return Math.max(desiredCount, itemAwareMinimum);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        List<BlockPos> reachable = this.chestCondition.getReachableChests();
        if (reachable.isEmpty()) {
            this.requestStop("No reachable village chests with logistics work found");
            return;
        }

        this.reachableChests.clear();
        this.reachableChests.addAll(reachable);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.reachableChests.clear();
        this.visited.clear();
        this.currentChestPos = null;
        this.takenByItem.clear();
        this.storedByItem.clear();
        this.takenTotal = 0;
        this.storedTotal = 0;
    }

}
