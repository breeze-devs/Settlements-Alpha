package dev.breezes.settlements.infrastructure.minecraft.entities.projectiles;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.domain.ballista.BallistaBoltGeometry;
import dev.breezes.settlements.domain.ballista.BallistaGeometry;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.mixins.AbstractArrowInvoker;
import dev.breezes.settlements.shared.util.RandomUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * A bolt thrown by a ballista. Its position is its tip: the shaft trails behind the point that flies and strikes.
 */
public class BallistaBolt extends AbstractArrow {

    public static final float LAUNCH_SPEED_BLOCKS_PER_TICK = 5.0F;

    private static final String NBT_SPARES_VILLAGER_ALLIES = "sparesVillagerAllies";

    private static final int TRAIL_PARTICLES_PER_TICK = 4;

    private static final ClockTicks STUCK_LIFETIME = ClockTicks.minutes(5);

    // At full speed, one bolt kills any raider short of a ravager
    private static final int MIN_FULL_SPEED_DAMAGE = 40;
    private static final int MAX_FULL_SPEED_DAMAGE = 60;

    /**
     * Whether this bolt passes through villager allies instead of striking them.
     */
    private boolean sparesVillagerAllies;

    public BallistaBolt(@Nonnull EntityType<? extends BallistaBolt> type, @Nonnull Level level) {
        super(type, level);
    }

    /**
     * A fired bolt at the given tip position, to be picked up again as the ammunition it was fired as.
     */
    public BallistaBolt(@Nonnull Level level,
                        @Nonnull Vec3 tip,
                        @Nonnull ItemStack ammunition,
                        boolean sparesVillagerAllies) {
        super(EntityRegistry.BALLISTA_BOLT.get(), tip.x, tip.y, tip.z, level, ammunition, null);
        this.sparesVillagerAllies = sparesVillagerAllies;
        this.pickup = Pickup.ALLOWED;

        // Infinite pierce by design
        ((AbstractArrowInvoker) this).invokeSetPierceLevel(Byte.MAX_VALUE);
    }

    @Override
    public void tick() {
        Vec3 previousPosition = this.position();
        // Resolve impacts first so a bolt that lands this tick stops producing a trail.
        super.tick();
        if (!this.level().isClientSide() || this.inGround || this.isRemoved()) {
            return;
        }

        Vec3 travel = this.position().subtract(previousPosition);
        if (travel.lengthSqr() == 0.0) {
            return;
        }

        // Emit particle trail
        for (int i = 0; i < TRAIL_PARTICLES_PER_TICK; i++) {
            double fraction = (double) i / TRAIL_PARTICLES_PER_TICK;
            this.level().addParticle(ParticleTypes.CRIT,
                    previousPosition.x + travel.x * fraction,
                    previousPosition.y + travel.y * fraction,
                    previousPosition.z + travel.z * fraction,
                    -travel.x, -travel.y + 0.2, -travel.z);
        }
    }

    @Override
    protected boolean canHitEntity(@Nonnull Entity target) {
        // The bolt will 'fly through' the entity if this returns false
        return super.canHitEntity(target)
                && !(this.sparesVillagerAllies && target.getType().is(SettlementsEntityTypeTags.VILLAGER_ALLIES));
    }

    @Override
    protected void onHitEntity(@Nonnull EntityHitResult result) {
        // Calibrate the roll at launch speed so slower impacts deal less damage
        int fullSpeedDamage = RandomUtil.randomInt(MIN_FULL_SPEED_DAMAGE, MAX_FULL_SPEED_DAMAGE, true);
        this.setBaseDamage((double) fullSpeedDamage / LAUNCH_SPEED_BLOCKS_PER_TICK);
        super.onHitEntity(result);
    }

    @Override
    public boolean isCritArrow() {
        // The per-impact roll already supplies the intended damage variation.
        return false;
    }

    @Override
    protected void tickDespawn() {
        // The time in the ground is not saved, so a reload starts the stay over
        if (this.inGroundTime >= STUCK_LIFETIME.getTicks()) {
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(NBT_SPARES_VILLAGER_ALLIES, this.sparesVillagerAllies);
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.sparesVillagerAllies = tag.getBoolean(NBT_SPARES_VILLAGER_ALLIES);
    }

    @Override
    @Nonnull
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ItemRegistry.BALLISTA_BOLT.get());
    }

    /**
     * Whether this bolt has struck a block and stays in it.
     */
    public boolean isInGround() {
        return this.inGround;
    }

    // An arrow's yRot is its heading from +z toward +x, and its xRot its climb toward +y
    private Vec3 pointing() {
        double yRot = Math.toRadians(this.getYRot());
        double xRot = Math.toRadians(this.getXRot());
        return new Vec3(Math.sin(yRot) * Math.cos(xRot), Math.sin(xRot), Math.cos(yRot) * Math.cos(xRot));
    }

    @Override
    @Nonnull
    public AABB getBoundingBoxForCulling() {
        // The shaft is drawn behind the tip, so a box around the tip alone culls the bolt while its shaft is on screen
        Vec3 tipToTail = this.pointing()
                .scale(-BallistaBoltGeometry.TIP_REACH_PIXELS / BallistaGeometry.PIXELS_PER_BLOCK);
        return this.getBoundingBox().expandTowards(tipToTail);
    }

}
