package dev.breezes.settlements.infrastructure.minecraft.entities.projectiles;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.fishing.FishCatchEntry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.RandomUtil;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Custom fishing hook entity owned by a villager instead of a player.
 * <p>
 * Extends Projectile directly and maintains its own simplified state machine.
 */
@CustomLog
public class VillagerFishingHook extends Projectile {

    private static final double CAST_VELOCITY_SCALE = 0.6;
    private static final double CAST_VERTICAL_VELOCITY_OFFSET = 0.2;
    private static final double CAST_VELOCITY_RANDOM_TRIANGLE = 0.0103365;
    private static final double DEFAULT_ENTITY_SCALE = 1.0;
    public static final double MIN_CATCH_ENTITY_SCALE = 0.20;
    public static final double MAX_CATCH_ENTITY_SCALE = 2.00;
    private static final double SMALL_CATCH_SCALE_CEILING = 0.80;
    private static final double MEDIUM_CATCH_SCALE_CEILING = 1.30;

    private enum HookState {
        FLYING,
        BOBBING,
        CAUGHT
    }

    /**
     * Null on the client while the hook is untied, meaning its villager is not yet loaded.
     * That happens when the hook's add-entity packet beats the villager's as a player joins, or when the villager
     * leaves rendering range. An untied hook stays alive but invisible, and ties itself to its villager once the
     * villager is loaded.
     */
    @Getter
    @Nullable
    private BaseVillager villagerOwner;
    /**
     * The villager's entity id from the add-entity packet, which an untied hook looks up again on every tick.
     */
    @ClientSide
    private int ownerEntityId;

    private int lifetimeTicks;
    private final int maxLifetimeTicks;

    private HookState state;
    private int bobTimer;

    private boolean fishBitten;
    @Getter
    private boolean missedWater;
    @Getter
    @Nullable
    private Entity fishedEntity;
    @Getter
    @Nullable
    private FishCatchEntry selectedCatchEntry;
    @Getter
    @Nullable
    private String selectedCatchSize;

    public VillagerFishingHook(EntityType<? extends VillagerFishingHook> entityType, Level level) {
        super(entityType, level);

        this.villagerOwner = null;

        this.lifetimeTicks = 0;
        this.maxLifetimeTicks = Integer.MAX_VALUE;

        this.state = HookState.FLYING;
        this.bobTimer = 0;

        this.fishBitten = false;
        this.missedWater = false;
        this.fishedEntity = null;
        this.selectedCatchEntry = null;
        this.selectedCatchSize = null;
    }

