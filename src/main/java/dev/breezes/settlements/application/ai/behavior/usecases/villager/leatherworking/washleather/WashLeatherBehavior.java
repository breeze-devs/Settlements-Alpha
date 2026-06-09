package dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.washleather;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.teardown.DiscardEntityObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.RestoreBlockObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.TemporaryArtifactHandle;
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
import dev.breezes.settlements.domain.ai.conditions.JobSiteBlockExistsCondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.BlockFlag;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.TransformedItemDisplay;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@CustomLog
public class WashLeatherBehavior extends VillagerStateMachineBehavior {

    private static final double CLOSE_ENOUGH_DISTANCE = 2.0;

    private static final ClockTicks FILL_DURATION = ClockTicks.seconds(0.5);
    private static final ClockTicks HOLD_LEATHER_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks WASH_DURATION = ClockTicks.seconds(5);
    private static final ClockTicks REMOVE_LEATHER_DURATION = ClockTicks.seconds(1);
    private static final ClockTicks DRAIN_DURATION = ClockTicks.seconds(0.5);

    // Dirty water color (murky brown) particle color
    private static final int DIRTY_WATER_COLOR = 0xFF6F5A3D;

    private static final int WASH_EFFECT_INTERVAL_TICKS = 5;
    private static final int INTERACT_RETRIGGER_INTERVAL_TICKS = 20;

    private enum WashStage implements StageKey {
        WASH_LEATHER,
        END;
    }

    private final JobSiteBlockExistsCondition<BaseVillager> jobSiteBlockExistsCondition;

    @Nullable
    private PhysicalBlock cauldron;
    @Nullable
    private BlockPos cauldronPos;
    @Nullable
    private Location cauldronCenter;
    @Nullable
    private TransformedItemDisplay leatherDisplay;
    @Nullable
    private TemporaryArtifactHandle restoreHandle;
    @Nullable
    private TemporaryArtifactHandle leatherDisplayHandle;

    public WashLeatherBehavior(WashLeatherConfig config,
                               HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig,
                config.experienceReward());

        this.jobSiteBlockExistsCondition = new JobSiteBlockExistsCondition<>(block -> block != null && block.is(Blocks.CAULDRON));
        this.preconditions.add(this.jobSiteBlockExistsCondition);

        this.cauldron = null;
        this.cauldronPos = null;
        this.cauldronCenter = null;
        this.leatherDisplay = null;
        this.restoreHandle = null;
        this.leatherDisplayHandle = null;

