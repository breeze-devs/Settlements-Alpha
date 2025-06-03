package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.particles.ParticleRegistry;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.RandomUtil;
import dev.breezes.settlements.util.Ticks;
import lombok.Builder;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@CustomLog
public class CutStoneBehavior extends AbstractInteractAtTargetBehavior {

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 2.0;

    private static final double ANIMATION_OFFSET = 0.6;
    private static final Ticks ANIMATION_HALF_DURATION = Ticks.seconds(1.5);

    private static final List<CutStoneRecipe> RECIPES = List.of(
            CutStoneRecipe.builder()
                    .input(Blocks.SMOOTH_STONE.defaultBlockState())
                    .output(Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE))
                    .build()
    );

    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(type = ConfigurationType.BEHAVIOR,
            identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 60, min = 1)
    private static int behaviorCooldownMax;

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final ITickable animationTickable;

    @Nonnull
    private BehaviorState behaviorState;
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

    public CutStoneBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin), Ticks.seconds(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin), Ticks.seconds(behaviorCooldownMax)),
                Tickable.of(Ticks.one()));

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.STONECUTTER));
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        this.animationTickable = Tickable.of(ANIMATION_HALF_DURATION);

        // Initialize variables
        this.behaviorState = BehaviorState.STANDBY;
        this.stoneCutter = null;
        this.currentRecipe = null;
        this.initialBlockDisplay = null;
        this.finalBlockDisplay = null;
        this.initialMatrix = null;
        this.intermediateMatrix = this.getMatrix(0, 0);
        this.finalMatrix = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.animationTickable.reset();
        this.behaviorState = BehaviorState.SETUP;

        this.stoneCutter = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();

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
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.stoneCutter.getLocation(false).toBlockPos(),
                0.5F, NAVIGATE_STOP_DISTANCE));
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        Location location = this.stoneCutter.getLocation(true).add(0, 0.5, 0, false);
        switch (this.behaviorState) {
            case SETUP -> {
                this.initialBlockDisplay.spawn(location);
                this.initialBlockDisplay.setTransformation(this.intermediateMatrix, ANIMATION_HALF_DURATION);
                this.behaviorState = BehaviorState.INITIAL_CUT;
            }
            case INITIAL_CUT -> {
                ParticleRegistry.cutBlock(location, this.currentRecipe.getInput());
                if (this.animationTickable.tickCheckAndReset(1)) {
                    this.behaviorState = BehaviorState.TRANSITION;
                }
            }
            case TRANSITION -> {
                this.initialBlockDisplay.remove();
                this.finalBlockDisplay.spawn(location);
                this.finalBlockDisplay.setTransformation(this.finalMatrix, ANIMATION_HALF_DURATION);
                this.behaviorState = BehaviorState.FINAL_CUT;
            }
            case FINAL_CUT -> {
                ParticleRegistry.cutBlock(location, this.currentRecipe.getOutput());
                if (this.animationTickable.tickCheckAndReset(1)) {
                    this.requestStop();
                }
            }
        }
        SoundRegistry.STONE_CUTTER_WORKING.playGlobally(location, SoundSource.BLOCKS);
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        // Do nothing
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.stoneCutter != null && this.stoneCutter.is(Blocks.STONECUTTER);
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return Location.fromEntity(villager, false).distanceSquared(this.stoneCutter.getLocation(false)) < INTERACTION_DISTANCE * INTERACTION_DISTANCE;
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.initialBlockDisplay.remove();
        this.finalBlockDisplay.remove();

        this.animationTickable.reset();
        this.behaviorState = BehaviorState.STANDBY;
        this.stoneCutter = null;
        this.initialBlockDisplay = null;
        this.finalBlockDisplay = null;
        this.initialMatrix = null;
        this.finalMatrix = null;
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

    private enum BehaviorState {

        /**
         * Behavior not started
         */
        STANDBY,

        /**
         * Setting up the behavior
         */
        SETUP,

        /**
         * Displaying the initial cut (input block display)
         */
        INITIAL_CUT,

        /**
         * Swapping the input block display with the output block display
         */
        TRANSITION,

        /**
         * Displaying the final cut (output block display)
         */
        FINAL_CUT;

    }

}
