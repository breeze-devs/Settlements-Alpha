package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.GeneralConfig;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.mixins.BaseContainerBlockEntityMixin;
import dev.breezes.settlements.models.behaviors.stages.StagedStep;
import dev.breezes.settlements.models.behaviors.states.BehaviorContext;
import dev.breezes.settlements.models.behaviors.states.registry.BehaviorStateType;
import dev.breezes.settlements.models.behaviors.states.registry.targets.TargetState;
import dev.breezes.settlements.models.behaviors.states.registry.targets.Targetable;
import dev.breezes.settlements.models.behaviors.steps.BehaviorStep;
import dev.breezes.settlements.models.behaviors.steps.SequencedStep;
import dev.breezes.settlements.models.behaviors.steps.StageKey;
import dev.breezes.settlements.models.behaviors.steps.StepResult;
import dev.breezes.settlements.models.behaviors.steps.TimeBasedStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.models.behaviors.steps.concrete.StayCloseStep;
import dev.breezes.settlements.models.blocks.BlockFlag;
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.LockCode;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class BlastOreBehavior extends StateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final Ticks ITEM_INTERACTION_DURATION = Ticks.seconds(1);
    private static final Ticks BLASTING_DURATION = Ticks.seconds(8);

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

    @Nullable
    private PhysicalBlock blastFurnace;
    @Nullable
    private BlastOreRecipe currentRecipe;

    public BlastOreBehavior(BlastOreConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.BLAST_FURNACE));
        // TODO: we perhaps should check whether the furnace is currently in use?
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        // Initialize variables
        this.blastFurnace = null;
        this.currentRecipe = null;

        this.initializeStateMachine(this.createControlStep(), BlastStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
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
                                   @Nonnull BehaviorContext context) {

        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop();
            return;
        }
        this.blastFurnace = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        this.currentRecipe = RandomUtil.choice(RECIPES);

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.blastFurnace)));

        this.setFurnaceLockState(true);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
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
        villager.clearHeldItem();
        this.setFurnaceLockState(false);

        this.blastFurnace = null;
        this.currentRecipe = null;
    }

    private BehaviorStep createBlastStep() {
        TimeBasedStep setup = TimeBasedStep.builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.currentRecipe == null || this.blastFurnace == null) {
                        return StepResult.complete();
                    }
                    ctx.getInitiator().setHeldItem(this.currentRecipe.getInput().getDefaultInstance());
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.blastFurnace == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().clearHeldItem();
                    this.setFurnaceLitState(true);
                    Location location = this.blastFurnace.getLocation(true).add(0, 0.5, 0, false);
                    location.displayParticles(ParticleTypes.LAVA, 5, 0.3, 0.3, 0.3, 0.1);
                    SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep blasting = TimeBasedStep.builder()
                .withTickable(BLASTING_DURATION.asTickable())
                .onEnd(ctx -> {
                    if (this.blastFurnace == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    this.setFurnaceLitState(false);
                    ctx.getInitiator().setHeldItem(this.currentRecipe.getOutput().getDefaultInstance());
                    SoundRegistry.ITEM_POP_OUT.playGlobally(this.blastFurnace.getLocation(true).add(0, 0.5, 0, false), SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep takeOut = TimeBasedStep.builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep(0.5f, 1))
                .actionStep(new SequencedStep("BlastOreBehavior.sequence", List.of(setup, blasting, takeOut)))
                .build();
    }

    private void setFurnaceLitState(boolean lit) {
        if (this.blastFurnace == null) {
            return;
        }

        Level level = this.blastFurnace.getLevel();
        BlockPos pos = this.blastFurnace.getLocation(false).toBlockPos();

        AbstractFurnaceBlockEntity blockEntity = (AbstractFurnaceBlockEntity) level.getBlockEntity(pos);
        if (blockEntity == null) {
            log.error("BlockEntity is null");
            return;
        }

        log.behaviorTrace("Setting furnace lit state to {}", lit);
        BlockState newState = level.getBlockState(pos)
                .setValue(AbstractFurnaceBlock.LIT, lit);
        level.setBlock(pos, newState, BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

    private void setFurnaceLockState(boolean locked) {
        if (this.blastFurnace == null) {
            return;
        }

        BlockEntity blockEntity = this.blastFurnace.getLevel().getBlockEntity(this.blastFurnace.getLocation(false).toBlockPos());
        if (blockEntity == null) {
            log.error("BlockEntity is null");
            return;
        }

        String lockKey = locked ? GeneralConfig.globalLockKey : "";
        ((BaseContainerBlockEntityMixin) blockEntity).setLockKey(new LockCode(lockKey));
    }

    @Builder
    @Getter
    private static class BlastOreRecipe {

        private final Item input;
        private final Item output;
        // TODO: when implementing inventory, we can add counts here

    }

}
