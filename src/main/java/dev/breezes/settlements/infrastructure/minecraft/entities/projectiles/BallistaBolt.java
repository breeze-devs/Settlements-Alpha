package dev.breezes.settlements.infrastructure.minecraft.entities.projectiles;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.domain.ballista.BallistaBoltGeometry;
import dev.breezes.settlements.domain.ballista.BallistaGeometry;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.mixins.AbstractArrowInvoker;
import dev.breezes.settlements.shared.util.RandomUtil;
import dev.breezes.settlements.shared.util.SyncedDataWrapper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A bolt thrown by a ballista. Its position is its tip: the shaft trails behind the point that flies and strikes.
 */
public class BallistaBolt extends AbstractArrow {

    // Vanilla caps each axis of a velocity it sends a client at 3.9, so a faster bolt would fly short on the client
    public static final float LAUNCH_SPEED_BLOCKS_PER_TICK = 3.9F;

    private static final double TIP_REACH_BLOCKS = BallistaBoltGeometry.TIP_REACH_PIXELS / BallistaGeometry.PIXELS_PER_BLOCK;

    private static final String NBT_SPARES_VILLAGER_ALLIES = "sparesVillagerAllies";

    private static final int TRAIL_PARTICLES_PER_TICK = 4;

    private static final ClockTicks STUCK_LIFETIME = ClockTicks.minutes(20);

    // At full speed, one bolt kills any raider short of a ravager
    private static final int MIN_FULL_SPEED_DAMAGE = 40;
    private static final int MAX_FULL_SPEED_DAMAGE = 60;

    /**
     * Whether this bolt passes through villager allies instead of striking them.
     * <p>
     * Synced because hit detection also runs on the client, where a bolt that thinks it struck an ally slows to a
     * crawl until the server's position corrects it.
     */
    private static final SyncedDataWrapper<Boolean> DATA_SPARES_VILLAGER_ALLIES = SyncedDataWrapper.<Boolean>builder()
            .entityClass(BallistaBolt.class)
            .serializer(EntityDataSerializers.BOOLEAN)
            .defaultValue(false)
            .build();

    // Holds until the first flight tick; never saved, since a reloaded bolt is long past it
    private boolean freshlyFired;

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
        DATA_SPARES_VILLAGER_ALLIES.set(this.entityData, sparesVillagerAllies);
        this.pickup = Pickup.ALLOWED;
        this.freshlyFired = true;

        // Infinite pierce by design
        ((AbstractArrowInvoker) this).invokeSetPierceLevel(Byte.MAX_VALUE);
    }

    @Override
    protected void defineSynchedData(@Nonnull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        DATA_SPARES_VILLAGER_ALLIES.define(builder);
    }

    @Override
    public void tick() {
        Vec3 previousPosition = this.position();
        // Resolve impacts first so a bolt that lands this tick stops producing a trail.
        super.tick();
        this.freshlyFired = false;
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
        boolean sparedAlly = DATA_SPARES_VILLAGER_ALLIES.get(this.entityData)
                && target.getType().is(SettlementsEntityTypeTags.VILLAGER_ALLIES);
        // The bolt will 'fly through' the entity if this returns false
        return super.canHitEntity(target) && !sparedAlly;
    }

    @Override
    @Nullable
    protected EntityHitResult findHitEntity(@Nonnull Vec3 start, @Nonnull Vec3 end) {
        if (!this.freshlyFired) {
            return super.findHitEntity(start, end);
        }

        // Detect and hit point-blank-ranged entities
        Vec3 tail = start.subtract(end.subtract(start).normalize().scale(TIP_REACH_BLOCKS));
        return ProjectileUtil.getEntityHitResult(this.level(), this, tail, end, new AABB(tail, end).inflate(1.0),
                this::canHitEntity);
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
        tag.putBoolean(NBT_SPARES_VILLAGER_ALLIES, DATA_SPARES_VILLAGER_ALLIES.get(this.entityData));
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        DATA_SPARES_VILLAGER_ALLIES.set(this.entityData, tag.getBoolean(NBT_SPARES_VILLAGER_ALLIES));
    }

    @Override
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
    public AABB getBoundingBoxForCulling() {
        // The shaft is drawn behind the tip, so a box around the tip alone culls the bolt while its shaft is on screen
        Vec3 tipToTail = this.pointing().scale(-TIP_REACH_BLOCKS);
        return this.getBoundingBox().expandTowards(tipToTail);
    }

}
