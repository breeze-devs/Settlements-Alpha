package dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit;

import dev.breezes.settlements.application.ai.behavior.runtime.BehaviorSupport;
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
import dev.breezes.settlements.bootstrap.registry.sounds.SoundRegistry;
import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.domain.world.location.Vector;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.SettlementsEgg;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Throws one burst of combat eggs at a hostile from the villager's current position.
 * <p>
 * Ends after the burst, or as soon as the hostile dies, is removed, or leaves reach.
 */
@CustomLog
public class ThrowEggsAtHostileBehavior extends VillagerStateMachineBehavior {

    private static final ClockTicks BURST_DURATION = ClockTicks.of(30);
    private static final ClockTicks THROW_INTERVAL = ClockTicks.of(3);

    private static final float EGG_VELOCITY = 1.5f;
    private static final float EGG_INACCURACY = 4.0f;

    private enum Stage implements StageKey {
        BURST,
        END
    }

    private final LivingEntity target;
    private final double reachSquared;

    // Flips each throw so eggs leave the raised left and right fists in turn
    private int eggsThrown;

    /**
     * @param target      the hostile to pelt
     * @param reachBlocks the distance beyond which the hostile is out of reach and the burst ends
     */
    ThrowEggsAtHostileBehavior(@Nonnull BehaviorSupport support, @Nonnull LivingEntity target, double reachBlocks) {
        super(log, ClockTicks.ZERO.asTickable(), ClockTicks.ZERO.asTickable(), support);

        this.target = target;
        this.reachSquared = reachBlocks * reachBlocks;

        this.initializeStateMachine(this.createControlStep(), Stage.END);
    }

    private StagedStep<BaseVillager> createControlStep() {
        return StagedStep.<BaseVillager>builder()
                .name("ThrowEggsAtHostileBehavior")
                .initialStage(Stage.BURST)
                .stageStepMap(Map.of(Stage.BURST, this.createBurstStep()))
                .nextStage(Stage.END)
                .build();
    }

    private BehaviorStep<BaseVillager> createBurstStep() {
        return TimeBasedStep.<BaseVillager>builder()
                .name("EggCombatBurst")
                .withTickable(BURST_DURATION.asTickable())
                .onStart(ctx -> {
                    BaseVillager villager = ctx.getInitiator();
                    // Stop any existing path so the villager throws from its current position
                    villager.getNavigationManager().stop();
                    ctx.setState(BehaviorStateType.TARGET, TargetState.of(Targetable.fromEntity(this.target)));
                    villager.setMotion(AnimationArchetype.THROW);
                    return StepResult.noOp();
                })
                .addPeriodicStep(THROW_INTERVAL.getTicksAsInt(), this::throwEggAtTarget)
                .onEnd(ctx -> StepResult.transition(Stage.END))
                .build();
    }

    @Override
    protected void onBehaviorStart(@Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        this.eggsThrown = 0;

        // Both hands carry an egg so the straight-arm windmill throw reads as dual-fisted
        villager.setHeldItem(new ItemStack(Items.EGG));
        villager.setOffhandItem(new ItemStack(Items.EGG));
    }

    @Override
    protected boolean preTickGuard(int delta,
                                   @Nonnull Level world,
                                   @Nonnull BaseVillager villager,
                                   @Nonnull BehaviorContext<BaseVillager> context) {
        return this.target.isAlive()
                && !this.target.isRemoved()
                && villager.distanceToSqr(this.target) <= this.reachSquared;
    }

    @Override
    protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        villager.getNavigationManager().stop();
        villager.setMotion(AnimationArchetype.IDLE);
        villager.clearHeldItem();
        villager.clearOffhandItem();
    }

    private StepResult throwEggAtTarget(@Nonnull BehaviorContext<BaseVillager> context) {
        BaseVillager villager = context.getInitiator();
        Level world = villager.level();

        boolean rightHand = (this.eggsThrown % 2 == 0);
        this.eggsThrown++;

        Vec3 handPosition = ThrowEggsBehavior.computeRaisedHandPosition(villager, rightHand);
        Location handLocation = Location.of(handPosition.x, handPosition.y, handPosition.z, world);
        Vector direction = handLocation.getDirectionTo(Location.fromEntity(this.target, true));

        SettlementsEgg egg = new SettlementsEgg(world, villager, true);
        egg.setPos(handPosition.x, handPosition.y, handPosition.z);
        egg.shoot(direction.getX(), direction.getY(), direction.getZ(), EGG_VELOCITY, EGG_INACCURACY);
        world.addFreshEntity(egg);

        SoundRegistry.THROW_EGG.playGlobally(handLocation, SoundSource.NEUTRAL);
        return StepResult.noOp();
    }

}
