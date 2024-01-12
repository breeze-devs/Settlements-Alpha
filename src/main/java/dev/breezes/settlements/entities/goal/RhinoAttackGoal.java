package dev.breezes.settlements.entities.goal;

import dev.breezes.settlements.entities.custom.RhinoEntity;
import dev.breezes.settlements.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.phys.AABB;

public class RhinoAttackGoal extends MeleeAttackGoal {

    private static final int PATHFINDING_COOLDOWN = TimeUtil.ticks(10);
    private static final double ATTACK_AOE = 1.5;

    private final RhinoEntity rhino;
    private final double speedModifier;

    private int pathfindCooldownRemaining;

    private AttackState attackState;
    private int stateDurationRemaining;

    public RhinoAttackGoal(RhinoEntity rhino, double speedModifier) {
        super(rhino, speedModifier, true);
        this.rhino = rhino;
        this.speedModifier = speedModifier;
        this.resetVariables();
    }

    @Override
    public void start() {
        super.start();
        this.resetVariables();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            this.stop();
            return;
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (--this.pathfindCooldownRemaining < 0) {
            this.mob.getNavigation().moveTo(target, this.speedModifier);

            boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
            this.pathfindCooldownRemaining = this.adjustedTickDelay(PATHFINDING_COOLDOWN + (hasLineOfSight ? 0 : TimeUtil.seconds(2)));
        }

        double distance = this.mob.getPerceivedTargetDistanceSquareForMeleeAttack(target);
        this.updateAnimationState(target, distance);
    }

    private void updateAnimationState(LivingEntity target, double distance) {
        if (--this.stateDurationRemaining > 0) {
            return;
        }

        switch (this.attackState) {
            case IDLE -> {
                // Check if we are within attack range
                if (distance < this.getAttackReachSqr(target)) {
                    this.attackState = AttackState.PRE_ATTACK;
                    this.stateDurationRemaining = this.attackState.getStateDuration();

                    this.rhino.setAttacking(true);
                }
            }
            case PRE_ATTACK -> {
                this.attackState = AttackState.ATTACKING;
                this.stateDurationRemaining = this.attackState.getStateDuration();
            }
            case ATTACKING -> {
                // Attack only if we are still in range
                // - otherwise the attack is considered a miss
                if (distance < this.getAttackReachSqr(target)) {
                    this.attackTarget(target);
                }

                // Update state
                this.attackState = AttackState.POST_ATTACK;
                this.stateDurationRemaining = this.attackState.getStateDuration();
            }
            case POST_ATTACK -> {
                this.attackState = AttackState.IDLE;
                this.stateDurationRemaining = this.attackState.getStateDuration();

                this.rhino.setAttacking(false);
            }
        }
    }

    private void attackTarget(LivingEntity target) {
        // Hurt target in an AOE
        this.rhino.doHurtTarget(target);
        AABB aoe = target.getBoundingBox().inflate(ATTACK_AOE);
        this.rhino.level().getEntities(target, aoe, (entity) -> entity != target && entity.getType() == target.getType())
                .forEach(this.rhino::doHurtTarget);

        // Play attack sound
        this.rhino.playAttackSound();
    }

    @Override
    public void stop() {
        super.stop();
        this.resetVariables();
        this.rhino.setAttacking(false);
    }

    private void resetVariables() {
        this.pathfindCooldownRemaining = 0;
        this.attackState = AttackState.IDLE;
        this.stateDurationRemaining = this.attackState.getStateDuration();
    }

    @AllArgsConstructor
    @Getter
    private enum AttackState {
        IDLE(-1),
        PRE_ATTACK(TimeUtil.ticks(10)),
        ATTACKING(0),
        POST_ATTACK(TimeUtil.ticks(10));

        private final int stateDuration;
    }

}
