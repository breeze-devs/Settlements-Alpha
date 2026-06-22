package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes.BehaviorOutcome;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.TargetState;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets.Targetable;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.BehaviorStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.TimeBasedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.LoopBackStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.NavigateToTargetStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.OneShotStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.concrete.StayCloseStep;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.ai.conditions.PerceivedEntityExistsCondition;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventType;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.domain.world.location.Vector;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.SettlementsEgg;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CustomLog
public class ThrowEggsBehavior extends VillagerStateMachineBehavior {

    private static final int BURST_DURATION_TICKS = 30;
    private static final int BURST_ROUNDS_MIN = 2;
    private static final int BURST_ROUNDS_MAX = 4;

    private static final float EGG_VELOCITY = 1.5f;
    private static final float EGG_INACCURACY = 8.0f;

    // Spawn offsets that place the egg at the windmill's overhead release point rather than the
    // body center: just above the head, shifted to the active shoulder, a touch ahead of the torso.
    private static final double HAND_LATERAL_OFFSET = 0.4;
    private static final double HAND_FORWARD_OFFSET = 0.1;
    private static final double HAND_RAISED_ABOVE_HEAD = 0.15;

    private static final int ANGRY_VILLAGER_PARTICLE_COUNT = 1;
    private static final double ANGRY_VILLAGER_PARTICLE_OFFSET = 0.3;
    private static final double ANGRY_VILLAGER_PARTICLE_SPEED = 0.1;

    private static final double RELOCATE_RADIUS = 4.0;
    private static final double CLOSE_ENOUGH_DISTANCE = 1.5;
    private static final int RELOCATE_TIMEOUT_TICKS = ClockTicks.seconds(4).getTicksAsInt();
    private static final Set<Class<?>> TARGET_CLASSES = Set.of(
            Villager.class,
            WanderingTrader.class,
            Player.class
    );

    private enum Stage implements StageKey {
        RELOCATE_PICK,
        RELOCATE_MOVE,
        BURST,
        BURST_LOOP,
        END
    }

    @Nullable
    private LivingEntity victim;
    // Flips each throw so eggs leave the raised left and right fists in turn
    private int eggsThrown;

    public ThrowEggsBehavior(ThrowEggsConfig config, HungerConfig hungerConfig) {
        super(log, config.createPreconditionCheckCooldownTickable(), config.createBehaviorCooldownTickable(), hungerConfig);

        this.preconditions.add(PerceivedEntityExistsCondition.<BaseVillager, LivingEntity>builder()
                .entityType(LivingEntity.class)
                .filter(this::isValidEggTarget)
                .build());

        this.victim = null;

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        Map<StageKey, BehaviorStep<BaseVillager>> stageMap = new HashMap<>();
        stageMap.put(Stage.RELOCATE_PICK, this.createRelocatePickStep());
        stageMap.put(Stage.RELOCATE_MOVE, this.createRelocateMoveStep());
        stageMap.put(Stage.BURST, this.createBurstStep());
        stageMap.put(Stage.BURST_LOOP, LoopBackStep.<BaseVillager>builder()
                .name("EggBurstLoop")
                .loopBackTo(Stage.RELOCATE_PICK)
                .completionTransition(Stage.END)
                // Resolved lazily once per run — determines how many times the nitwit scampers and bursts
                .maxIterationsResolver(ctx -> RandomUtil.randomInt(BURST_ROUNDS_MIN, BURST_ROUNDS_MAX, true))
                .build());

        return StagedStep.<BaseVillager>builder()
                .name("ThrowEggsBehavior")
                .initialStage(Stage.RELOCATE_PICK)
                .stageStepMap(stageMap)
                .nextStage(Stage.END)
                .build();
    }

