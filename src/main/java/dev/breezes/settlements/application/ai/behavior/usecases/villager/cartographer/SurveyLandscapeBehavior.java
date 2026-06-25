package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.ConditionalStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.CartographerAnimations;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

@CustomLog
public class SurveyLandscapeBehavior extends VillagerStateMachineBehavior {

    private static final int SURVEY_POINT_HORIZONTAL_REACH = 5;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;
    private static final int MAX_SAMPLING_RETRIES = 20;
    private static final int MAX_SURVEY_POINT_Y_DIFFERENCE = 10;

    private enum SurveyStage implements StageKey {
        NAVIGATE_TO_POINT,
        SURVEY,
        MARK_MAP,
        END;
    }

    private final SurveyLandscapeConfig config;

    // Runtime state
    private final Queue<Location> remainingPoints = new ArrayDeque<>();
    private boolean shouldRewardExperience;

    public SurveyLandscapeBehavior(@Nonnull SurveyLandscapeConfig config,
                                   @Nonnull BehaviorSupport support) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(),
                support, config.experienceReward());
        this.config = config;
        this.shouldRewardExperience = false;
        this.initializeStateMachine(this.createControlStep(), SurveyStage.END);
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.sampleSurveyPoints(villager, world);

        if (this.remainingPoints.isEmpty()) {
            return;
        }

        this.shouldRewardExperience = false;

        Location firstPoint = this.remainingPoints.poll();
        context.setState(BehaviorStateType.TARGET, TargetState.of(this.toTargetable(firstPoint)));
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        if (this.shouldRewardExperience) {
            this.rewardExperience(villager);
        }

        villager.getNavigationManager().stop();
        villager.clearHeldItem();
        villager.setMotion(AnimationArchetype.IDLE);

        this.remainingPoints.clear();
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("SurveyLandscapeBehavior")
                .initialStage(SurveyStage.NAVIGATE_TO_POINT)
                .stageStepMap(Map.of(
                        SurveyStage.NAVIGATE_TO_POINT, this.createNavigateToPointStep(),
                        SurveyStage.SURVEY, this.createSurveyStep(),
                        SurveyStage.MARK_MAP, this.createMarkMapStep()))
                .nextStage(SurveyStage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createNavigateToPointStep() {
        TimeBasedStep<BaseVillager> arrivedStep = TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(0.5).asTickable())
                .onEnd(context -> StepResult.transition(SurveyStage.SURVEY))
                .build();

        // Reachability gating is off so the cartographer always attempts the sampled survey point
        NavigateToTargetStep<BaseVillager> navigateStep = NavigateToTargetStep.<BaseVillager>builder()
                .navigationType(NavigationType.WALK)
                .completionDistance(NAVIGATION_COMPLETION_DISTANCE)
                .reachabilityGated(false)
                .build();

        ICondition<BehaviorContext<BaseVillager>> closeEnoughHorizontally = ICondition.named(
                "closeEnoughHorizontally",
                context -> {
                    if (context == null) {
                        return false;
                    }
                    BaseVillager villager = context.getInitiator();
                    return context.getState(BehaviorStateType.TARGET, TargetState.class)
                            .flatMap(TargetState::getFirst)
                            .map(Targetable::getLocation)
                            .map(target -> Math.abs(target.getBlockX() - villager.getMinecraftEntity().getBlockX())
                                    + Math.abs(target.getBlockZ() - villager.getMinecraftEntity().getBlockZ())
                                    <= SURVEY_POINT_HORIZONTAL_REACH)
                            .orElse(false);
                });

        return ConditionalStep.<BaseVillager>builder()
                .name("SurveyArrivedOrNavigate")
                .condition(closeEnoughHorizontally)
                .trueStep(arrivedStep)
                .falseStep(navigateStep)
                .build();
    }

    private BehaviorStep<BaseVillager> createSurveyStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(CartographerAnimations.SURVEY_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator();
                    villager.getNavigationManager().stop();
                    villager.setHeldItem(Items.SPYGLASS.getDefaultInstance());
                    villager.triggerMotion(AnimationArchetype.SURVEY_WITH_SPYGLASS);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    return StepResult.transition(SurveyStage.MARK_MAP);
                })
                .build();
    }

    private BehaviorStep<BaseVillager> createMarkMapStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.of(CartographerAnimations.MARK_DURATION_TICKS).asTickable())
                .onStart(context -> {
                    BaseVillager villager = context.getInitiator();
                    villager.getNavigationManager().stop();
                    villager.setHeldItem(Items.MAP.getDefaultInstance());
                    SoundRegistry.MAP_FLAP.playGlobally(Location.fromEntity(context.getInitiator(), false), SoundSource.NEUTRAL);

                    villager.triggerMotion(AnimationArchetype.WRITE_TO_MAP);
                    return StepResult.noOp();
                })
                .addKeyFrame(ClockTicks.of(CartographerAnimations.MARK_DURATION_TICKS / 2), context -> {
                    context.getInitiator().setHeldItem(Items.FILLED_MAP.getDefaultInstance());
                    SoundRegistry.MAP_SCRIBBLE.playGlobally(Location.fromEntity(context.getInitiator(), false), SoundSource.NEUTRAL);
                    return StepResult.noOp();
                })
                .onEnd(context -> {
                    context.getInitiator().clearHeldItem();
                    this.shouldRewardExperience = true;

                    if (context.primaryDeed().isEmpty()) {
                        BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.LANDSCAPE_SURVEYED, null);
                        outcome.markSucceeded();
                        context.declarePrimaryDeed(outcome);
                    }

                    if (this.remainingPoints.isEmpty()) {
                        // All points surveyed
                        return StepResult.transition(SurveyStage.END);
                    }

                    Location nextPoint = this.remainingPoints.poll();
                    context.setState(BehaviorStateType.TARGET, TargetState.of(this.toTargetable(nextPoint)));
                    return StepResult.transition(SurveyStage.NAVIGATE_TO_POINT);
                })
                .build();
    }

    private void sampleSurveyPoints(@Nonnull BaseVillager villager, @Nonnull Level world) {
        int pointCount = this.config.expertiseSurveyPointCount()
                .getOrDefault(villager.getExpertise().getConfigName(), 2);

        Location origin = Location.fromEntity(villager, false);
        List<Location> sampled = new ArrayList<>();

        for (int i = 0; i < pointCount; i++) {
            Optional<Location> candidate = this.trySamplePoint(origin, sampled, villager, world);
            candidate.ifPresent(sampled::add);
        }

        if (sampled.isEmpty()) {
            log.behaviorWarn("No reachable survey points found; stopping behavior");
            this.requestStop("no_reachable_survey_points");
            return;
        }

        this.remainingPoints.addAll(sampled);
    }

    private Optional<Location> trySamplePoint(@Nonnull Location origin,
                                              @Nonnull List<Location> existing,
                                              @Nonnull BaseVillager villager,
                                              @Nonnull Level world) {
        ServerLevel serverLevel = (ServerLevel) world;
        double minSeparationSquared = (double) this.config.minPointSeparation() * this.config.minPointSeparation();

        for (int attempt = 0; attempt < MAX_SAMPLING_RETRIES; attempt++) {
            double yaw = RandomUtil.randomDouble(0, 2 * Math.PI);
            double distance = RandomUtil.randomDouble(this.config.minPointDistance(), this.config.maxPointDistance());

            int x = origin.getBlockX() + (int) (Math.cos(yaw) * distance);
            int z = origin.getBlockZ() + (int) (Math.sin(yaw) * distance);
            // Surface height gives the highest motion-blocking non-leaf block (subtract 1 to land on it)
            int y = serverLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;

            // Reject points whose surface Y is too far from the villager — Minecraft pathfinding can't
            // bridge large vertical gaps (e.g. surface is sealed off above us by a glass roof)
            if (Math.abs(y - origin.getBlockY()) > MAX_SURVEY_POINT_Y_DIFFERENCE) {
                continue;
            }

            // Heightmap can land on non-solid surfaces (water, fluids); reject so the villager doesn't survey from mid-water
            BlockPos targetPos = new BlockPos(x, y, z);
            BlockState surfaceState = serverLevel.getBlockState(targetPos);
            if (surfaceState.getCollisionShape(serverLevel, targetPos).isEmpty()) {
                continue;
            }

            Location candidate = Location.of(x + 0.5, y, z + 0.5, world);

            // Reject if too close to a previously accepted point to avoid clustering
            boolean tooClose = existing.stream()
                    .anyMatch(p -> p.distanceSquared(candidate) < minSeparationSquared);
            if (tooClose) {
                continue;
            }

            if (!villager.getNavigationManager().canReach(candidate, this.config.maxPointDistance())) {
                continue;
            }

            return Optional.of(candidate);
        }

        return Optional.empty();
    }

    /**
     * Wraps a {@link Location} as a {@link Targetable} by reading the surface block at that position,
     * which lets StayCloseStep and NavigateToTargetStep work without any new Targetable implementation.
     */
    private Targetable toTargetable(@Nonnull Location location) {
        Level level = location.getLevel()
                .orElseThrow(() -> new IllegalStateException("Survey point location has no level reference"));
        return Targetable.fromBlock(PhysicalBlock.of(location, level.getBlockState(location.toBlockPos())));
    }

}
