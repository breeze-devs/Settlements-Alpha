package dev.breezes.settlements.infrastructure.minecraft.entities.cuccos.goals;

import dev.breezes.settlements.infrastructure.minecraft.entities.cuccos.CuccoEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class DiveBombGoal extends Goal {

    private static final double CHICKEN_GLIDE_Y_MULTIPLIER = 0.6D;
    private static final double CONTACT_DAMAGE_DISTANCE_SQUARED = 1.0D;
    private static final float DIVE_BOMB_DAMAGE = 0.01F;

    private final CuccoEntity cucco;
    private final double diveThrust;

    public DiveBombGoal(@Nonnull CuccoEntity cucco, double diveThrust) {
        this.cucco = cucco;
        this.diveThrust = diveThrust;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.cucco.getTarget();
        return target != null && target.isAlive() && !this.cucco.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = this.cucco.getTarget();
        if (target == null) {
            return;
        }

        this.cucco.getLookControl().setLookAt(target, 30.0F, 30.0F);

        Vec3 currentMovement = this.cucco.getDeltaMovement();
        double fallVelocity = currentMovement.y;
        if (fallVelocity < 0.0D) {
            // Vanilla chickens dampen falling every tick; undo that glide only while a targeted cucco is diving.
            fallVelocity /= CHICKEN_GLIDE_Y_MULTIPLIER;
        }

        Vec3 targetCenter = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 direction = targetCenter.subtract(this.cucco.position()).normalize();

        this.cucco.setDeltaMovement(currentMovement.x + direction.x * this.diveThrust,
                fallVelocity + direction.y * this.diveThrust,
                currentMovement.z + direction.z * this.diveThrust);

        if (this.cucco.distanceToSqr(target) < CONTACT_DAMAGE_DISTANCE_SQUARED) {
            target.invulnerableTime = 0;
            target.hurt(this.cucco.damageSources().mobAttack(this.cucco), DIVE_BOMB_DAMAGE);
        }
    }

}
