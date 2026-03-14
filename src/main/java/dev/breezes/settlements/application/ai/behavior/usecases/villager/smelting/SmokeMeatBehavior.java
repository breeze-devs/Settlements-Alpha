package dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
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
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorDescriptor;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.BaseContainerBlockEntityMixin;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.LockCode;
import net.minecraft.world.item.Item;
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

@CustomLog
public class SmokeMeatBehavior extends StateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final Ticks ITEM_INTERACTION_DURATION = Ticks.seconds(1);
    private static final Ticks SMOKING_DURATION = Ticks.seconds(6);

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
    @Getter
    private final BehaviorDescriptor behaviorDescriptor;

    @Nullable
    private PhysicalBlock smoker;
    @Nullable
    private SmokeMeatRecipe currentRecipe;

    public SmokeMeatBehavior(SmokeMeatConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));
        this.behaviorDescriptor = BehaviorDescriptor.builder()
                .displayNameKey("ui.settlements.behavior.behavior.smoke_meat")
                .iconItemId(ResourceLocation.withDefaultNamespace("smoker"))
                .displaySuffix(null)
                .build();

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.SMOKER));
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        // Initialize variables
        this.smoker = null;
        this.currentRecipe = null;

        this.initializeStateMachine(this.createControlStep(), SmokeStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
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
                                   @Nonnull BehaviorContext context) {
        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop();
            return;
        }

        this.smoker = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        this.currentRecipe = RandomUtil.choice(RECIPES);
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.smoker)));

        this.setSmokerLockState(true);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return this.smoker != null && this.smoker.is(Blocks.SMOKER);
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
        if (this.smoker != null) {
            entity.getLookControl().setLookAt(this.smoker.getLocation(false).toVec3());
        }
        return super.tickContinueConditions(delta, world, entity);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        this.setSmokerLitState(false);
        villager.clearHeldItem();
        this.setSmokerLockState(false);

        this.smoker = null;
        this.currentRecipe = null;
    }

    private BehaviorStep createSmokeStep() {
        TimeBasedStep setup = TimeBasedStep.builder()
                .withTickable(ITEM_INTERACTION_DURATION.asTickable())
                .onStart(ctx -> {
                    if (this.currentRecipe == null || this.smoker == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().setHeldItem(this.currentRecipe.getInput().getDefaultInstance());
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.smoker == null) {
                        return StepResult.complete();
                    }

                    ctx.getInitiator().clearHeldItem();
                    this.setSmokerLitState(true);
                    Location location = this.smoker.getLocation(true).add(0, 0.5, 0, false);
                    location.displayParticles(ParticleTypes.SMOKE, 6, 0.3, 0.3, 0.3, 0.02);
                    SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep smoking = TimeBasedStep.builder()
                .withTickable(SMOKING_DURATION.asTickable())
                .onEnd(ctx -> {
                    if (this.smoker == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    this.setSmokerLitState(false);
                    ctx.getInitiator().setHeldItem(this.currentRecipe.getOutput().getDefaultInstance());
                    SoundRegistry.ITEM_POP_OUT.playGlobally(this.smoker.getLocation(true).add(0, 0.5, 0, false), SoundSource.BLOCKS);
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
                .actionStep(new SequencedStep("SmokeMeatBehavior.sequence", List.of(setup, smoking, takeOut)))
                .build();
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

    private void setSmokerLockState(boolean locked) {
        if (this.smoker == null) {
            return;
        }

        BlockEntity blockEntity = this.smoker.getLevel().getBlockEntity(this.smoker.getLocation(false).toBlockPos());
        if (!(blockEntity instanceof BaseContainerBlockEntityMixin lockableContainer)) {
            log.behaviorTrace("Skipping smoker lock state update because block entity is not lockable");
            return;
        }

        String lockKey = locked ? GeneralConfig.globalLockKey : "";
        lockableContainer.setLockKey(new LockCode(lockKey));
    }

    @Builder
    @Getter
    private static class SmokeMeatRecipe {

        private final Item input;
        private final Item output;
        // TODO: when implementing inventory, we can add counts here

    }

}