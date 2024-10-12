package dev.breezes.settlements.models.behaviors;

import dev.breezes.settlements.client.ClientExecutor;
import dev.breezes.settlements.client.ClientUtil;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.conditions.NearbyNeedsPotionExistsCondition;
import dev.breezes.settlements.models.misc.RandomRangeTickable;
import dev.breezes.settlements.models.misc.Tickable;
import dev.breezes.settlements.sounds.SoundRegistry;
import dev.breezes.settlements.util.DistanceUtils;
import dev.breezes.settlements.util.Ticks;
import lombok.CustomLog;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

@CustomLog
public class ThrowPotionsBehavior extends AbstractInteractAtTargetBehavior{

    private NearbyNeedsPotionExistsCondition<BaseVillager, LivingEntity> nearbyNeedsPotionExistsCondition;

    @Nullable
    private LivingEntity targetToThrow;
    private ItemStack potionToThrow;

    public ThrowPotionsBehavior(EntityType<? extends LivingEntity> type) {
        super(log, Tickable.of(Ticks.seconds(0)), RandomRangeTickable.of(Ticks.seconds(5), Ticks.seconds(15)), Tickable.of(Ticks.seconds(1)));

       this.nearbyNeedsPotionExistsCondition = new NearbyNeedsPotionExistsCondition<>(30, 15, type);
       this.preconditions.add(this.nearbyNeedsPotionExistsCondition);

        // Initialize variables
        this.targetToThrow = null;
    }

    public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
        this.targetToThrow = this.nearbyNeedsPotionExistsCondition.getTargets().getFirst();
        if(this.targetToThrow.isUnderWater() && !this.targetToThrow.hasEffect(MobEffects.WATER_BREATHING)){
            potionToThrow = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER_BREATHING);
        }
        else if (this.targetToThrow.isOnFire() && !this.targetToThrow.hasEffect(MobEffects.FIRE_RESISTANCE)){
            potionToThrow = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.FIRE_RESISTANCE);
        }
        else{
            potionToThrow = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        }
    }

    @Override
    protected void navigateToTarget(int delta, @NotNull Level world, @NotNull BaseVillager villager) {
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetToThrow.position(), 0.5F, 5));
    }

    @Override
    protected void interactWithTarget(int delta, @NotNull Level world, @NotNull BaseVillager villager) {
        villager.clearHeldItem();
        SoundRegistry.THROW_POTION.playGlobally(world, villager.getX(), villager.getY(), villager.getZ(), SoundSource.NEUTRAL);
        double xDistance = targetToThrow.getX() + targetToThrow.getDeltaMovement().x - villager.getX();
        double yDistance = targetToThrow.getY() + targetToThrow.getDeltaMovement().y - villager.getY();
        double zDistance = targetToThrow.getZ() + targetToThrow.getDeltaMovement().z - villager.getZ();
        double totalDistance = Math.sqrt(xDistance * xDistance + zDistance * zDistance);

        ThrownPotion potionEntity = new ThrownPotion(villager.level(), villager);
        potionEntity.setItem(potionToThrow);
        potionEntity.setXRot(potionEntity.getXRot() + 20.0F);
        potionEntity.shoot(xDistance, yDistance + totalDistance * 0.2, zDistance, 0.75F, 8.0F);

        villager.level().addFreshEntity(potionEntity);
        log.behaviorStatus("Thrown potion at target");

        ClientExecutor.runOnClient(() -> ClientUtil.getClientSideVillager(villager)
                .ifPresent(clientVillager -> clientVillager.getSpinAnimator().playOnce()));

        this.requestStop();
    }

    @Override
    protected void tickExtra(int delta, @Nonnull Level level, @Nonnull BaseVillager villager) {
        villager.setHeldItem(potionToThrow);
    }

    @Override
    protected boolean hasTarget(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return this.targetToThrow != null;
    }

    @Override
    protected boolean isTargetInReach(@Nonnull Level world, @Nonnull BaseVillager villager) {
        return DistanceUtils.isWithinDistance(villager.position(), this.targetToThrow.position(), 5.5D);
    }

    @Override
    public void doStop(@Nonnull Level world, @Nonnull BaseVillager villager) {
        this.targetToThrow = null;
    }
}
