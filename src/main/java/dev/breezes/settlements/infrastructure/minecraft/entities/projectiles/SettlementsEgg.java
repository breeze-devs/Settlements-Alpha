package dev.breezes.settlements.infrastructure.minecraft.entities.projectiles;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.particles.ParticleTypeRegistry;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * A custom egg projectile that:
 * - Never spawns chickens (no onHitEntity chicken roll).
 * - Applies knockback to any LivingEntity including players.
 * - Spawns a custom egg-splat particle + vanilla item crack spray on impact.
 * <p>
 * An egg thrown in combat also passes through villager allies, and slows and knocks back harder
 * the villager enemies it strikes.
 */
public class SettlementsEgg extends ThrowableItemProjectile {

    private static final float FRIENDLY_KNOCKBACK_STRENGTH = 0.3f;
    private static final float ENEMY_KNOCKBACK_STRENGTH = 0.8f;
    private static final ClockTicks ENEMY_SLOWNESS_DURATION = ClockTicks.seconds(5);
    private static final int ENEMY_SLOWNESS_AMPLIFIER = 2; // Slowness III

    // Particle burst counts on impact
    private static final int EGG_SPLAT_COUNT = 1;
    private static final int EGG_CRACK_COUNT = 3;
    private static final double SPLAT_SPREAD = 0.2;
    private static final double CRACK_SPREAD = 0.1;
    private static final double CRACK_SPEED = 0.05;

    /**
     * Whether this egg was thrown in combat rather than as a prank. Not saved, so an egg reloaded mid-flight lands as a
     * prank egg, which is harmless.
     */
    private boolean combat;

    public SettlementsEgg(EntityType<? extends SettlementsEgg> type, Level level) {
        super(type, level);
    }

    public SettlementsEgg(Level level, LivingEntity shooter, boolean combat) {
        super(EntityRegistry.SETTLEMENTS_EGG.get(), shooter, level);
        this.combat = combat;
    }

    @Override
    @Nonnull
    protected Item getDefaultItem() {
        return Items.EGG;
    }

    @Override
    protected boolean canHitEntity(@Nonnull Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        if (!this.combat) {
            // A prank egg is meant for anyone, villagers and players included
            return true;
        }
        // An ally in the line of fire neither takes the egg nor shields the enemy behind it
        return !target.getType().is(SettlementsEntityTypeTags.VILLAGER_ALLIES);
    }

    @Override
    protected void onHitEntity(@Nonnull EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) {
            return;
        }
        if (!(result.getEntity() instanceof LivingEntity living)) {
            return;
        }

        boolean hitsEnemy = this.combat && living.getType().is(SettlementsEntityTypeTags.VILLAGER_ENEMIES);
        if (hitsEnemy) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    ENEMY_SLOWNESS_DURATION.getTicksAsInt(), ENEMY_SLOWNESS_AMPLIFIER));
        }

        // Knock the target along the egg's flight direction
        Vec3 travel = this.getDeltaMovement();
        double pushX = -travel.x;
        double pushZ = -travel.z;
        if (pushX == 0.0 && pushZ == 0.0) {
            pushX = this.getX() - living.getX();
            pushZ = this.getZ() - living.getZ();
        }
        living.knockback(hitsEnemy ? ENEMY_KNOCKBACK_STRENGTH : FRIENDLY_KNOCKBACK_STRENGTH, pushX, pushZ);
    }

    @Override
    protected void onHit(@Nonnull HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            spawnImpactParticles(result);
            this.discard();
        }
    }

    /**
     * Emits the egg-splat (custom) and item-crack (vanilla) particles at the impact location.
     * Runs server-side only; the server fans out the particle packets to nearby clients.
     */
    private void spawnImpactParticles(@Nonnull HitResult result) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Spawn egg particle at egg's position if entity hit, since hit entity position is at the feet
        Vec3 impactLocation = (result instanceof EntityHitResult) ? this.position() : result.getLocation();
        double x = impactLocation.x;
        double y = impactLocation.y;
        double z = impactLocation.z;

        // Custom camera-facing splat
        serverLevel.sendParticles(ParticleTypeRegistry.EGG_SPLAT.get(), x, y, z, EGG_SPLAT_COUNT, SPLAT_SPREAD, SPLAT_SPREAD, SPLAT_SPREAD, 0.0);

        // Vanilla egg crack spray for the satisfying shatter feeling
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EGG)), x, y, z,
                EGG_CRACK_COUNT, CRACK_SPREAD, CRACK_SPREAD, CRACK_SPREAD, CRACK_SPEED);
    }

}
