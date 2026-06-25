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

    private static final List<BlastOreRecipe> RECIPES = List.of(
            BlastOreRecipe.builder()
                    .input(Items.RAW_IRON)
                    .output(Items.IRON_INGOT)
                    .build(),
            BlastOreRecipe.builder()
                    .input(Items.RAW_GOLD)
                    .output(Items.GOLD_INGOT)
                    .build(),
            BlastOreRecipe.builder()
                    .input(Items.RAW_COPPER)
                    .output(Items.COPPER_INGOT)
                    .build());

    private enum BlastStage implements StageKey {
        BLAST_ORE,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final BlastRecipeAvailableCondition blastRecipeAvailableCondition;

    private final float explosionChance;
    private final ClockTicks dazeDuration;

    @Nullable
    private PhysicalBlock blastFurnace;
    @Nullable
    private BlastOreRecipe currentRecipe;
    @Nullable
    private TemporaryArtifactHandle litHandle;

    private boolean misfired;

    public BlastOreBehavior(BlastOreConfig config,
                            BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), support,
                config.experienceReward());

        this.explosionChance = config.explosionChance();
        this.dazeDuration = ClockTicks.seconds(config.dazeDurationSeconds());

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.BLAST_FURNACE), 1);
        this.blastRecipeAvailableCondition = new BlastRecipeAvailableCondition(RECIPES);
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.blastRecipeAvailableCondition);

        // Initialize variables
        this.blastFurnace = null;
        this.currentRecipe = null;
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

        this.currentRecipe = RandomUtil.choice(validRecipes).orElseThrow();

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
    }

    private BehaviorStep<BaseVillager> createBlastStep() {
        TimeBasedStep<BaseVillager> setup = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.currentRecipe == null || this.blastFurnace == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    if (!this.consumeRecipeInput(villager, this.currentRecipe)) {
                        return StepResult.fail("BLAST_ORE_INPUT_CONSUME_FAILED");
                    }

                    ctx.getInitiator().setHeldItem(this.currentRecipe.createInputStack());
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
                    if (this.blastFurnace == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    if (this.litHandle != null) {
                        this.litHandle.dispose(ctx.getLevel());
                        this.litHandle = null;
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    // The ingot is always banked regardless of misfire — the gag is purely cosmetic
                    villager.getSettlementsInventory().add(this.currentRecipe.createOutputStack());

                    ctx.declarePrimaryDeed(BehaviorOutcome.forDeed(WorldEventType.ORE_SMELTED, null)).markSucceeded();

                    // Resolve furnace top location here before entering the misfire branch so
                    // neither branch has to handle a potential null dereference separately
                    Location furnaceTop = this.blastFurnace.getLocation(true).add(0, 0.5, 0, false);

                    if (RandomUtil.chance(this.explosionChance)) {
                        this.misfired = true;
                        ctx.addSecondaryDeed(BehaviorOutcome.forDeed(WorldEventType.FURNACE_MISFIRED, null)).markSucceeded();

                        // Smoke puff at the furnace
                        furnaceTop.displayParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, 100, 3, 1.5, 3, 0.1);
                        furnaceTop.displayParticles(ParticleTypes.LARGE_SMOKE, 20, 0.4, 0.4, 0.4, 0.02);
                        furnaceTop.displayParticles(ParticleTypes.EXPLOSION, 4, 0.4, 0.4, 0.4, 0.1);

                        Location face = Location.fromEntity(villager, true);
                        face.displayParticles(ParticleTypes.ANGRY_VILLAGER, 3, 0.1, 0.1, 0.1, 0.4);

                        SoundRegistry.BLAST_MISFIRE.playGlobally(face, SoundSource.BLOCKS);
                        villager.setSooty(SOOT_DURATION);

                        // Skip the normal hand-off; the ingot is already in inventory
                        ctx.getInitiator().clearHeldItem();
                    } else {
                        ctx.getInitiator().setHeldItem(this.currentRecipe.createOutputStack());
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
                    this.rewardExperience(ctx.getInitiator().getMinecraftEntity());
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

    private boolean consumeRecipeInput(@Nonnull BaseVillager villager,
                                       @Nonnull BlastOreRecipe recipe) {
        return villager.getSettlementsInventory().consume(recipe.getInput(), recipe.getInputCount()) == recipe.getInputCount();
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
