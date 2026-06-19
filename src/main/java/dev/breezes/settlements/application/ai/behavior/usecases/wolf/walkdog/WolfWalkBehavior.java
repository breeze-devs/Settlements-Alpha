package dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog;

import dev.breezes.settlements.application.ai.behavior.runtime.StateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetableType;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.RandomRangeTickable;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CustomLog
public class WolfWalkBehavior extends StateMachineBehavior<SettlementsWolf> {

    private static final double TARGET_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int NAVIGATION_COMPLETION_DISTANCE = 1;
    private static final double ENTITY_TARGET_CHANCE = 0.65;
    private static final double DIG_CHANCE = 0.35;
    private static final int DIG_EFFECT_INTERVAL_TICKS = 5;

    private enum WolfWalkStage implements StageKey {
        PICK_TARGET,
        WALK_TO_TARGET,
        SNIFF,
        DIG,
        END;
    }

    private final WolfWalkConfig config;
    private int elapsedTicks;

    public WolfWalkBehavior(@Nonnull WolfWalkConfig config) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable());
        this.config = config;
        this.elapsedTicks = 0;

        this.preconditions.add(wolf -> wolf.isFollowOwnerLockedBy(WalkDogBehavior.class) && !wolf.isOrderedToSit());
        this.continueConditions.add(wolf -> wolf.isFollowOwnerLockedBy(WalkDogBehavior.class));
        this.continueConditions.add(wolf -> Optional.ofNullable(wolf.getOwner())
                .filter(BaseVillager::isAlive)
                .filter(owner -> !owner.isRemoved())
                .isPresent());

        this.initializeStateMachine(this.createControlStep(), WolfWalkStage.END);
    }

    private StagedStep<SettlementsWolf> createControlStep() {
        return StagedStep.<SettlementsWolf>builder()
                .name("WolfWalkBehavior")
                .initialStage(WolfWalkStage.PICK_TARGET)
                .stageStepMap(Map.of(
                        WolfWalkStage.PICK_TARGET, this.createPickTargetStep(),
                        WolfWalkStage.WALK_TO_TARGET, this.createWalkToTargetStep(),
                        WolfWalkStage.SNIFF, this.createSniffStep(),
                        WolfWalkStage.DIG, this.createDigStep()))
                .nextStage(WolfWalkStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<SettlementsWolf> createPickTargetStep() {
        return context -> {
            if (this.elapsedTicks >= ClockTicks.seconds(this.config.walkDurationSeconds()).getTicks()) {
                log.behaviorTrace("Walk duration elapsed ({} ticks), ending", this.elapsedTicks);
                return StepResult.transition(WolfWalkStage.END);
            }

            SettlementsWolf wolf = context.getInitiator();
            log.behaviorTrace("Picking target for wolf {} | pos={}", wolf.getUUID(), wolf.blockPosition());
            Optional<Targetable> target = RandomUtil.chance(ENTITY_TARGET_CHANCE)
                    ? this.pickEntityTarget(wolf).or(() -> this.pickBlockTarget(wolf))
                    : this.pickBlockTarget(wolf).or(() -> this.pickEntityTarget(wolf));

            if (target.isEmpty()) {
                log.behaviorTrace("No target found, ending behavior");
                return StepResult.transition(WolfWalkStage.END);
            }

            log.behaviorTrace("Picked target: type={} | loc={}", target.get().getType(), target.get().getLocation());
            context.setState(BehaviorStateType.TARGET, TargetState.of(target.get()));
            return StepResult.transition(WolfWalkStage.WALK_TO_TARGET);
        };
    }

    private BehaviorStep<SettlementsWolf> createWalkToTargetStep() {
        return StayCloseStep.<SettlementsWolf>builder()
                .closeEnoughDistance(TARGET_CLOSE_ENOUGH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(NavigationType.RUN, NAVIGATION_COMPLETION_DISTANCE))
                .actionStep(ctx -> StepResult.transition(WolfWalkStage.SNIFF))
                .build();
    }

    private BehaviorStep<SettlementsWolf> createSniffStep() {
        return TimeBasedStep.<SettlementsWolf>builder()
                .withTickable(RandomRangeTickable.of(
                        ClockTicks.seconds(this.config.sniffDurationMinSeconds()),
                        ClockTicks.seconds(this.config.sniffDurationMaxSeconds())))
                .everyTick(this::lookAtTarget)
                .everyTick(this::followMovingEntityTarget)
                .onEnd(context -> this.shouldDig(context)
                        ? StepResult.transition(WolfWalkStage.DIG)
                        : StepResult.transition(WolfWalkStage.PICK_TARGET))
                .build();
    }

    private BehaviorStep<SettlementsWolf> createDigStep() {
        return TimeBasedStep.<SettlementsWolf>builder()
                .withTickable(RandomRangeTickable.of(
                        ClockTicks.seconds(this.config.digDurationMinSeconds()),
                        ClockTicks.seconds(this.config.digDurationMaxSeconds())))
                .addPeriodicStep(DIG_EFFECT_INTERVAL_TICKS, this::playDigEffects)
                .onEnd(ctx -> StepResult.transition(WolfWalkStage.PICK_TARGET))
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull SettlementsWolf entity,
                                   @Nonnull BehaviorContext<SettlementsWolf> context) {
        this.elapsedTicks = 0;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull SettlementsWolf entity) {
        entity.getNavigationManager().stop();
        entity.unlockFollowOwner(WalkDogBehavior.class);
        this.elapsedTicks = 0;
    }

    @Override
    public boolean tickContinueConditions(int delta, @Nonnull Level world, @Nonnull SettlementsWolf entity) {
        this.elapsedTicks += delta;
        return super.tickContinueConditions(delta, world, entity);
    }

    private Optional<Targetable> pickEntityTarget(@Nonnull SettlementsWolf wolf) {
        BaseVillager owner = wolf.getOwner();
        AABB scanBox = wolf.getBoundingBox().inflate(this.config.entityTargetScanRange());
        List<Entity> targets = wolf.level().getEntities(wolf, scanBox, entity -> isSniffableEntity(wolf, owner, entity));

        log.behaviorTrace("Entity scan found {} candidates (range={})", targets.size(), this.config.entityTargetScanRange());
        if (targets.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(Targetable.fromEntity(RandomUtil.choice(targets).orElseThrow()));
    }

    private Optional<Targetable> pickBlockTarget(@Nonnull SettlementsWolf wolf) {
        Vec3 randomTarget = DefaultRandomPos.getPos(wolf,
                this.config.blockTargetHorizontalRange(),
                this.config.blockTargetVerticalRange());
        if (randomTarget == null) {
            return Optional.empty();
        }

        BlockPos blockPos = BlockPos.containing(randomTarget).below();
        BlockState blockState = wolf.level().getBlockState(blockPos);
        if (blockState.isAir()) {
            log.behaviorTrace("Block target pos {} is air, skipping", blockPos);
            return Optional.empty();
        }
        log.behaviorTrace("Block target: {}", blockPos);
        return Optional.of(Targetable.fromBlock(PhysicalBlock.of(Location.of(blockPos, wolf.level()), blockState)));
    }

    private static boolean isSniffableEntity(@Nonnull SettlementsWolf wolf,
                                             @Nullable BaseVillager owner,
                                             @Nonnull Entity entity) {
        if (!entity.isAlive() || entity.isRemoved() || entity == owner) {
            return false;
        }

        // TODO: we can migrate to a tag-based registry if needed
        return entity.getType() == EntityType.PLAYER
                || entity.getType() == EntityType.ITEM
                || entity instanceof AbstractVillager;
    }

    @Override
    protected boolean shouldLookAtActiveTarget() {
        // Look is driven explicitly by the WALK/SNIFF steps via everyTick(this::lookAtTarget).
        return false;
    }

    private StepResult lookAtTarget(@Nonnull BehaviorContext<SettlementsWolf> context) {
        SettlementsWolf wolf = context.getInitiator();
        context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation)
                .ifPresent(location -> wolf.getLookControl().setLookAt(location.toVec3()));
        return StepResult.noOp();
    }

    private StepResult followMovingEntityTarget(@Nonnull BehaviorContext<SettlementsWolf> context) {
        SettlementsWolf wolf = context.getInitiator();
        context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .filter(target -> target.getType() == TargetableType.ENTITY)
                .map(Targetable::getAsEntity)
                .filter(Entity::isAlive)
                .map(entity -> Location.fromEntity(entity, false))
                .ifPresent(location -> {
                    // Moving targets drift away from the original path, so refresh during sniffing instead of waiting for navigation to finish.
                    if (location.distanceSquared(wolf) > TARGET_CLOSE_ENOUGH_DISTANCE * TARGET_CLOSE_ENOUGH_DISTANCE) {
                        wolf.getNavigationManager().navigateTo(location, NavigationType.RUN, NAVIGATION_COMPLETION_DISTANCE);
                    } else {
                        wolf.getNavigationManager().stop();
                    }
                });
        return StepResult.noOp();
    }

    private boolean shouldDig(@Nonnull BehaviorContext<SettlementsWolf> context) {
        return RandomUtil.chance(DIG_CHANCE) && context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(target -> target.getType() == TargetableType.BLOCK)
                .orElse(false);
    }

    private StepResult playDigEffects(@Nonnull BehaviorContext<SettlementsWolf> context) {
        SettlementsWolf wolf = context.getInitiator();
        BlockPos blockPosBelow = wolf.blockPosition().below();
        BlockState blockStateBelow = wolf.level().getBlockState(blockPosBelow);
        if (blockStateBelow.isAir()) {
            return StepResult.noOp();
        }

        Location effectLocation = Location.of(blockPosBelow, wolf.level()).center(true);
        ParticleRegistry.digBlockCrack(effectLocation.add(0, 0.9, 0, true), blockStateBelow);
        effectLocation.playSound(blockStateBelow.getSoundType(wolf.level(), blockPosBelow, null).getBreakSound(), 0.3f, 1.4f, SoundSource.NEUTRAL);
        return StepResult.noOp();
    }

}
