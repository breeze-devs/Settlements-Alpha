package dev.breezes.settlements.infrastructure.minecraft.entities.cats;

import dev.breezes.settlements.application.ai.brain.DefaultBrain;
import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.domain.ai.brain.IBrain;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.navigation.INavigationManager;
import dev.breezes.settlements.domain.animal.PetSquish;
import dev.breezes.settlements.domain.exceptions.SpawnFailedException;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.goals.CatFollowOwnerGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.cats.goals.CatSitWhenOrderedToGoal;
import dev.breezes.settlements.infrastructure.minecraft.entities.pet.PetReaction;
import dev.breezes.settlements.infrastructure.minecraft.entities.pet.Pettable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.CatMixin;
import dev.breezes.settlements.infrastructure.minecraft.mixins.LevelMixin;
import dev.breezes.settlements.infrastructure.minecraft.navigation.VanillaBasicNavigationManager;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Getter
public class SettlementsCat extends Cat implements ISettlementsBrainEntity, Pettable {

    private final IBrain settlementsBrain;
    private final INavigationManager<SettlementsCat> navigationManager;

    private final Set<Class<?>> followOwnerLocks;

    @Getter
    private final PetReaction petReaction = PetReaction.builder()
            .cooldown(ClockTicks.seconds(1))
            .sound(SoundEvents.CAT_AMBIENT)
            .build();

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
        this.goalSelector.addGoal(6, new CatFollowOwnerGoal(this, 1.0D, 25.0F, 2.0F));
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
    public InteractionResult mobInteract(@Nonnull Player player, @Nonnull InteractionHand hand) {
        // Empty-handed right-click is our petting gesture; everything else (food, collar dye, leash) falls
        // through to vanilla so we don't accidentally swallow an interaction we don't own.
        if (player.getItemInHand(hand).isEmpty()) {
            this.pet(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    /**
     * Reacts to being petted with a short squish, a meow, and a couple of hearts.
     * <p>
     * Takes the acting {@link LivingEntity} rather than a {@link Player} so a future villager (or baby
     * villager) behavior can pet a cat through this exact path — the player is just the first caller.
     *
     * @return whether the pet actually landed (false while on cooldown, or when called client-side)
     */
    @Override
    public boolean pet(@Nonnull LivingEntity actor) {
        return this.petReaction.trigger(this);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (this.petReaction.onEntityEvent(id, this.tickCount)) {
            return;
        }
        super.handleEntityEvent(id);
    }

    /**
     * Client render hook: the per-axis squish scale for the current frame, or {@link PetSquish#IDENTITY}
     * when no petting reaction is playing.
     */
    @Nonnull
    public PetSquish.Factors getPetSquishFactors(float partialTick) {
        return this.petReaction.squishFactors(this.tickCount, partialTick);
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
        // No plan orchestration on cats, so drive the look control directly to win the per-tick race.
        this.getLookControl().setLookAt(target.toVec3());
    }
}
