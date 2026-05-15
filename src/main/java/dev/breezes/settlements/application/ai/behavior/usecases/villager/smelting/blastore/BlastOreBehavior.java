package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
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
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.BaseContainerBlockEntityMixin;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class BlastOreBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final ClockTicks ITEM_INTERACTION_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks BLASTING_DURATION = ClockTicks.seconds(8);

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

    @Nullable
    private PhysicalBlock blastFurnace;
    @Nullable
    private BlastOreRecipe currentRecipe;

    public BlastOreBehavior(BlastOreConfig config,
                            HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.BLAST_FURNACE));
        this.blastRecipeAvailableCondition = new BlastRecipeAvailableCondition(RECIPES);
        // TODO: we perhaps should check whether the furnace is currently in use?
        this.preconditions.add(this.jobSiteBlockExistsCondition);
        this.preconditions.add(this.blastRecipeAvailableCondition);

        // Initialize variables
        this.blastFurnace = null;
        this.currentRecipe = null;

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

        this.currentRecipe = RandomUtil.choice(validRecipes);

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.blastFurnace)));

        this.setFurnaceLockState(true);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.blastFurnace != null && this.blastFurnace.is(Blocks.BLAST_FURNACE);
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        if (this.blastFurnace != null) {
            entity.getLookControl().setLookAt(this.blastFurnace.getLocation(false).toVec3());
        }
        return super.tickContinueConditions(delta, world, entity);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        this.setFurnaceLitState(false);
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);
        this.setFurnaceLockState(false);

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
                    ctx.getInitiator().getMinecraftEntity().setMotion(AnimationArchetype.IDLE);
                    this.setFurnaceLitState(true);
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

                    this.setFurnaceLitState(false);
                    BaseVillager villager = ctx.getInitiator().getMinecraftEntity();
                    this.storeOrDropOutput(villager, this.currentRecipe);

                    ctx.getInitiator().setHeldItem(this.currentRecipe.createOutputStack());
                    villager.triggerMotion(AnimationArchetype.INTERACT);
                    SoundRegistry.ITEM_POP_OUT.playGlobally(this.blastFurnace.getLocation(true).add(0, 0.5, 0, false), SoundSource.BLOCKS);
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
                .navigateStep(new NavigateToTargetStep<>(0.5f, 1))
                .actionStep(new SequencedStep<>("BlastOreBehavior.sequence", List.of(setup, blasting, takeOut)))
                .build();
    }

    private boolean consumeRecipeInput(@Nonnull BaseVillager villager,
                                       @Nonnull BlastOreRecipe recipe) {
        return villager.getSettlementsInventory().consume(recipe.getInput(), recipe.getInputCount()) == recipe.getInputCount();
    }

    private void storeOrDropOutput(@Nonnull BaseVillager villager,
                                   @Nonnull BlastOreRecipe recipe) {
        Optional<ItemStack> leftover = villager.getSettlementsInventory().addItem(recipe.createOutputStack());
        leftover.ifPresent(stack -> this.dropItemNearBlastFurnace(villager, stack));
    }

    private void dropItemNearBlastFurnace(@Nonnull BaseVillager villager,
                                          @Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        Location dropLocation = this.blastFurnace != null
                ? this.blastFurnace.getLocation(true).add(0, 0.5, 0, false)
                : Location.fromEntity(villager, false);
        Level level = villager.level();

        ItemEntity itemEntity = new ItemEntity(level,
                dropLocation.getX(),
                dropLocation.getY(),
                dropLocation.getZ(),
                stack.copy());
        level.addFreshEntity(itemEntity);
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

    private void setFurnaceLockState(boolean locked) {
        if (this.blastFurnace == null) {
            return;
        }

        BlockEntity blockEntity = this.blastFurnace.getLevel().getBlockEntity(this.blastFurnace.getLocation(false).toBlockPos());
        if (!(blockEntity instanceof BaseContainerBlockEntityMixin lockableContainer)) {
            log.behaviorTrace("Skipping furnace lock state update because block entity is not lockable");
            return;
        }

        String lockKey = locked ? GeneralConfig.globalLockKey : "";
        lockableContainer.setLockKey(new LockCode(lockKey));
    }

}
