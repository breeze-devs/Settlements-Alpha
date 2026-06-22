package dev.breezes.settlements.infrastructure.minecraft.entities.wolves;

import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.wolf.walkdog.WolfWalkConfig;
import dev.breezes.settlements.application.ai.brain.DefaultBrain;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.exceptions.SpawnFailedException;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.ITickable;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.goals.WolfFollowOwnerGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.goals.WolfSitWhenOrderedToGoal;
import dev.breezes.settlements.infrastructure.minecraft.mixins.LevelMixin;
import dev.breezes.settlements.infrastructure.minecraft.mixins.WolfMixin;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaBasicNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@CustomLog
@Getter
public class SettlementsWolf extends Wolf implements ISettlementsBrainEntity {

    private static final String DIRTY_NBT_KEY = "Dirty";
    private static final double BASE_MAX_HEALTH = 20;
    private static final ClockTicks UNTAMED_LIFETIME = ClockTicks.minutes(30);
    private static final ClockTicks DIRTY_ROLL_INTERVAL = ClockTicks.minutes(1);
    private static final double DIRTY_CHANCE = 0.05D;
    private static final int POOF_PARTICLE_COUNT = 12;
    private static final double POOF_PARTICLE_OFFSET = 0.25D;
    private static final double POOF_PARTICLE_SPEED = 0.02D;

    private final IBrain settlementsBrain;
    private final INavigationManager<SettlementsWolf> navigationManager;
    private final List<IBehavior<SettlementsWolf>> wolfBehaviors;
    private final ITickable untamedLifetime;
    private final ITickable dirtyRollTimer;

    private final Set<Class<?>> followOwnerLocks;

    @Setter
    private boolean dirty;

    public SettlementsWolf(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = DefaultBrain.builder()
                .build(); // TODO: implement
        this.navigationManager = new VanillaBasicNavigationManager<>(this);
        this.wolfBehaviors = new ArrayList<>();
        this.untamedLifetime = UNTAMED_LIFETIME.asTickable();
        this.dirtyRollTimer = DIRTY_ROLL_INTERVAL.asTickable();
        this.followOwnerLocks = new HashSet<>();
        this.dirty = false;

        if (!level.isClientSide()) {
            // Behaviors run only on the server
            this.initGoals();
            this.initBehaviors();
        }
    }

    public void lockFollowOwner(@Nonnull Class<?> owner) {
        this.followOwnerLocks.add(owner);
    }

    public void unlockFollowOwner(@Nonnull Class<?> owner) {
        this.followOwnerLocks.remove(owner);
    }

    public boolean isFollowOwnerLocked() {
        return !this.followOwnerLocks.isEmpty();
    }

    public boolean isFollowOwnerLockedBy(@Nonnull Class<?> owner) {
        return this.followOwnerLocks.contains(owner);
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(DIRTY_NBT_KEY, this.dirty);
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dirty = tag.getBoolean(DIRTY_NBT_KEY);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (!this.isTame() && this.untamedLifetime.tickAndCheck(1)) {
            this.spawnPoof();
            this.discard();
            return;
        }

        this.rollForDirtyState();

        for (IBehavior<SettlementsWolf> behavior : this.wolfBehaviors) {
            if (behavior.getStatus() == BehaviorStatus.RUNNING) {
                behavior.tick(1, this.level(), this);
                break;
            }
            if (behavior.tickPreconditions(1, this.level(), this)) {
                behavior.start(this.level(), this);
                break;
            }
        }
    }

    public static SettlementsWolf spawn(@Nonnull Location location) {
        ServerLevel serverLevel = location.getLevel()
                .filter(level -> level instanceof ServerLevel)
                .map(level -> (ServerLevel) level)
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn SettlementsWolf at %s: level is not server level".formatted(location.toString())));

        SettlementsWolf wolf = Optional.ofNullable(EntityRegistry.SETTLEMENTS_WOLF.get().create(serverLevel))
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn SettlementsWolf at %s".formatted(location.toString())));
        location.teleportEntityHere(wolf);
        wolf.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(location.toBlockPos()), MobSpawnType.REINFORCEMENT, null);
        wolf.setHealth(wolf.getMaxHealth());
        serverLevel.addFreshEntityWithPassengers(wolf);

        return wolf;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, BASE_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        // A villager-owned Settlements wolf stays village-bound — defer to vanilla, which won't let a
        // non-owner player claim it.
        if (this.isTame()) {
            return super.mobInteract(player, hand);
        }

