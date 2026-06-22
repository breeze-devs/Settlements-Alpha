package dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat;

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
import dev.breezes.settlements.application.hunger.HungerConfig;
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
public class SmokeMeatBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final ClockTicks ITEM_INTERACTION_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks SMOKING_DURATION = ClockTicks.seconds(6);

    private static final List<SmokeMeatRecipe> RECIPES = List.of(
            SmokeMeatRecipe.builder().input(Items.BEEF).output(Items.COOKED_BEEF).build(),
            SmokeMeatRecipe.builder().input(Items.PORKCHOP).output(Items.COOKED_PORKCHOP).build(),
            SmokeMeatRecipe.builder().input(Items.CHICKEN).output(Items.COOKED_CHICKEN).build(),
            SmokeMeatRecipe.builder().input(Items.MUTTON).output(Items.COOKED_MUTTON).build(),
            SmokeMeatRecipe.builder().input(Items.RABBIT).output(Items.COOKED_RABBIT).build(),
            SmokeMeatRecipe.builder().input(Items.COD).output(Items.COOKED_COD).build(),
            SmokeMeatRecipe.builder().input(Items.SALMON).output(Items.COOKED_SALMON).build());

    private enum SmokeStage implements StageKey {
        SMOKE_MEAT,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final SmokeRecipeAvailableCondition smokeRecipeAvailableCondition;

    @Nullable
    private PhysicalBlock smoker;
    @Nullable
    private SmokeMeatRecipe currentRecipe;
    @Nullable
    private TemporaryArtifactHandle litHandle;

    public SmokeMeatBehavior(SmokeMeatConfig config,
                             HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.SMOKER), 1);
        this.smokeRecipeAvailableCondition = new SmokeRecipeAvailableCondition(RECIPES);
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.smokeRecipeAvailableCondition);

        // Initialize variables
        this.smoker = null;
        this.currentRecipe = null;
        this.litHandle = null;

        this.initializeStateMachine(this.createControlStep(), SmokeStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("SmokeMeatBehavior")
                .initialStage(SmokeStage.SMOKE_MEAT)
                .stageStepMap(Map.of(
                        SmokeStage.SMOKE_MEAT, this.createSmokeStep()
                ))
                .nextStage(SmokeStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop("No smoker block found at job site");
            return;
        }

        this.smoker = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        List<SmokeMeatRecipe> validRecipes = this.smokeRecipeAvailableCondition.getValidRecipes();
        if (validRecipes.isEmpty()) {
            this.requestStop("No valid smoke recipes available");
            return;
        }

        this.currentRecipe = RandomUtil.choice(validRecipes).orElseThrow();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.smoker)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.smoker != null && this.smoker.is(Blocks.SMOKER);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        // Lit reset is handled by teardownAll() via the tracked obligation; clear the handle
        // for next-run reuse (this behavior instance is reused across runs).
        this.litHandle = null;
        this.smoker = null;
        this.currentRecipe = null;
    }

    private BehaviorStep<BaseVillager> createSmokeStep() {
        TimeBasedStep<BaseVillager> setup = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.currentRecipe == null || this.smoker == null) {
                        return StepResult.complete();
                    }

                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    if (!this.consumeRecipeInput(villager, this.currentRecipe)) {
                        return StepResult.fail("SMOKE_MEAT_INPUT_CONSUME_FAILED");
                    }

                    ctx.getInitiator().setHeldItem(this.currentRecipe.createInputStack());
                    villager.triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.smoker == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().clearHeldItem();
                    this.setSmokerLitState(true);

                    // Track a crash-recovery obligation for the lit state.  The smoking step's
                    // onEnd will dispose this handle when it sets lit back to false.  If the
                    // server crashes in the window between now and that disposal, the reconciler
                    // will reset lit on the next reload.
                    BlockPos smokerPos = this.smoker.getLocation(false).toBlockPos();
                    ResourceLocation smokerBlockId = BuiltInRegistries.BLOCK.getKey(Blocks.SMOKER);
                    this.litHandle = ctx.getTeardownScope().track(
                            new ResetBlockStateObligation(smokerPos, smokerBlockId, "lit", "false"));

                    Location location = this.smoker.getLocation(true).add(0, 0.5, 0, false);
                    location.displayParticles(ParticleTypes.SMOKE, 6, 0.3, 0.3, 0.3, 0.02);
                    SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> smoking = TimeBasedStep.<BaseVillager>builder()
                .withTickable(SMOKING_DURATION.asTickable())
                .onEnd(ctx -> {
                    if (this.smoker == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    if (this.litHandle != null) {
                        this.litHandle.dispose(ctx.getLevel());
                        this.litHandle = null;
                    }
                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    ItemStack outputStack = this.currentRecipe.createOutputStack();
                    villager.getSettlementsInventory().add(outputStack);

                    BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.MEAT_SMOKED, null);
                    outcome.recordDeedDetail(outputStack.getItem().toString());
                    outcome.markSucceeded();
                    ctx.declarePrimaryDeed(outcome);

                    ctx.getInitiator().setHeldItem(outputStack);
                    villager.triggerMotion(AnimationArchetype.INTERACT);
                    SoundRegistry.ITEM_POP_OUT.playGlobally(this.smoker.getLocation(true).add(0, 0.5, 0, false), SoundSource.BLOCKS);
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

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(new SequencedStep<>("SmokeMeatBehavior.sequence", List.of(setup, smoking, takeOut)))
                .build();
    }

    private boolean consumeRecipeInput(@Nonnull BaseVillager villager,
                                       @Nonnull SmokeMeatRecipe recipe) {
        return villager.getSettlementsInventory().consume(recipe.getInput(), recipe.getInputCount()) == recipe.getInputCount();
    }

    private void setSmokerLitState(boolean lit) {
        if (this.smoker == null) {
            return;
        }

        Level level = this.smoker.getLevel();
        BlockPos pos = this.smoker.getLocation(false).toBlockPos();

        BlockState currentState = level.getBlockState(pos);
        if (!currentState.is(Blocks.SMOKER)) {
            log.behaviorTrace("Skipping smoker lit state update at {} because block is no longer a smoker", pos);
            return;
        }
        if (!currentState.hasProperty(AbstractFurnaceBlock.LIT)) {
            log.behaviorTrace("Skipping smoker lit state update at {} because block state has no LIT property", pos);
            return;
        }
        if (currentState.getValue(AbstractFurnaceBlock.LIT) == lit) {
            return;
        }

        log.behaviorTrace("Setting smoker lit state to {}", lit);
        BlockState newState = currentState.setValue(AbstractFurnaceBlock.LIT, lit);
        level.setBlock(pos, newState, BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

}