    /**
     * Picks a random walkable point ~4 blocks from the current victim position and stores it
     * as the TargetState so the navigation step can move toward it.
     */
    private BehaviorStep<BaseVillager> createRelocatePickStep() {
        return OneShotStep.<BaseVillager>builder()
                .name("EggRelocatePick")
                .action(ctx -> {
                    if (this.victim == null) {
                        return StepResult.transition(Stage.END);
                    }

                    BaseVillager villager = ctx.getInitiator();
                    Level world = villager.level();

                    // Drop the windmill while scampering so the run reads as normal locomotion
                    villager.setMotion(AnimationArchetype.IDLE);

                    // Project a random angle around the victim and snap to surface height
                    double angle = RandomUtil.randomDouble(0, 2 * Math.PI);
                    int targetX = (int) (this.victim.getX() + Math.cos(angle) * RELOCATE_RADIUS);
                    int targetZ = (int) (this.victim.getZ() + Math.sin(angle) * RELOCATE_RADIUS);
                    int targetY;

                    if (world instanceof ServerLevel server) {
                        targetY = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ) - 1;
                    } else {
                        // Fallback for client side — stay at current height
                        targetY = villager.getBlockY();
                    }
                    targetY += (int) villager.getEyeHeight();

                    BlockPos relocatePos = new BlockPos(targetX, targetY, targetZ);
                    Location relocateLocation = Location.of(relocatePos, world);
                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromBlock(
                            PhysicalBlock.of(relocateLocation, world.getBlockState(relocatePos)))));

