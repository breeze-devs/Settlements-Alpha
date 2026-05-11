package dev.breezes.settlements.application.ai.behavior.usecases.villager.idle;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.SettlementsWolf;
import lombok.CustomLog;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public class WalkDogBehavior extends VillagerStateMachineBehavior {

    private static final float APPROACH_SPEED = 0.6f;
    private static final float FOLLOW_SPEED = 0.55f;
    private static final double LEASH_DISTANCE = 2.5;
    private static final int FOLLOW_COMPLETION_DISTANCE = 3;

    private enum WalkDogStage implements StageKey {
        APPROACH_WOLF,
        LEASH_WOLF,
        FOLLOWING_WOLF,
        UNLEASH_WOLF,
        END;
    }

    private final WalkDogConfig config;
    private SettlementsWolf cachedWolf;
    private boolean wolfFollowLockAcquired;

    public WalkDogBehavior(@Nonnull WalkDogConfig config, @Nonnull HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);
        this.config = config;

        this.preconditions.add(this::hasValidWolfToWalk);
        this.continueConditions.add(villager -> this.cachedWolf != null && this.cachedWolf.isAlive()
                && !this.cachedWolf.isRemoved()
                && villager.equals(this.cachedWolf.getOwner())
                && (!this.wolfFollowLockAcquired || this.cachedWolf.isFollowOwnerLockedBy(WalkDogBehavior.class))
        );

        this.initializeStateMachine(this.createControlStep(), WalkDogStage.END);
    }

    private boolean hasValidWolfToWalk(@Nonnull BaseVillager villager) {
        return villager.getBrain().getMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType())
                .filter(uuids -> !uuids.isEmpty())
                .flatMap(uuids -> this.findFirstValidWolf(villager, villager.level(), uuids))
                .isPresent();
    }

    private Optional<SettlementsWolf> findFirstValidWolf(@Nonnull BaseVillager villager,
                                                         @Nonnull Level world,
                                                         @Nonnull List<UUID> uuids) {
        AABB scanBox = this.createWolfScanBox(villager);
        List<SettlementsWolf> nearbyWolves = world.getEntitiesOfClass(SettlementsWolf.class, scanBox);

        for (UUID uuid : uuids) {
            Optional<SettlementsWolf> matchingWolf = nearbyWolves.stream()
                    .filter(wolf -> this.isValidWolfForWalk(villager, wolf, uuid))
                    .findFirst();
            if (matchingWolf.isPresent()) {
                return matchingWolf;
            }
        }
        return Optional.empty();
    }

    private boolean isValidWolfForWalk(@Nonnull BaseVillager villager,
                                       @Nonnull SettlementsWolf wolf,
                                       @Nonnull UUID uuid) {
        return wolf.getUUID().equals(uuid) && wolf.isAlive() && villager.equals(wolf.getOwner());
    }

    private AABB createWolfScanBox(@Nonnull BaseVillager villager) {
        return villager.getBoundingBox()
                .inflate(this.config.horizontalScanRange(), this.config.verticalScanRange(), this.config.horizontalScanRange());
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("WalkDogBehavior")
                .initialStage(WalkDogStage.APPROACH_WOLF)
                .stageStepMap(Map.of(
                        WalkDogStage.APPROACH_WOLF, this.createApproachStep(),
                        WalkDogStage.LEASH_WOLF, this.createLeashStep(),
                        WalkDogStage.FOLLOWING_WOLF, this.createFollowingStep(),
                        WalkDogStage.UNLEASH_WOLF, this.createUnleashStep()))
                .nextStage(WalkDogStage.END)
                .onEnd(ctx -> StepResult.noOp())
                .build();
    }

    private BehaviorStep<BaseVillager> createApproachStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(LEASH_DISTANCE)
                .navigateStep(new NavigateToTargetStep<>(APPROACH_SPEED, 1))
                .actionStep(ctx -> StepResult.transition(WalkDogStage.LEASH_WOLF))
                .build();
    }

    private BehaviorStep<BaseVillager> createLeashStep() {
        return context -> {
            BaseVillager villager = context.getInitiator();
            context.getLevel().getChunkSource().broadcast(this.cachedWolf, new ClientboundSetEntityLinkPacket(this.cachedWolf, villager));

            this.cachedWolf.lockFollowOwner(WalkDogBehavior.class);
            this.wolfFollowLockAcquired = true;
            ParticleRegistry.breedHearts(Location.fromEntity(this.cachedWolf, false));
            return StepResult.transition(WalkDogStage.FOLLOWING_WOLF);
        };
    }

    private BehaviorStep<BaseVillager> createFollowingStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .withTickable(ClockTicks.seconds(this.config.walkDurationSeconds()).asTickable())
                .addPeriodicStep(ClockTicks.seconds(1).getTicksAsInt(), context -> {
                    context.getInitiator().getNavigationManager().navigateTo(Location.fromEntity(this.cachedWolf, false), FOLLOW_SPEED, FOLLOW_COMPLETION_DISTANCE);
                    return StepResult.noOp();
                })
                .onEnd(ctx -> StepResult.transition(WalkDogStage.UNLEASH_WOLF))
                .build();
    }

    private BehaviorStep<BaseVillager> createUnleashStep() {
        return context -> {
            context.getLevel().getChunkSource().broadcast(this.cachedWolf, new ClientboundSetEntityLinkPacket(this.cachedWolf, null));
            this.cachedWolf.unlockFollowOwner(WalkDogBehavior.class);
            return StepResult.transition(WalkDogStage.END);
        };
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager entity,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        // Reset before selection so a stale reference from a previous run can never bleed through
        this.cachedWolf = null;
        entity.getBrain().getMemory(MemoryTypeRegistry.OWNED_WOLVES.getModuleType())
                .flatMap(uuids -> this.findFirstValidWolf(entity, world, uuids))
                .ifPresent(wolf -> this.cachedWolf = wolf);

        if (this.cachedWolf != null) {
            context.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.cachedWolf)));
            this.cachedWolf.setOrderedToSit(false);
        }
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
        if (this.cachedWolf != null) {
            ((ServerLevel) world).getChunkSource().broadcast(this.cachedWolf, new ClientboundSetEntityLinkPacket(this.cachedWolf, null));
            this.cachedWolf.unlockFollowOwner(WalkDogBehavior.class);
        }
        entity.getNavigationManager().stop();
        this.cachedWolf = null;
        this.wolfFollowLockAcquired = false;
    }

}
