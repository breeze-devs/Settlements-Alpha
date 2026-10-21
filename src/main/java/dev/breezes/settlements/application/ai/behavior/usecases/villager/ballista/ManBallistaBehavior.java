package dev.breezes.settlements.application.ai.behavior.usecases.villager.ballista;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
import dev.breezes.settlements.application.ai.behavior.runtime.VillagerStateMachineBehavior;
import dev.breezes.settlements.application.ai.behavior.workflow.staged.StagedStep;
import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.look.LookState;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.AbstractStep;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StageKey;
import dev.breezes.settlements.application.ai.behavior.workflow.steps.StepResult;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.ai.navigation.NavigationType;
import dev.breezes.settlements.domain.ai.threat.ThreatResponseRules;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.BallistaAnimations;
import dev.breezes.settlements.domain.ballista.BallistaFiringSolution;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import dev.breezes.settlements.domain.ballista.BallistaLease;
import dev.breezes.settlements.domain.ballista.BallistaOperatorSkill;
import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaBlockEntity;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.BallistaBolt;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Uses a ballista against one hostile, shot after shot, until the target is lost, the machine cannot be worked, or the
 * operator has no bolt left to load.
 */
@CustomLog(topic = "behavior")
public class ManBallistaBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks APPROACH_TIMEOUT = ClockTicks.seconds(12);
    private static final ClockTicks AIM_TIMEOUT = ClockTicks.seconds(8);

    // A hostile out of every shot this long is given up on, so a fresh action can take one a shot reaches
    private static final ClockTicks NO_SOLUTION_GRACE = ClockTicks.seconds(1);

    // One raycast per check, often enough that a shot is held soon after the target goes behind cover and loosed soon
    // after it steps back out
    private static final ClockTicks SIGHT_CHECK_INTERVAL = ClockTicks.of(5);

    private static final double LOCK_RANGE_SQUARED =
            BallistaCombatOption.LOCK_RANGE_BLOCKS * BallistaCombatOption.LOCK_RANGE_BLOCKS;

    // How near the station counts as at it, measured as vanilla measures a walk's arrival, so a walk vanilla calls
    // finished is never one this behavior sees as short
    private static final int REACH_BLOCKS = 1;

    // A shot is committed on the tick after its aim is given, once the barrel has caught up, and leaves the release
    // delay after that
    private static final int DEPARTURE_DELAY_TICKS = BallistaAnimations.FIRE_RELEASE_TICK + 1;

    // TODO: replace with the ballista_wind, ballista_load and ballista_fire villager clips once they are authored;
    //  existing archetypes stand in for them until then
    private static final AnimationArchetype WIND_MOTION = AnimationArchetype.FORGE;
    private static final AnimationArchetype LOAD_MOTION = AnimationArchetype.INTERACT;
    private static final AnimationArchetype FIRE_MOTION = AnimationArchetype.SWING_HEAVY;

    private enum Stage implements StageKey {
        APPROACH,
        WIND,
        LOAD,
        AIM,
        FIRE,
        END
    }

    private enum StationApproach {
        ARRIVED,
        WALKING,
        UNREACHABLE
    }

    private final BallistaCombatOption option;
    private final BallistaOperationConfig config;
    private final BallistaShotChecks shotChecks;
    private final LivingEntity target;
    private final ITickable leaseRenewal;
    private final ITickable sightCheck;

    /**
     * Chosen by the precondition, the one point where this action refuses to start; null until then.
     */
    @Nullable
    private BlockPos machinePos;

    /**
     * Re-read every tick, since a block entity does not survive its chunk reloading; null while not running.
     */
    @Nullable
    private BallistaBlockEntity machine;

    @Nullable
    private BallistaOperatorSkill skill;

    // As of the latest sight check
    private boolean targetInSight;
    private int ticksOutOfSight;

    /**
     * Stations a path was found to during the current shot cycle.
     */
    private final Set<BlockPos> reachableBallistas = new HashSet<>();

    /**
     * The station the villager was last sent walking to; null while it is not walking.
     */
    @Nullable
    private BlockPos walkTarget;

    /**
     * @param target the hostile to shoot
     */
    ManBallistaBehavior(@Nonnull BehaviorSupport support,
                        @Nonnull BallistaCombatOption option,
                        @Nonnull BallistaOperationConfig config,
                        @Nonnull BallistaShotChecks shotChecks,
                        @Nonnull LivingEntity target) {
        super(log, ClockTicks.ZERO.asTickable(), ClockTicks.ZERO.asTickable(), support);

        this.option = option;
        this.config = config;
        this.shotChecks = shotChecks;
        this.target = target;
        this.leaseRenewal = Tickable.staggered(BallistaLease.RENEWAL_INTERVAL);
        this.sightCheck = Tickable.staggered(SIGHT_CHECK_INTERVAL);

        this.preconditions.add(ICondition.named("a ballista the villager can take up", this::chooseMachine));

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ManBallistaBehavior")
                .initialStage(Stage.APPROACH)
                .stageStepMap(Map.of(
                        Stage.APPROACH, new ApproachStep(),
                        Stage.WIND, new WindStep(),
                        Stage.LOAD, new LoadStep(),
                        Stage.AIM, new AimStep(),
                        Stage.FIRE, new FireStep()))
                .nextStage(Stage.END)
                .build();
    }

    private boolean chooseMachine(@Nullable BaseVillager villager) {
        this.machinePos = null;
        this.reachableBallistas.clear();
        if (villager == null) {
            return false;
        }

        // A launch carries out a target the assessment already approved, perhaps one held past the engagement range, so
        // it accepts one out to the lock range. Held to the engagement range, a locked target would be refused, revoked
        // and selected again on every tick
        BallistaCombatOption.Readiness readiness = this.option.prepare(villager);
        BallistaBlockEntity candidate = readiness == null ? null : readiness.machineFor(this.target, true);
        if (candidate == null) {
            return false;
        }

        BlockPos station = BlockPos.containing(stationOf(candidate.getBlockPos(), candidate.getAim().getIntentYaw()));
        if (!villager.getNavigationManager().canReach(Location.of(station, villager.level()), REACH_BLOCKS)) {
            // Checked before the lease is taken, so a walled-off station costs nobody the machine
            this.option.rememberRefusal(villager, candidate.getBlockPos());
            return false;
        }

        this.machinePos = candidate.getBlockPos();
        this.reachableBallistas.add(station);
        return true;
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        BallistaBlockEntity chosen = this.machinePos == null ? null
                : BallistaBlockEntity.inLoadedChunk(context.getLevel(), this.machinePos);
        if (chosen == null || !chosen.acquireLease(villager.getUUID())) {
            if (this.machinePos != null) {
                this.option.rememberRefusal(villager, this.machinePos);
            }
            this.requestStop("No ballista could be taken up");
            return;
        }

        this.machine = chosen;
        this.skill = BallistaOperatorSkill.of(villager.getGenetics(), villager.getExpertise(), this.config.maxScatterDegrees());
        // The precondition just found the machine in sight of the target
        this.targetInSight = true;
        this.ticksOutOfSight = 0;
        this.walkTarget = null;

        context.setState(BehaviorStateType.LOOK_TARGET, LookState.ofLocation(Location.of(this.machinePos, world)));
        if (BallistaCombatOption.hasBoltInInventory(villager)) {
            // Carried in view on the way, so the machine's use is shown before it happens
            villager.setHeldItem(new ItemStack(ItemRegistry.BALLISTA_BOLT.get()));
        }
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        if (this.machinePos == null || !this.target.isAlive() || this.target.isRemoved() || this.target.level() != world) {
            return false;
        }

        this.machine = BallistaBlockEntity.inLoadedChunk(context.getLevel(), this.machinePos);
        if (this.machine == null
                || this.target.position().distanceToSqr(BallistaCombatOption.floorCenterOf(this.machinePos)) > LOCK_RANGE_SQUARED) {
            return false;
        }

        if (this.sightCheck.tickCheckAndReset(delta)) {
            // Run a fresh line-of-sight check because this gates a live shot
            this.targetInSight = this.shotChecks.refreshLineOfSight(context.getLevel(), this.machinePos, this.target);
        }

        this.ticksOutOfSight = this.targetInSight ? 0 : this.ticksOutOfSight + delta;
        // Can continue locking onto the target for a while after losing line of sight
        if (this.ticksOutOfSight > ThreatResponseRules.TARGETLESS_COMBAT_LIMIT.getTicksAsInt()) {
            return false;
        }

        // A refused renewal means the lease lapsed, and another villager may have taken the machine since
        return !this.leaseRenewal.tickCheckAndReset(delta) || this.machine.renewLease(villager.getUUID());
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        // The machine is left as it stands, wound or loaded for whoever operates it next
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.clearHeldItem();
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        if (this.machine != null) {
            // Released rather than left to lapse, so a villager waiting on the machine can take it at once
            this.machine.releaseLease(villager.getUUID());
        }
        this.machine = null;
        this.walkTarget = null;
    }

    /**
     * The stage the machine's state calls for next: the first it has not reached.
     */
    private StepResult resumeFromMachineState(@Nonnull BaseVillager villager) {
        boolean seated = this.machine.hasSeatedAmmunition();
        if (!seated && !BallistaCombatOption.hasBoltInInventory(villager)) {
            // Nothing to fire, so the villager stands down rather than wind a machine it cannot load
            return StepResult.transition(Stage.END);
        }

        return switch (this.machine.settledStage()) {
            // Another operator's wind or shot is under way, and it settles on its own
            case WINDING, FIRING -> StepResult.noOp();
            case UNWOUND -> StepResult.transition(Stage.WIND);
            case COCKED -> StepResult.transition(seated ? Stage.AIM : Stage.LOAD);
        };
    }

    /**
     * Sends the villager toward the station behind the machine's intended heading.
     */
    private StationApproach approachStation(@Nonnull BaseVillager villager) {
        BlockPos station = BlockPos.containing(stationOf(this.machinePos, this.machine.getAim().getIntentYaw()));
        INavigationManager<BaseVillager> navigation = villager.getNavigationManager();
        if (villager.blockPosition().distManhattan(station) <= REACH_BLOCKS) {
            if (this.walkTarget != null) {
                navigation.stop();
                this.walkTarget = null;
            }
            return StationApproach.ARRIVED;
        }

        boolean walkEndedShort = station.equals(this.walkTarget) && navigation.getNavigationTarget().isEmpty();
        if (walkEndedShort) {
            // The way may have been blocked since the path was found. Sent again on trust, the walk would end short on
            // every tick, each time after a pathfinder search that cannot reach the station
            this.reachableBallistas.remove(station);
        }
        if (!this.canReachStation(villager, station)) {
            return StationApproach.UNREACHABLE;
        }

        // Sent again only when the station moves or the last walk ended short of it, so a slewing turret does not
        // re-path every tick
        if (walkEndedShort || !station.equals(this.walkTarget)) {
            navigation.navigateTo(Location.of(station, villager.level()), NavigationType.RUN, REACH_BLOCKS);
            this.walkTarget = station;
        }
        return StationApproach.WALKING;
    }

    private boolean canReachStation(@Nonnull BaseVillager villager, @Nonnull BlockPos station) {
        // The turret's heading can swing back and forth across a block boundary, so every station found is kept
        if (this.reachableBallistas.contains(station)) {
            return true;
        }
        if (!villager.getNavigationManager().canReach(Location.of(station, villager.level()), REACH_BLOCKS)) {
            return false;
        }
        this.reachableBallistas.add(station);
        return true;
    }

    /**
     * Ends the action over a station the villager cannot reach.
     */
    private StepResult failUnreachableStation(@Nonnull BaseVillager villager) {
        this.option.rememberRefusal(villager, this.machinePos);
        return StepResult.fail("station_unreachable");
    }

    /**
     * The aim that lands a bolt on the target as this operator judges it, before scatter; empty when no shot reaches it.
     */
    private Optional<BallistaFiringSolution> solveForTarget() {
        Vec3 aimPoint = BallistaCombatOption.aimPointOf(this.target);
        Vec3 velocity = new Vec3(this.target.getX() - this.target.xo,
                this.target.getY() - this.target.yo,
                this.target.getZ() - this.target.zo);
        // A less skilled operator underestimates the target's motion, both until the shot leaves and over its flight
        Vec3 judgedVelocity = velocity.scale(this.skill.getLeadQuality());
        Vec3 atDeparture = aimPoint.add(judgedVelocity.scale(DEPARTURE_DELAY_TICKS))
                .subtract(BallistaCombatOption.floorCenterOf(this.machinePos));
        return BallistaFiringSolution.solve(atDeparture, judgedVelocity, BallistaBolt.LAUNCH_SPEED_BLOCKS_PER_TICK);
    }

    /**
     * Where the operator stands to work the machine on the given heading.
     */
    private static Vec3 stationOf(@Nonnull BlockPos ballistaPos, float yaw) {
        return BallistaCombatOption.floorCenterOf(ballistaPos).add(BallistaLaunchPoint.stationOffset(yaw));
    }

    private static ItemStack boltToLoad(@Nonnull BaseVillager villager) {
        Item bolt = ItemRegistry.BALLISTA_BOLT.get();
        VillagerInventory inventory = villager.getSettlementsInventory();
        return inventory.findFirst(stack -> stack.is(bolt))
                .orElseGet(() -> inventory.requirementsBypassed() ? new ItemStack(bolt) : ItemStack.EMPTY);
    }

    private final class ApproachStep extends AbstractStep<BaseVillager> {

        private ApproachStep() {
            super("BallistaApproach", APPROACH_TIMEOUT.getTicksAsInt(), null);
        }

        @Override
        protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
            BaseVillager villager = context.getInitiator();
            return switch (approachStation(villager)) {
                case ARRIVED -> resumeFromMachineState(villager);
                case WALKING -> StepResult.noOp();
                case UNREACHABLE -> failUnreachableStation(villager);
            };
        }

    }

    private final class WindStep extends AbstractStep<BaseVillager> {

        private boolean winding;

        private WindStep() {
            super("BallistaWind");
        }

        @Override
        protected void doOnEnter() {
            this.winding = false;
        }

        @Override
        protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
            BaseVillager villager = context.getInitiator();
            if (!this.winding) {
                if (!machine.wind()) {
                    // Someone else changed the machine since this stage was chosen
                    return resumeFromMachineState(villager);
                }
                this.winding = true;
                villager.setMotion(WIND_MOTION);
                return StepResult.noOp();
            }

            if (machine.settledStage() == BallistaStateMachine.Stage.WINDING) {
                return StepResult.noOp();
            }
            villager.setMotion(AnimationArchetype.IDLE);
            return resumeFromMachineState(villager);
        }

    }

    private final class LoadStep extends AbstractStep<BaseVillager> {

        private int ticksLoading;

        private LoadStep() {
            super("BallistaLoad");
        }

        @Override
        protected void doOnEnter() {
            this.ticksLoading = 0;
        }

        @Override
        protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
            BaseVillager villager = context.getInitiator();
            if (this.ticksLoading == 0) {
                villager.triggerMotion(LOAD_MOTION);
            }
            if (++this.ticksLoading < skill.getLoadDuration().getTicksAsInt()) {
                return StepResult.noOp();
            }

            ItemStack bolt = boltToLoad(villager);
            // Seated before it is paid for, so a refused load costs the villager nothing
            if (machine.load(villager, bolt)) {
                villager.getSettlementsInventory().consumeOrBypassed(bolt, 1);
                if (!BallistaCombatOption.hasBoltInInventory(villager)) {
                    villager.clearHeldItem();
                }
            }
            return resumeFromMachineState(villager);
        }

    }

    private final class AimStep extends AbstractStep<BaseVillager> {

        private boolean aimed;
        private int ticksWithoutSolution;

        // Drawn once per shot and kept, so the intent holds still once the barrel reaches it
        private double scatterDirectionDraw;
        private double scatterDistanceDraw;

        private AimStep() {
            super("BallistaAim", AIM_TIMEOUT.getTicksAsInt(), null);
        }

        @Override
        protected void doOnEnter() {
            this.aimed = false;
            this.ticksWithoutSolution = 0;
            this.scatterDirectionDraw = RandomUtil.RANDOM.nextDouble();
            this.scatterDistanceDraw = RandomUtil.RANDOM.nextDouble();
        }

        @Override
        protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
            BaseVillager villager = context.getInitiator();
            StationApproach approach = approachStation(villager);
            if (approach == StationApproach.UNREACHABLE) {
                return failUnreachableStation(villager);
            }

            // The shot leaves at the barrel's angles, so it waits until the barrel has reached the aim already given, and
            // for the target to be in sight, so it is never loosed into cover
            if (this.aimed && machine.getAim().isAtRest() && approach == StationApproach.ARRIVED && targetInSight) {
                if (!machine.fire(villager)) {
                    return resumeFromMachineState(villager);
                }
                villager.triggerMotion(FIRE_MOTION);
                return StepResult.transition(Stage.FIRE);
            }

            Optional<BallistaFiringSolution> solution = solveForTarget();
            if (solution.isEmpty()) {
                return ++this.ticksWithoutSolution > NO_SOLUTION_GRACE.getTicksAsInt()
                        ? StepResult.fail("no_firing_solution")
                        : StepResult.noOp();
            }

            this.ticksWithoutSolution = 0;
            BallistaFiringSolution intended = skill.scatter(solution.get(), this.scatterDirectionDraw, this.scatterDistanceDraw);
            // Checked before the machine turns, so it is never left on a heading nobody can work it from
            if (!canReachStation(villager, BlockPos.containing(stationOf(machinePos, intended.getYaw())))) {
                return failUnreachableStation(villager);
            }
            machine.aimAt(intended.getYaw(), intended.getPitch());
            this.aimed = true;
            return StepResult.noOp();
        }

    }

    private final class FireStep extends AbstractStep<BaseVillager> {

        private FireStep() {
            super("BallistaFire");
        }

        @Override
        protected StepResult doTick(@Nonnull BehaviorContext<BaseVillager> context) {
            // Held until the machine settles, so the pull plays out and the next shot cycle finds the machine unwound.
            // That cycle starts from the approach, so an operator pushed off its station walks back before it winds
            if (machine.settledStage() == BallistaStateMachine.Stage.FIRING) {
                return StepResult.noOp();
            }

            // Blocks around the machine can change, so clear the reachable to re-pathfind
            reachableBallistas.clear();
            return StepResult.transition(Stage.APPROACH);
        }

    }

}