        // An untamed Settlements wolf can be claimed by a player with bones, but the act converts it back
        // into an ordinary vanilla wolf: the Settlements variety remains exclusive to villagers.
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.is(Items.BONE) && !this.isAngry()) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }

                // Mirror vanilla wolf taming odds (1-in-3 per bone); honor the tame event so other mods can veto.
                if (this.random.nextInt(3) == 0 && !EventHooks.onAnimalTame(this, player)) {
                    this.convertToVanillaWolf(player);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6); // smoke: taming attempt failed
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // No other player interaction does anything to an untamed Settlements wolf.
        return InteractionResult.PASS;
    }

    /**
     * Swaps this Settlements wolf for a vanilla wolf tamed by the given player, carrying over only the coat
     * variant. We replace the entity rather than tame in place because Settlements wolves are village-only;
     * a player's pet must be a plain vanilla wolf.
     */
    private void convertToVanillaWolf(@Nonnull Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Wolf vanillaWolf = EntityType.WOLF.create(serverLevel);
        if (vanillaWolf == null) {
            return;
        }

        vanillaWolf.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        vanillaWolf.setVariant(this.getVariant());
        vanillaWolf.tame(player);
        vanillaWolf.setOrderedToSit(true);

        serverLevel.addFreshEntity(vanillaWolf);
        serverLevel.broadcastEntityEvent(vanillaWolf, (byte) 7); // hearts: taming succeeded
        this.discard();
    }

    private void spawnPoof() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                POOF_PARTICLE_COUNT,
                POOF_PARTICLE_OFFSET, POOF_PARTICLE_OFFSET, POOF_PARTICLE_OFFSET,
                POOF_PARTICLE_SPEED);
    }

    private void rollForDirtyState() {
        if (this.dirty || !this.dirtyRollTimer.tickCheckAndReset(1)) {
            return;
        }

        if (RandomUtil.chance(DIRTY_CHANCE)) {
            this.dirty = true;
        }
    }

    private void initGoals() {
        // Replace default standby & follow goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        this.goalSelector.addGoal(2, new WolfSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new WolfFollowOwnerGoal(this, 1.0D, 15.0F, 2.0F));

        // Strip vanilla prey-hunting goals: wolves hunt sheep/rabbits/turtles when untamed by default,
        // but Settlements wolves coexist peacefully with passive animals in village environments.
        this.targetSelector.removeAllGoals(goal -> goal instanceof NonTameRandomTargetGoal<?>);

        // Add target to all mobs that are hostile to villagers
        ISettlementsVillager.getEnemyClasses()
                .forEach(enemy -> this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, enemy, true)));
    }

    private void initBehaviors() {
        WolfWalkConfig wolfWalkConfig = SettlementsDagger.serverOrThrow().wolfWalkConfig();
        this.wolfBehaviors.add(new WolfWalkBehavior(wolfWalkConfig));
    }

    @Override
    @Nullable
    public BaseVillager getOwner() {
        if (this.getOwnerUUID() == null) {
            return null;
        }

        // Try to get the owner entity
        // TODO: THIS IS ASSUMING SERVER LEVEL
        return (BaseVillager) Optional.of(this.level())
                .filter(level -> level instanceof LevelMixin)
                .map(level -> (LevelMixin) level)
                .map(LevelMixin::invokeGetEntities)
                .map(entities -> entities.get(this.getOwnerUUID()))
                .filter(entity -> entity instanceof BaseVillager)
                .orElse(null);
    }

    public void setCollarColor(@Nonnull DyeColor color) {
        ((WolfMixin) this).invokeSetCollarColor(color);
    }

    @Override
    public IBrain getSettlementsBrain() {
        return this.settlementsBrain;
    }

    @Override
    public Entity getMinecraftEntity() {
        return this;
    }

    @Override
    public BubbleManager getBubbleManager() {
        return null;
    }

    @Override
    public int getNetworkingId() {
        return this.getId();
    }

    @Override
    public void lookAt(@Nonnull Location target) {
        // No plan orchestration on wolves, so drive the look control directly to win the per-tick race.
        this.getLookControl().setLookAt(target.toVec3());
    }

    @Override
    public void dropLeash(boolean sendPacket, boolean dropItem) {
        // Never drop leash item to prevent giving players free leads
        super.dropLeash(sendPacket, false);
    }

}
