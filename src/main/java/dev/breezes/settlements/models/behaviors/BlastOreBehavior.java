package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.annotations.configurations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.GeneralConfig;
import dev.breezes.settlements.configurations.constants.BehaviorConfigConstants;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.mixins.BaseContainerBlockEntityMixin;
import dev.breezes.settlements.models.blocks.BlockFlag;
import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.models.misc.ITickable;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
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

@CustomLog
public class BlastOreBehavior extends AbstractInteractAtTargetBehavior {

    private static final int NAVIGATE_STOP_DISTANCE = 1;
    private static final double INTERACTION_DISTANCE = 2.0;

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
                    .build()
    );

    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 10, min = 1)
    private static int preconditionCheckCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 20, min = 1)
    private static int preconditionCheckCooldownMax;

    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MIN_DESCRIPTION,
            defaultValue = 30, min = 1)
    private static int behaviorCooldownMin;
    @IntegerConfig(identifier = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_IDENTIFIER,
            description = BehaviorConfigConstants.BEHAVIOR_COOLDOWN_MAX_DESCRIPTION,
            defaultValue = 90, min = 1)
    private static int behaviorCooldownMax;

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;
    private final ITickable itemInteractionTickable;
    private final ITickable blastingTickable;

    @Nonnull
    private BehaviorState behaviorState;
    @Nullable
    private PhysicalBlock blastFurnace;
    @Nullable
    private BlastOreRecipe currentRecipe;

    public BlastOreBehavior() {
        super(log,
                RandomRangeTickable.of(Ticks.seconds(preconditionCheckCooldownMin), Ticks.seconds(preconditionCheckCooldownMax)),
                RandomRangeTickable.of(Ticks.seconds(behaviorCooldownMin), Ticks.seconds(behaviorCooldownMax)),
                Tickable.of(Ticks.one()));

        // Create behavior preconditions
        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.BLAST_FURNACE));
        // TODO: we perhaps should check whether the furnace is currently in use?
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        this.itemInteractionTickable = Tickable.of(ITEM_INTERACTION_DURATION);
        this.blastingTickable = Tickable.of(BLASTING_DURATION);

        // Initialize variables
        this.behaviorState = BehaviorState.STANDBY;
        this.blastFurnace = null;
        this.currentRecipe = null;
    }

    @Override
    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.itemInteractionTickable.reset();
        this.blastingTickable.reset();
        this.behaviorState = BehaviorState.SETUP;

        this.blastFurnace = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        this.currentRecipe = RandomUtil.choice(RECIPES);

        this.setFurnaceLockState(true);
    }

    @Override
    protected void navigateToTarget(int delta, @Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().walkTo(this.blastFurnace.getLocation(false), NAVIGATE_STOP_DISTANCE);
    }

    @Override
    protected void interactWithTarget(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        Location location = this.blastFurnace.getLocation(true).add(0, 0.5, 0, false);
        switch (this.behaviorState) {
            case SETUP -> {
                villager.setHeldItem(this.currentRecipe.getInput().getDefaultInstance());
                this.behaviorState = BehaviorState.PUT_IN_FURNACE;
            }
            case PUT_IN_FURNACE -> {
                if (this.itemInteractionTickable.tickCheckAndReset(1)) {
                    villager.clearHeldItem();
                    this.setFurnaceLitState(true);
                    location.displayParticles(ParticleTypes.LAVA, 5, 0.3, 0.3, 0.3, 0.1);

                    SoundRegistry.ITEM_POP_IN.playGlobally(location, SoundSource.BLOCKS);
                    this.behaviorState = BehaviorState.BLASTING;
                }
            }
            case BLASTING -> {
                if (this.blastingTickable.tickCheckAndReset(1)) {
                    this.setFurnaceLitState(false);

                    villager.setHeldItem(this.currentRecipe.getOutput().getDefaultInstance());
                    SoundRegistry.ITEM_POP_OUT.playGlobally(location, SoundSource.BLOCKS);
                    this.behaviorState = BehaviorState.TAKE_FROM_FURNACE;
                }
            }
            case TAKE_FROM_FURNACE -> {
                if (this.itemInteractionTickable.tickCheckAndReset(1)) {
                    villager.clearHeldItem();
                    this.requestStop();
                }
            }
        }
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        villager.getLookControl().setLookAt(this.blastFurnace.getLocation(false).toVec3());
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.blastFurnace != null && this.blastFurnace.is(Blocks.BLAST_FURNACE);
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return Location.fromEntity(villager, false).distanceSquared(this.blastFurnace.getLocation(false)) < INTERACTION_DISTANCE * INTERACTION_DISTANCE;
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.setFurnaceLockState(false);

        this.itemInteractionTickable.reset();
        this.blastingTickable.reset();

        this.behaviorState = BehaviorState.STANDBY;
        this.blastFurnace = null;
        this.currentRecipe = null;
    }

    private void setFurnaceLitState(boolean lit) {
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
         * Putting the input block into the furnace
         */
        PUT_IN_FURNACE,

        /**
         * Blasting the ore
         */
        BLASTING,

        /**
         * Taking the output block from the furnace
         */
        TAKE_FROM_FURNACE;

    }

}
