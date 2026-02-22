package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.entities.villager.BaseVillager;
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
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.particles.ParticleRegistry;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class CutStoneBehavior extends StateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final double ANIMATION_OFFSET = 0.6;
    private static final Ticks ANIMATION_HALF_DURATION = Ticks.seconds(1.5);

    private static final List<CutStoneRecipe> RECIPES = List.of(
            CutStoneRecipe.builder()
                    .input(Blocks.SMOOTH_STONE.defaultBlockState())
                    .output(Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE))
                    .build()
    );

    private enum CutStage implements StageKey {
        CUT_STONE,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    @Nullable
    private PhysicalBlock stoneCutter;

    @Nullable
    private CutStoneRecipe currentRecipe;
    @Nullable
    private TransformedBlockDisplay initialBlockDisplay;
    @Nullable
    private TransformedBlockDisplay finalBlockDisplay;

    @Nullable
    private TransformationMatrix initialMatrix;
    private final TransformationMatrix intermediateMatrix; // always at (0, 0)
    @Nullable
    private TransformationMatrix finalMatrix;

    public CutStoneBehavior(CutStoneConfig config) {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(config.preconditionCheckCooldownMin()),
                        Ticks.seconds(config.preconditionCheckCooldownMax())),
                RandomRangeTickable.of(Ticks.seconds(config.behaviorCooldownMin()),
                        Ticks.seconds(config.behaviorCooldownMax())));

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.STONECUTTER));
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        // Initialize variables
        this.stoneCutter = null;
        this.currentRecipe = null;
        this.initialBlockDisplay = null;
        this.finalBlockDisplay = null;
        this.initialMatrix = null;
        this.intermediateMatrix = this.getMatrix(0, 0);
        this.finalMatrix = null;

        this.initializeStateMachine(this.createControlStep(), CutStage.END);
    }

    protected StagedStep createControlStep() {
        return StagedStep.builder()
                .name("CutStoneBehavior")
                .initialStage(CutStage.CUT_STONE)
                .stageStepMap(Map.of(CutStage.CUT_STONE, this.createCutStep()))
                .nextStage(CutStage.END)
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

        this.stoneCutter = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.stoneCutter)));

        this.currentRecipe = RandomUtil.choice(RECIPES);
        Direction direction = this.stoneCutter.getBlockState().getValue(StonecutterBlock.FACING);
        Direction.Axis axis = direction.getAxis();
        int axisDirection = direction.getAxisDirection().getStep();

        if (axis == Direction.Axis.Z) {
            this.initialMatrix = this.getMatrix(-axisDirection * ANIMATION_OFFSET, 0);
            this.finalMatrix = this.getMatrix(axisDirection * ANIMATION_OFFSET, 0);
        } else if (axis == Direction.Axis.X) {
            this.initialMatrix = this.getMatrix(0, -axisDirection * ANIMATION_OFFSET);
            this.finalMatrix = this.getMatrix(0, axisDirection * ANIMATION_OFFSET);
        } else {
            log.error("Unexpected axis '{}' for stone-cutter block at location {}", axis, this.stoneCutter.getLocation(false));
            this.requestStop();
            return;
        }

        this.initialBlockDisplay = new TransformedBlockDisplay(this.currentRecipe.getInput(), initialMatrix, true);
        this.finalBlockDisplay = new TransformedBlockDisplay(this.currentRecipe.getOutput(), intermediateMatrix, true);
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext context) {
        return this.stoneCutter != null && this.stoneCutter.is(Blocks.STONECUTTER);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();

        if (this.initialBlockDisplay != null) {
            this.initialBlockDisplay.remove();
        }
        if (this.finalBlockDisplay != null) {
            this.finalBlockDisplay.remove();
        }

        this.stoneCutter = null;
        this.currentRecipe = null;
        this.initialBlockDisplay = null;
        this.finalBlockDisplay = null;
        this.initialMatrix = null;
        this.finalMatrix = null;
    }

    private BehaviorStep createCutStep() {
        TimeBasedStep setup = TimeBasedStep.builder()
                .withTickable(Ticks.one().asTickable())
                .onEnd(ctx -> {
                    if (this.stoneCutter == null || this.initialBlockDisplay == null) {
                        return StepResult.complete();
                    }

                    Location location = this.stoneCutter.getLocation(true).add(0, 0.5, 0, false);
                    this.initialBlockDisplay.spawn(location);
                    this.initialBlockDisplay.setTransformation(this.intermediateMatrix, ANIMATION_HALF_DURATION);
                    SoundRegistry.STONE_CUTTER_WORKING.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep initialCut = TimeBasedStep.builder()
                .withTickable(ANIMATION_HALF_DURATION.asTickable())
                .everyTick(ctx -> {
                    if (this.stoneCutter == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    Location location = this.stoneCutter.getLocation(true).add(0, 0.5, 0, false);
                    ParticleRegistry.cutBlock(location, this.currentRecipe.getInput());
                    SoundRegistry.STONE_CUTTER_WORKING.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep transition = TimeBasedStep.builder()
                .withTickable(Ticks.one().asTickable())
                .onEnd(ctx -> {
                    if (this.stoneCutter == null || this.finalBlockDisplay == null || this.finalMatrix == null) {
                        return StepResult.complete();
                    }

                    if (this.initialBlockDisplay != null) {
                        this.initialBlockDisplay.remove();
                    }

                    Location location = this.stoneCutter.getLocation(true).add(0, 0.5, 0, false);
                    this.finalBlockDisplay.spawn(location);
                    this.finalBlockDisplay.setTransformation(this.finalMatrix, ANIMATION_HALF_DURATION);
                    SoundRegistry.STONE_CUTTER_WORKING.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep finalCut = TimeBasedStep.builder()
                .withTickable(ANIMATION_HALF_DURATION.asTickable())
                .everyTick(ctx -> {
                    if (this.stoneCutter == null || this.currentRecipe == null) {
                        return StepResult.complete();
                    }

                    Location location = this.stoneCutter.getLocation(true).add(0, 0.5, 0, false);
                    ParticleRegistry.cutBlock(location, this.currentRecipe.getOutput());
                    SoundRegistry.STONE_CUTTER_WORKING.playGlobally(location, SoundSource.BLOCKS);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.complete())
                .build();

        SequencedStep sequence = new SequencedStep("CutStoneBehavior.sequence",
                List.of(setup, initialCut, transition, finalCut));

        return StayCloseStep.builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep(0.5f, 1))
                .actionStep(sequence)
                .build();
    }

    private TransformationMatrix getMatrix(double dx, double dz) {
        return new TransformationMatrix(
                0.6f, 0.0f, 0.0f, (float) (-0.3f + dx),
                0.0f, 0.6f, 0.0f, 0.1f,
                0.0f, 0.0f, 0.6f, (float) (-0.3f + dz),
                0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Builder
    @Getter
    private static class CutStoneRecipe {

        private final BlockState input;
        private final BlockState output;
        // TODO: when implementing inventory, we can add counts here

    }

}