    /**
     * Constructs a fishing hook from the villager's eye position aimed at the water block
     */
    public VillagerFishingHook(@Nonnull BaseVillager villager,
                               @Nonnull BlockPos waterTarget,
                               int biteTimeTicks,
                               int maxLifetimeTicks,
                               double extraHorizontalRandomness,
                               double extraVerticalVelocity) {
        super(EntityRegistry.VILLAGER_FISHING_HOOK.get(), villager.level());

        this.villagerOwner = villager;
        this.setOwner(villager);

        this.lifetimeTicks = 0;
        this.maxLifetimeTicks = maxLifetimeTicks;

        this.state = HookState.FLYING;
        this.bobTimer = biteTimeTicks;

        this.fishBitten = false;
        this.missedWater = false;
        this.fishedEntity = null;
        this.selectedCatchEntry = null;
        this.selectedCatchSize = null;

        this.noCulling = true;

        // Position at villager's eye level
        double startX = villager.getX();
        double startY = villager.getEyeY();
        double startZ = villager.getZ();
        this.moveTo(startX, startY, startZ);

        // Calculate velocity toward water target
        double dx = waterTarget.getX() + 0.5 - startX;
        double dy = waterTarget.getY() + 0.5 - startY;
        double dz = waterTarget.getZ() + 0.5 - startZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 0) {
            double randomX = this.random.triangle(0.0, CAST_VELOCITY_RANDOM_TRIANGLE + extraHorizontalRandomness);
            double randomZ = this.random.triangle(0.0, CAST_VELOCITY_RANDOM_TRIANGLE + extraHorizontalRandomness);
            this.setDeltaMovement(
                    dx / distance * CAST_VELOCITY_SCALE + randomX,
                    dy / distance * CAST_VELOCITY_SCALE + CAST_VERTICAL_VELOCITY_OFFSET + extraVerticalVelocity
                            + this.random.triangle(0.0, CAST_VELOCITY_RANDOM_TRIANGLE),
                    dz / distance * CAST_VELOCITY_SCALE + randomZ
            );
        }
    }

    @Override
    protected void defineSynchedData(@Nonnull SynchedEntityData.Builder builder) {
        // No sync-ed data
    }

    /**
     * Spawn the hook entity into the world.
     */
    public void castIntoWorld() {
        if (this.villagerOwner == null) {
            log.error("Cannot cast villager fishing hook into world when owner is null");
            return;
        }

        this.level().addFreshEntity(this);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            // Attempt to resolve the villager every tick to give the fishing hook an owner
            if (this.villagerOwner == null || this.villagerOwner.isRemoved()) {
                this.resolveVillagerOwner();
            }
        } else {
            // Only the server side discards the hook, and this removal reaches the client through packets anyway.
            if (this.villagerOwner == null || !this.villagerOwner.isAlive()) {
                this.discard();
                return;
            }

            if (++this.lifetimeTicks >= this.maxLifetimeTicks) {
                log.info("Fishing hook max lifetime reached, discarding");
                this.discard();
                return;
            }

            if (this.distanceToSqr(this.villagerOwner) > 1024.0) {
                this.discard();
                return;
            }
        }

        BlockPos blockPos = this.blockPosition();
        FluidState fluidState = this.level().getFluidState(blockPos);
        boolean isInWater = fluidState.is(FluidTags.WATER);
        float waterHeight = isInWater ? fluidState.getHeight(this.level(), blockPos) : 0.0F;

        switch (this.state) {
            case FLYING -> this.tickFlying(isInWater);
            case BOBBING, CAUGHT -> this.tickBobbing(blockPos, isInWater, waterHeight);
        }

        if (!isInWater) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.updateRotation();

        if (!this.level().isClientSide
                && this.state == HookState.FLYING && (this.onGround() || this.horizontalCollision)) {
            log.warn("Fishing hook missed water, discarding");
            this.missedWater = true;
            this.discard();
            return;
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
        this.reapplyPosition();
    }

    private void tickFlying(boolean isInWater) {
        if (isInWater) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 0.2, 0.3));
            this.state = HookState.BOBBING;
        }
    }

    private void tickBobbing(@Nonnull BlockPos blockPos, boolean isInWater, float waterHeight) {
        Vec3 velocity = this.getDeltaMovement();
        double deltaY = this.getY() + velocity.y - (double) blockPos.getY() - (double) waterHeight;
        if (Math.abs(deltaY) < 0.01) {
            deltaY += Math.signum(deltaY) * 0.1;
        }
        this.setDeltaMovement(
                velocity.x * 0.9,
                velocity.y - deltaY * (double) this.random.nextFloat() * 0.2,
                velocity.z * 0.9
        );

        if (!isInWater || this.level().isClientSide || this.state == HookState.CAUGHT) {
            return;
        }

        if (--this.bobTimer <= 0) {
            this.fishBitten = true;
            this.state = HookState.CAUGHT;
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(),
                        8, 0.2, 0.0, 0.2, 0.02);
            }
        }
    }

    /**
     * Check if a fish has bitten the hook.
     */
    public boolean hasBitten() {
        return this.fishBitten;
    }

    /**
     * Reel in the catch: spawns a visual fish entity that flies toward the villager.
     * The fish does not drop loot and auto-dies after a few seconds.
     *
     * @return the spawned fish entity, or null if reeling failed
     */
    public Optional<Entity> reelIn() {
        if (this.villagerOwner == null || this.level().isClientSide) {
            this.discard();
            return Optional.empty();
        }

        Optional<Entity> fishOptional = createRandomFish();
        if (fishOptional.isEmpty()) {
            this.discard();
            return Optional.empty();
        }

        Entity fishedEntity = fishOptional.get();

        // Position at hook and launch toward villager
        fishedEntity.setPos(this.getX(), this.getY(), this.getZ());
        double dx = this.villagerOwner.getX() - this.getX();
        double dy = this.villagerOwner.getY() - this.getY();
        double dz = this.villagerOwner.getZ() - this.getZ();
        fishedEntity.setDeltaMovement(dx * 0.2, dy * 0.2 + 0.2, dz * 0.2);

        this.level().addFreshEntity(fishedEntity);
        this.fishedEntity = fishedEntity;

        this.discard();
        return Optional.of(fishedEntity);
    }

    private Optional<Entity> createRandomFish() {
        Optional<FishCatchEntry> catchEntry = SettlementsDagger.serverOrThrow().fishCatchDataManager().rollRandomCatch();
        if (catchEntry.isEmpty()) {
            log.warn("No fish catch entries available while reeling in villager fishing hook");
            this.selectedCatchEntry = null;
            this.selectedCatchSize = null;
            return Optional.empty();
        }

        FishCatchEntry entry = catchEntry.get();
        Optional<Entity> entity = BuiltInRegistries.ENTITY_TYPE.getOptional(entry.getEntityId())
                .map(entityType -> entityType.create(this.level()))
                .map(createdEntity -> (Entity) createdEntity);
        if (entity.isEmpty()) {
            log.warn("Unable to create fish catch entity '{}'", entry.getEntityId());
            this.selectedCatchEntry = null;
            this.selectedCatchSize = null;
            return Optional.empty();
        }

        this.selectedCatchEntry = entry;
        entity.ifPresent(this::randomizeCatchEntityScale);
        return entity;
    }

    private void randomizeCatchEntityScale(@Nonnull Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        AttributeInstance scale = livingEntity.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }

        double targetScale = RandomUtil.randomDouble(MIN_CATCH_ENTITY_SCALE, MAX_CATCH_ENTITY_SCALE);
        this.selectedCatchSize = describeCatchSize(targetScale);
        scale.addPermanentModifier(new AttributeModifier(ResourceLocationUtil.mod("fish_scale"),
                toAddMultipliedTotalScaleModifier(targetScale), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static String describeCatchSize(double scale) {
        if (scale < SMALL_CATCH_SCALE_CEILING) {
            return "small";
        }
        if (scale < MEDIUM_CATCH_SCALE_CEILING) {
            return "medium";
        }
        return "large";
    }

    private double toAddMultipliedTotalScaleModifier(double targetScale) {
        return targetScale - DEFAULT_ENTITY_SCALE;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(@Nonnull ServerEntity serverEntity) {
        // The client's copy ties itself to its villager by this id. A hook with no villager sends its own id instead,
        // which never matches a villager, so that copy stays untied until the server discards the hook.
        int ownerId = this.villagerOwner != null ? this.villagerOwner.getId() : this.getId();
        return new ClientboundAddEntityPacket(this, serverEntity, ownerId);
    }

    @Override
    public void recreateFromPacket(@Nonnull ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);

        this.ownerEntityId = packet.getData();
        this.resolveVillagerOwner();
    }

    @ClientSide
    private void resolveVillagerOwner() {
        if (this.level().getEntity(this.ownerEntityId) instanceof BaseVillager villager) {
            this.villagerOwner = villager;
            // Keeps the inherited owner in agreement, as the casting constructor sets both
            this.setOwner(villager);
        } else {
            this.villagerOwner = null;
        }
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

}