        this.initializeStateMachine(this.createControlStep(), WashStage.END);
    }

    protected StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("WashLeatherBehavior")
                .initialStage(WashStage.WASH_LEATHER)
                .stageStepMap(Map.of(
                        WashStage.WASH_LEATHER, this.createWashStep()
                ))
                .nextStage(WashStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.jobSiteBlockExistsCondition.getJobSiteBlock().isEmpty()) {
            this.requestStop("No cauldron block found at job site");
            return;
        }

        this.cauldron = this.jobSiteBlockExistsCondition.getJobSiteBlock().get();
        this.cauldronPos = this.cauldron.getLocation(false).toBlockPos();
        // Rise slightly above the cauldron rim for VFX and display spawning
        this.cauldronCenter = this.cauldron.getLocation(true).add(0, 0.5, 0, false);

        context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(this.cauldron)));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.cauldronPos == null) {
            return false;
        }

        BlockState current = world.getBlockState(this.cauldronPos);
        return current.is(Blocks.CAULDRON) || current.is(Blocks.WATER_CAULDRON);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        // Teardown obligations (leather display removal, cauldron restoration) are handled by TeardownScope.teardownAll()
        this.cauldron = null;
        this.cauldronPos = null;
        this.cauldronCenter = null;
        this.leatherDisplay = null;
        this.restoreHandle = null;
        this.leatherDisplayHandle = null;
    }

    private BehaviorStep<BaseVillager> createWashStep() {
        TimeBasedStep<BaseVillager> fill = TimeBasedStep.<BaseVillager>builder()
                .withTickable(FILL_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.WATER_BUCKET.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);

                    this.setCauldronWaterLevel(ctx.getLevel(), 3);
                    this.restoreHandle = ctx.getTeardownScope().track(new RestoreBlockObligation(this.cauldronPos,
                            BuiltInRegistries.BLOCK.getKey(Blocks.CAULDRON), BuiltInRegistries.BLOCK.getKey(Blocks.WATER_CAULDRON)));

                    if (this.cauldronCenter != null) {
                        this.cauldronCenter.displayParticles(ParticleTypes.SPLASH, 6, 0.2, 0.1, 0.2, 0.02);
                        this.cauldronCenter.playSound(SoundEvents.BUCKET_EMPTY, 0.5f, 1.0f, SoundSource.BLOCKS);
                    }
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> holdLeather = TimeBasedStep.<BaseVillager>builder()
                .withTickable(HOLD_LEATHER_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.LEATHER.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    if (this.cauldron == null || this.cauldronPos == null) {
                        return StepResult.complete();
                    }

                    // Leather floats just below the water surface inside the basin
                    Location displayLocation = this.cauldron.getLocation(false);
                    TransformationMatrix matrix = this.getAnimatedLeatherMatrix(ctx.getLevel().getGameTime());
                    this.leatherDisplay = TransformedItemDisplay.builder()
                            .itemStack(Items.LEATHER.getDefaultInstance())
                            .transform(matrix)
                            .build();
                    this.leatherDisplay.spawn(displayLocation);

                    this.leatherDisplayHandle = ctx.getTeardownScope().track(
                            new DiscardEntityObligation(this.leatherDisplay.getDisplayEntity().getUUID(), this.cauldronPos));

                    ctx.getInitiator().clearHeldItem();

                    if (this.cauldronCenter != null) {
                        this.cauldronCenter.displayParticles(ParticleTypes.SPLASH, 4, 0.15, 0.05, 0.15, 0.01);
                        this.cauldronCenter.playSound(SoundEvents.ITEM_PICKUP, 0.3f, 0.8f, SoundSource.BLOCKS);
                    }
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> wash = TimeBasedStep.<BaseVillager>builder()
                .withTickable(WASH_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.BRUSH.getDefaultInstance());
                    return StepResult.noOp();
                })
                .everyTick(ctx -> {
                    if (this.leatherDisplay != null) {
                        TransformationMatrix matrix = this.getAnimatedLeatherMatrix(ctx.getLevel().getGameTime());
                        this.leatherDisplay.setTransformation(matrix, ClockTicks.ONE);
                    }
                    return StepResult.noOp();
                })
                .addPeriodicStep(INTERACT_RETRIGGER_INTERVAL_TICKS, ctx -> {
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    return StepResult.noOp();
                })
                .addPeriodicStep(WASH_EFFECT_INTERVAL_TICKS, ctx -> {
                    if (this.cauldronCenter == null) {
                        return StepResult.noOp();
                    }

                    this.cauldronCenter.displayParticles(ParticleTypes.SPLASH, 8, 0.3, 0.1, 0.3, 0.02);
                    this.cauldronCenter.playSound(SoundEvents.GENERIC_SPLASH, 0.25f, 1.2f, SoundSource.BLOCKS);

                    ColorParticleOption dirtySwirl = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, DIRTY_WATER_COLOR);
                    this.cauldronCenter.displayParticles(dirtySwirl, 5, 0.2, 0.1, 0.2, 0.0);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> removeLeather = TimeBasedStep.<BaseVillager>builder()
                .withTickable(REMOVE_LEATHER_DURATION.asTickable())
                .onStart(ctx -> {
                    // Removes entity now and drops it from the ledger
                    if (this.leatherDisplayHandle != null) {
                        this.leatherDisplayHandle.dispose(ctx.getLevel());
                        this.leatherDisplayHandle = null;
                    }
                    this.leatherDisplay = null;

                    ctx.getInitiator().setHeldItem(Items.LEATHER.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);

                    if (this.cauldronCenter != null) {
                        this.cauldronCenter.displayParticles(ParticleTypes.WAX_OFF, 6, 0.2, 0.3, 0.2, 0.1);
                        this.cauldronCenter.playSound(SoundEvents.ITEM_PICKUP, 0.3f, 1.2f, SoundSource.BLOCKS);
                    }
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    ctx.getInitiator().clearHeldItem();
                    return StepResult.noOp();
                })
                .build();

        TimeBasedStep<BaseVillager> drain = TimeBasedStep.<BaseVillager>builder()
                .withTickable(DRAIN_DURATION.asTickable())
                .onStart(ctx -> {
                    ctx.getInitiator().setHeldItem(Items.BUCKET.getDefaultInstance());
                    ctx.getInitiator().triggerMotion(AnimationArchetype.INTERACT);
                    if (this.cauldronCenter != null) {
                        this.cauldronCenter.displayParticles(ParticleTypes.SPLASH, 5, 0.2, 0.1, 0.2, 0.02);
                        this.cauldronCenter.playSound(SoundEvents.BUCKET_FILL, 0.5f, 1.0f, SoundSource.BLOCKS);
                    }
                    return StepResult.noOp();
                })
                .onEnd(ctx -> {
                    this.setCauldronWaterLevel(ctx.getLevel(), 0);
                    if (this.restoreHandle != null) {
                        this.restoreHandle.dispose(ctx.getLevel());
                        this.restoreHandle = null;
                    }

                    ctx.getInitiator().clearHeldItem();
                    this.rewardExperience(ctx.getInitiator().getMinecraftEntity());
                    return StepResult.complete();
                })
                .build();

        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.WALK, 1))
                .actionStep(new SequencedStep<>("WashLeatherBehavior.sequence",
                        List.of(fill, holdLeather, wash, removeLeather, drain)))
                .build();
    }

    /**
     * Sets the cauldron water level directly. Level 0 converts back to an empty cauldron.
     */
    private void setCauldronWaterLevel(@Nonnull Level level, int waterLevel) {
        if (this.cauldronPos == null) {
            return;
        }

        BlockState current = level.getBlockState(this.cauldronPos);
        if (!current.is(Blocks.CAULDRON) && !current.is(Blocks.WATER_CAULDRON)) {
            log.behaviorTrace("Skipping cauldron water level update at {} — block is no longer a cauldron", this.cauldronPos);
            return;
        }

        BlockState newState;
        if (waterLevel <= 0) {
            newState = Blocks.CAULDRON.defaultBlockState();
        } else {
            newState = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, waterLevel);
        }

        level.setBlock(this.cauldronPos, newState, BlockFlag.of(BlockFlag.SEND_BLOCK_UPDATE, BlockFlag.SEND_CLIENT_UPDATE));
    }

    /**
     * Returns the transformation matrix for the floating leather display inside the cauldron basin.
     */
    private TransformationMatrix getAnimatedLeatherMatrix(float gameTime) {
        float spinAngleRadians = (gameTime % 100) / 100.0f * Mth.TWO_PI;

        return new TransformationMatrix(new Matrix4f()
                .translate(0.5f, 0.875f, 0.5f)
                .rotateY(spinAngleRadians)
                .rotateX(Mth.DEG_TO_RAD * 30.0f)
                .scale(0.8f));
    }

}
