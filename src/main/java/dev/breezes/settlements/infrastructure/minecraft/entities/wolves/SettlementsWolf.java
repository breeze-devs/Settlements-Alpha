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
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.goals.WolfFollowOwnerGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.wolves.goals.WolfSitWhenOrderedToGoal;
import dev.breezes.settlements.infrastructure.minecraft.mixins.LevelMixin;
import dev.breezes.settlements.infrastructure.minecraft.mixins.WolfMixin;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaBasicNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@CustomLog
@Getter
public class SettlementsWolf extends Wolf implements ISettlementsBrainEntity {

    private final IBrain settlementsBrain;
    private final INavigationManager<SettlementsWolf> navigationManager;
    private final List<IBehavior<SettlementsWolf>> wolfBehaviors;

    private final Set<Class<?>> followOwnerLocks;

    public SettlementsWolf(EntityType<? extends Wolf> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = DefaultBrain.builder()
                .build(); // TODO: implement
        this.navigationManager = new VanillaBasicNavigationManager<>(this);
        this.wolfBehaviors = new ArrayList<>();
        this.followOwnerLocks = new HashSet<>();

        // Initialize goals
        this.initGoals();
        this.initBehaviors();

        // If not tamed by a villager, set as tamed by a random UUID to prevent players from taming it
        if (this.getOwnerUUID() == null) {
            this.setTame(true, true);
            this.setOwnerUUID(UUID.randomUUID());
            ((WolfMixin) this).invokeSetCollarColor(DyeColor.WHITE);
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
    protected void customServerAiStep() {
        super.customServerAiStep();

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
        serverLevel.addFreshEntityWithPassengers(wolf);

        return wolf;
    }

    private void initGoals() {
        // Replace default standby & follow goals
        this.goalSelector.removeAllGoals((goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal));
        this.goalSelector.addGoal(2, new WolfSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new WolfFollowOwnerGoal(this, 1.0D, 15.0F, 2.0F));

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
    public void dropLeash(boolean sendPacket, boolean dropItem) {
        // Never drop leash item to prevent giving players free leads
        super.dropLeash(sendPacket, false);
    }

}