                    return StepResult.transition(Stage.RELOCATE_MOVE);
                })
                .build();
    }

    /**
     * Navigates to the chosen relocate point. Times out after ~10 s so the nitwit
     * bursts anyway if pathfinding can't find a route.
     */
    private BehaviorStep<BaseVillager> createRelocateMoveStep() {
        return StayCloseStep.<BaseVillager>builder()
                .closeEnoughDistance(CLOSE_ENOUGH_DISTANCE)
                .navigateStep(NavigateToTargetStep.<BaseVillager>builder()
                        .navigationType(NavigationType.RUN)
                        .completionDistance(1)
                        // The relocation point may land in an unreachable spot
                        // Skip straight to BURST so the nitwit throws from wherever it is
                        .unreachableTransition(Stage.BURST)
                        .build())
                .actionStep(OneShotStep.<BaseVillager>builder()
                        .name("EggRelocateArrived")
                        .action(ctx -> StepResult.transition(Stage.BURST))
                        .build())
                .timeoutTicks(RELOCATE_TIMEOUT_TICKS)
                .timeoutTransition(Stage.BURST)
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        List<LivingEntity> candidates = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty())
                .ofType(LivingEntity.class, target -> this.isValidEggTarget(villager, target))
                .toList();

        if (candidates.isEmpty()) {
            log.behaviorStatus("No egg targets found nearby; stopping");
            this.requestStop("no_nearby_egg_targets");
            return;
        }

        this.victim = RandomUtil.choice(candidates).orElseThrow();
        this.eggsThrown = 0;

        // Both hands carry an egg so the straight-arm windmill throw reads as dual-fisted mischief
        villager.setHeldItem(new ItemStack(Items.EGG));
        villager.setOffhandItem(new ItemStack(Items.EGG));
    }

    private boolean isValidEggTarget(@Nonnull BaseVillager villager, @Nonnull LivingEntity target) {
        if (!target.isAlive() || target.isRemoved()) {
            return false;
        }
        if (target == villager) {
            return false;
        }
        if (TARGET_CLASSES.stream().noneMatch(targetClass -> targetClass.isInstance(target))) {
            return false;
        }
        if (target instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.victim != null && this.isValidEggTarget(villager, this.victim);
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.clearHeldItem();
        villager.clearOffhandItem();
        this.victim = null;
    }

    private BehaviorStep<BaseVillager> createBurstStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("EggBurst")
                .withTickable(ClockTicks.of(BURST_DURATION_TICKS).asTickable())
                .onStart(ctx -> {
                    if (this.victim != null) {
                        ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.victim)));
                    }
                    // Sustained windmill while the burst is in progress; relocate/stop reset it to IDLE
                    ctx.getInitiator().setMotion(AnimationArchetype.THROW);
                    return StepResult.noOp();
                })
                .addPeriodicStep(ClockTicks.of(3).getTicksAsInt(), this::throwEggAtVictim)
                .onEnd(ctx -> StepResult.transition(Stage.BURST_LOOP))
                .build();
    }

    private StepResult throwEggAtVictim(@Nonnull BehaviorContext<BaseVillager> context) {
        if (this.victim == null || !this.victim.isAlive()) {
            return StepResult.transition(Stage.BURST_LOOP);
        }

        BaseVillager villager = context.getInitiator();
        Level world = villager.level();

        // Alternate hands each throw; the egg leaves the raised fist rather than the body center
        boolean rightHand = (this.eggsThrown % 2 == 0);
        this.eggsThrown++;

        Vec3 handPosition = this.computeRaisedHandPosition(villager, rightHand);
        Location handLocation = Location.of(handPosition.x, handPosition.y, handPosition.z, world);
        Location victimLocation = Location.fromEntity(this.victim, true);
        Vector direction = handLocation.getDirectionTo(victimLocation);

        SettlementsEgg egg = new SettlementsEgg(world, villager);
        egg.setPos(handPosition.x, handPosition.y, handPosition.z);
        egg.shoot(direction.getX(), direction.getY(), direction.getZ(), EGG_VELOCITY, EGG_INACCURACY);
        world.addFreshEntity(egg);

        SoundRegistry.THROW_EGG.playGlobally(handLocation, SoundSource.NEUTRAL);
        this.spawnAngryVillagerParticleOnVictim(world);

        if (context.primaryDeed().isEmpty()) {
            BehaviorOutcome outcome = BehaviorOutcome.forDeed(WorldEventType.TARGET_EGGED, null);
            // Use the victim's UUID only when it is a villager — the name resolver maps villager UUIDs
            if (this.victim instanceof BaseVillager || this.victim instanceof Villager) {
                outcome.recordTargetedDeed(this.victim.getUUID());
            } else {
                outcome.markSucceeded();
            }
            context.declarePrimaryDeed(outcome);
        }

        return StepResult.noOp();
    }

    /**
     * Resolves the world position of the windmill's overhead release point for the active hand.
     * The throw arc raises a straight arm fully overhead, so the egg originates just above the head,
     * shifted to the throwing shoulder and slightly forward — not from the villager's eye-center
     * (where the projectile's shooter constructor would otherwise place it).
     */
    private Vec3 computeRaisedHandPosition(@Nonnull BaseVillager villager, boolean rightHand) {
        Vec3 look = villager.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        // Fall back to body forward if the gaze is near-vertical and the horizontal component collapses
        forward = forward.lengthSqr() < 1.0e-4 ? villager.getForward() : forward.normalize();
        // Horizontal right-hand perpendicular to facing
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double lateral = rightHand ? HAND_LATERAL_OFFSET : -HAND_LATERAL_OFFSET;

        return new Vec3(
                villager.getX() + right.x * lateral + forward.x * HAND_FORWARD_OFFSET,
                villager.getY() + villager.getBbHeight() + HAND_RAISED_ABOVE_HEAD,
                villager.getZ() + right.z * lateral + forward.z * HAND_FORWARD_OFFSET);
    }

    private void spawnAngryVillagerParticleOnVictim(@Nonnull Level world) {
        if (this.victim == null || this.victim instanceof Player || !(world instanceof ServerLevel server)) {
            return;
        }

        server.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                this.victim.getX(), this.victim.getY() + this.victim.getBbHeight(), this.victim.getZ(),
                ANGRY_VILLAGER_PARTICLE_COUNT,
                ANGRY_VILLAGER_PARTICLE_OFFSET, ANGRY_VILLAGER_PARTICLE_OFFSET, ANGRY_VILLAGER_PARTICLE_OFFSET,
                ANGRY_VILLAGER_PARTICLE_SPEED);
    }

}
