package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.ResetBlockStateObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.TemporaryArtifactHandle;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.SequencedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipe;
import dev.breezes.settlements.domain.smelting.catalog.BlastOreRecipeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class BlastOreBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final ClockTicks ITEM_INTERACTION_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks BLASTING_DURATION = ClockTicks.seconds(8);
    private static final ClockTicks SOOT_DURATION = ClockTicks.seconds(20);

    private static final int DAZE_STAR_EMIT_INTERVAL = 15;

    private static final int BATCH_SIZE = 8;

    private enum BlastStage implements StageKey {
        BLAST_ORE,
        END;
    }

    private final BlastOreRecipeRegistry recipeRegistry;

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final BlastRecipeAvailableCondition blastRecipeAvailableCondition;

    private final float explosionChance;
    private final ClockTicks dazeDuration;

    @Nullable
    private PhysicalBlock blastFurnace;
    @Nullable
    private BlastOreRecipe currentRecipe;
    @Nullable
    private Item currentInputItem;
    @Nullable
    private Item currentOutputItem;
    @Nullable
    private TemporaryArtifactHandle litHandle;

    private int currentBatchCount;
    private boolean misfired;

    public BlastOreBehavior(BlastOreConfig config,
                            BehaviorSupport support,
                            BlastOreRecipeRegistry recipeRegistry) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.recipeRegistry = recipeRegistry;
        this.explosionChance = config.explosionChance();
        this.dazeDuration = ClockTicks.seconds(config.dazeDurationSeconds());

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.BLAST_FURNACE), 1);
        this.blastRecipeAvailableCondition = new BlastRecipeAvailableCondition(this.recipeRegistry);
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.blastRecipeAvailableCondition);

        // Initialize variables
        this.blastFurnace = null;
        this.currentRecipe = null;
        this.currentInputItem = null;
        this.currentOutputItem = null;
        this.currentBatchCount = 0;
        this.litHandle = null;
        this.misfired = false;

        this.initializeStateMachine(this.createControlStep(), BlastStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("BlastOreBehavior")
                .initialStage(BlastStage.BLAST_ORE)
                .stageStepMap(Map.of(
                        BlastStage.BLAST_ORE, this.createBlastStep()
                ))
                .nextStage(BlastStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.misfired = false;

        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop("No blast furnace block found at job site");
            return;
        }
        this.blastFurnace = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        List<BlastOreRecipe> validRecipes = this.blastRecipeAvailableCondition.getValidRecipes();
        if (validRecipes.isEmpty()) {
            this.requestStop("No valid blast-ore recipes available");
            return;
        }

        BlastOreRecipe recipe = RandomUtil.choice(validRecipes).orElseThrow();

        // Resolved once per run rather than per tick — the villager's registry lookup result cannot
        // change mid-run, and every subsequent step (consume, held item, banking, deed) needs it.
        Item inputItem = BuiltInRegistries.ITEM.get(recipe.input());
        Item outputItem = BuiltInRegistries.ITEM.get(recipe.output());
        if (inputItem == Items.AIR || outputItem == Items.AIR) {
            this.requestStop("Blast-ore recipe references an item id that is not registered");
            return;
        }

        // Integer floor division is intentional: partial batches are allowed, but a partial ore is not.
        // For the shipped 1:1 recipes inputCount == 1, so this reduces to min(BATCH_SIZE, available).
        int available = entity.getSettlementsInventory().count(inputItem);
        int batch = Math.min(BATCH_SIZE, available / recipe.inputCount());
        if (batch < 1) {
            this.requestStop("Not enough ore in inventory to smelt a full batch");
            return;
        }

        this.currentRecipe = recipe;
        this.currentInputItem = inputItem;
        this.currentOutputItem = outputItem;
        this.currentBatchCount = batch;

        // Settle the entire economic outcome up front — consume the ore, bank the ingots, record the
        // deeds, decide the misfire, and pay experience — before any navigation or animation runs.
        // Everything afterward is pure theatre replaying a decided result, so an unreachable furnace or
        // an interrupted animation can never strand consumed ore or skip a reward that already happened.
        if (!this.commitSmelt(entity, context)) {
            this.requestStop("Blast-ore batch could not be consumed from inventory");
            return;
        }

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.blastFurnace)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.blastFurnace != null && this.blastFurnace.is(Blocks.BLAST_FURNACE);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.misfired = false;

        // Lit reset is handled by teardownAll() via the tracked obligation; clear the handle
        // for next-run reuse (this behavior instance is reused across runs).
        this.litHandle = null;
        this.blastFurnace = null;
        this.currentRecipe = null;
        this.currentBatchCount = 0;
        this.currentInputItem = null;
        this.currentOutputItem = null;
    }

    private BehaviorStep<BaseVillager> createBlastStep() {
        TimeBasedStep<BaseVillager> setup = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.currentInputItem == null || this.blastFurnace == null) {
                        return StepResult.complete();
                    }

                    // The ore was already consumed in onBehaviorStart; this only mimes loading the furnace.
                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    ctx.getInitiator().setHeldItem(new ItemStack(this.currentInputItem, this.currentBatchCount));
                    villager.triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.blastFurnace == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().clearHeldItem();
                    this.setFurnaceLitState(true);

                    BlockPos furnacePos = this.blastFurnace.getLocation(false).toBlockPos();
                    ResourceLocation furnaceBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.BLAST_FURNACE);
                    this.litHandle = ctx.getTeardownScope().track(
                            new ResetBlockStateObligation(furnacePos, furnaceBlockId, "lit", "false"));

                    Location location = this.blastFurnace.getLocation(true).add(0, 0.5, 0, false);
                    location.displayParticles(ParticleTypes.LAVA, 5, 0.3, 0.3, 0.3, 0.1);
                    SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> blasting = TimeBasedStep.<BaseVillager>builder()
                .withTickable(BLASTING_DURATION.asTickable())
                .onEnd(ctx -> {
                    if (this.blastFurnace == null || this.currentRecipe == null || this.currentOutputItem == null) {
                        return StepResult.complete();
                    }

                    if (this.litHandle != null) {
                        this.litHandle.dispose(ctx.getLevel());
                        this.litHandle = null;
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();

                    // Resolve furnace top location here before entering the misfire branch so
                    // neither branch has to handle a potential null dereference separately
                    Location furnaceTop = this.blastFurnace.getLocation(true).add(0, 0.5, 0, false);

                    if (this.misfired) {
                        // Smoke puff at the furnace
                        furnaceTop.displayParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, 100, 3, 1.5, 3, 0.1);
                        furnaceTop.displayParticles(ParticleTypes.LARGE_SMOKE, 20, 0.4, 0.4, 0.4, 0.02);
                        furnaceTop.displayParticles(ParticleTypes.EXPLOSION, 4, 0.4, 0.4, 0.4, 0.1);

                        Location face = Location.fromEntity(villager, true);
                        face.displayParticles(ParticleTypes.ANGRY_VILLAGER, 3, 0.1, 0.1, 0.1, 0.4);

                        SoundRegistry.BLAST_MISFIRE.playGlobally(face, SoundSource.BLOCKS);
                        villager.setSooty(SOOT_DURATION);

                        ctx.getInitiator().clearHeldItem();
                    } else {
                        ctx.getInitiator().setHeldItem(new ItemStack(this.currentOutputItem));
                        villager.triggerMotion(AnimationArchetype.INTERACT);
                        SoundRegistry.ITEM_POP_OUT.playGlobally(furnaceTop, SoundSource.BLOCKS);
                    }

                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> takeOut = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.complete();
                })
                .build();

        TimeBasedStep<BaseVillager> daze = TimeBasedStep.<BaseVillager>builder()
                .withTickable(this.dazeDuration.asTickable())
                .onStart(ctx -> {
                    if (!this.misfired) {
                        // Normal run — no daze needed; complete immediately to skip to the end
                        return StepResult.complete();
                    }

                    ctx.getInitiator().getMinecraftEntity().clearHeldItem();
                    return StepResult.noOp();
                })
                .addPeriodicStep(DAZE_STAR_EMIT_INTERVAL, ctx -> {
                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    // Spawn above eye level so the ring floats over the head rather than through it
                    Location starOrigin = Location.fromEntity(villager, true).add(0, 0.5, 0, false);
                    starOrigin.displayParticles(ParticleTypeRegistry.STUNNED_STAR.get(), 2, 0.0, 0.03, 0.0, 0.0);
                    return StepResult.noOp();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(new SequencedStep<>("BlastOreBehavior.sequence", List.of(setup, blasting, takeOut, daze)))
                .build();
    }

    /**
     * Runs the behavior's entire economic transaction in one shot: consume the ore, bank the smelted
     * output, record the primary (and, on a misfire, secondary) deed, decide the misfire, and gain experience.
     * <p>
     * Returns {@code false} without banking anything if the ore can no longer be consumed.
     */
    private boolean commitSmelt(@Nonnull BaseVillager villager,
                                @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.currentRecipe == null || this.currentInputItem == null || this.currentOutputItem == null) {
            return false;
        }

        if (!this.consumeRecipeInput(villager, this.currentRecipe)) {
            return false;
        }

        int outputTotal = this.currentRecipe.outputCount() * this.currentBatchCount;
        villager.getSettlementsInventory().add(new ItemStack(this.currentOutputItem, outputTotal));

        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null);
        outcome.putDetailField("item", BuiltInRegistries.ITEM.getKey(this.currentOutputItem).getPath());
        outcome.putDetailField("count", Integer.toString(outputTotal));
        outcome.markSucceeded();
        context.declarePrimaryDeed(outcome);

        this.misfired = RandomUtil.chance(this.explosionChance);
        if (this.misfired) {
            context.addSecondaryDeed(BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null)).markSucceeded();
        }

        this.rewardExperience(villager);
        return true;
    }

    private boolean consumeRecipeInput(@Nonnull BaseVillager villager,
                                       @Nonnull BlastOreRecipe recipe) {
        if (this.currentInputItem == null) {
            return false;
        }

        int requested = recipe.inputCount() * this.currentBatchCount;
        VillagerInventory inventory = villager.getSettlementsInventory();
        if (inventory.count(this.currentInputItem) < requested) {
            return false;
        }
        return inventory.consume(this.currentInputItem, requested) == requested;
    }

    private void setFurnaceLitState(boolean lit) {
        if (this.blastFurnace == null) {
            return;
        }

        Level level = this.blastFurnace.getLevel();
        BlockPos pos = this.blastFurnace.getLocation(false).toBlockPos();

        BlockState currentState = level.getBlockState(pos);
        if (!currentState.is(Blocks.BLAST_FURNACE)) {
            log.behaviorTrace("Skipping furnace lit state update at {} because block is no longer a blast furnace", pos);
            return;
        }
        if (!currentState.hasProperty(AbstractFurnaceBlock.LIT)) {
            log.behaviorTrace("Skipping furnace lit state update at {} because block state has no LIT property", pos);
            return;
        }
        if (currentState.getValue(AbstractFurnaceBlock.LIT) == lit) {
            return;
        }

        log.behaviorTrace("Setting furnace lit state to {}", lit);
        BlockState newState = currentState.setValue(AbstractFurnaceBlock.LIT, lit);
        level.setBlock(pos, newState, BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

}
