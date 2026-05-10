package dev.breezes.settlements.infrastructure.minecraft.entities.cats;

import dev.breezes.settlements.application.ai.brain.DefaultBrain;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.exceptions.SpawnFailedException;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.goals.CatFollowOwnerGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.goals.CatSitWhenOrderedToGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.CatMixin;
import dev.breezes.settlements.infrastructure.minecraft.mixins.LevelMixin;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaBasicNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class SettlementsCat extends Cat implements ISettlementsBrainEntity {

    private final IBrain settlementsBrain;
    private final INavigationManager<SettlementsCat> navigationManager;

    private final Set<Class<?>> followOwnerLocks;

    public SettlementsCat(EntityType<? extends Cat> entityType, Level level) {
        super(entityType, level);

        this.settlementsBrain = DefaultBrain.builder().build();
        this.navigationManager = new VanillaBasicNavigationManager<>(this);
        this.followOwnerLocks = new HashSet<>();

        this.initGoals();

        if (this.getOwnerUUID() == null) {
            this.setTame(true, true);
            this.setOwnerUUID(UUID.randomUUID());
            ((CatMixin) this).invokeSetCollarColor(DyeColor.WHITE);
        }
    }

    public static SettlementsCat spawn(@Nonnull Location location) {
        ServerLevel serverLevel = location.getLevel()
                .filter(level -> level instanceof ServerLevel)
                .map(level -> (ServerLevel) level)
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn SettlementsCat at %s: level is not server level".formatted(location.toString())));

        SettlementsCat cat = Optional.ofNullable(EntityRegistry.SETTLEMENTS_CAT.get().create(serverLevel))
                .orElseThrow(() -> new SpawnFailedException("Failed to spawn SettlementsCat at %s".formatted(location.toString())));
        location.teleportEntityHere(cat);
        cat.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(location.toBlockPos()), MobSpawnType.REINFORCEMENT, null);
        serverLevel.addFreshEntityWithPassengers(cat);

        return cat;
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

    private void initGoals() {
        this.goalSelector.removeAllGoals(goal -> goal instanceof SitWhenOrderedToGoal || goal instanceof FollowOwnerGoal);
        this.goalSelector.addGoal(1, new CatSitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new CatFollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
    }

    @Override
    @Nullable
    public BaseVillager getOwner() {
        if (this.getOwnerUUID() == null) {
            return null;
        }

        return (BaseVillager) Optional.of(this.level())
                .filter(level -> level instanceof LevelMixin)
                .map(level -> (LevelMixin) level)
                .map(LevelMixin::invokeGetEntities)
                .map(entities -> entities.get(this.getOwnerUUID()))
                .filter(entity -> entity instanceof BaseVillager)
                .orElse(null);
    }

    public void setCollarColor(@Nonnull DyeColor color) {
        ((CatMixin) this).invokeSetCollarColor(color);
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
}
